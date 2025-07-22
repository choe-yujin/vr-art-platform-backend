# Dockerfile (모든 환경 공통 - 최종 버전)

# =================================================================
# Stage 1: Build the application using Gradle
# =================================================================
FROM gradle:8.5.0-jdk17 AS builder
WORKDIR /app

# 1. Gradle 설정 파일만 먼저 복사하여 의존성 레이어를 캐싱합니다.
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

# 2. 의존성을 먼저 다운로드하여 별도의 레이어로 만듭니다.
RUN ./gradlew dependencies --no-daemon

# 3. 나머지 소스 코드를 복사합니다.
COPY src ./src

# 4. 애플리케이션을 빌드합니다. Spring Boot의 Layered JAR 기능을 활용합니다.
RUN ./gradlew bootJar --no-daemon

# =================================================================
# Stage 2: Create the final, minimal production image
# =================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# [보안 강화] 애플리케이션 실행을 위한 비-루트(non-root) 사용자를 생성합니다.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# [최적화] Spring Boot Layered JAR 복사
COPY --from=builder /app/build/libs/app.jar app.jar

# Layered JAR에서 레이어 분리 (dependencies/, spring-boot-loader/, snapshot-dependencies/, application/ 생성)
RUN java -Djarmode=layertools -jar app.jar extract

# 생성된 폴더들의 소유권 변경 (COPY → RUN 으로 변경)
RUN chown -R appuser:appgroup dependencies/ spring-boot-loader/ snapshot-dependencies/ application/

# [상태 확인] 애플리케이션 정상 실행 확인용 Health Check 설치 및 설정
RUN apk add --no-cache curl
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:8888/api/auth/health || exit 1

# [보안 강화] 비-루트 사용자로 전환
USER appuser

# 애플리케이션 포트 노출
EXPOSE 8888

# 최종 실행 명령어
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]