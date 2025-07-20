# =================================================================
# Stage 1: Build the application using Gradle
# =================================================================
FROM gradle:8.5.0-jdk17 AS builder
# 환경 변수들은 배포 시 주입
ENV META_APP_ID=""
ENV META_APP_SECRET=""
ENV DATABASE_URL=""
ENV JWT_SECRET=""

WORKDIR /app

# Optimize Docker layer caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# ==================== ✨ 핵심 수정 사항 ✨ ====================
# gradlew 스크립트에 실행 권한을 부여합니다.
# 이 과정이 없으면 Docker 환경에서 './gradlew' 명령어를 찾지 못하는 오류가 발생합니다.
RUN chmod +x ./gradlew
# ============================================================

RUN ./gradlew dependencies --no-daemon

# Copy the rest of the source code
COPY . .

# Build the application JAR
RUN ./gradlew clean bootJar --no-daemon

# =================================================================
# Stage 2: Create the final, minimal production image
# =================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Install necessary packages AS ROOT, BEFORE switching user.
RUN apk add --no-cache curl

# 런타임에 생성될 파일들을 저장할 디렉토리를 만들고,
# 비-루트 사용자인 'appuser'에게 쓰기 권한을 부여합니다.
RUN mkdir -p /app/static-files/qr-images \
             /app/static-files/thumbnails && \
    chown -R appuser:appgroup /app/static-files

# Healthcheck to ensure the application is running correctly
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8888/api/ai/health || exit 1

# NOW, switch to the non-root user to run the application securely
USER appuser

# Copy the application JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the application port (doesn't require root)
EXPOSE 8888

# Entrypoint to run the application as 'appuser'
ENTRYPOINT ["java", "-jar", "/app/app.jar"]