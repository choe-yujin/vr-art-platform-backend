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

# [최적화] Spring Boot Layered JAR에서 분리된 레이어들을 복사합니다.
COPY --from=builder /app/build/libs/app.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# 각 레이어를 순서대로 복사하고, appuser에게 소유권을 부여합니다.
COPY --chown=appuser:appgroup dependencies/ ./
COPY --chown=appuser:appgroup spring-boot-loader/ ./
COPY --chown=appuser:appgroup snapshot-dependencies/ ./
COPY --chown=appuser:appgroup application/ ./

# [상태 확인] 애플리케이션이 정상적으로 실행 중인지 확인하는 Health Check
RUN apk add --no-cache curl
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:8888/api/auth/health || exit 1

# [보안 강화] 비-루트 사용자로 전환하여 애플리케이션을 실행합니다.
USER appuser

# 애플리케이션 포트를 노출합니다.
EXPOSE 8888

# 최종 컨테이너 실행 명령어
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]