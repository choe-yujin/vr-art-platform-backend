# =================================================================
# Stage 1: Build the application using Gradle
# =================================================================
FROM gradle:8.14.2-jdk17 AS builder

WORKDIR /app

# Optimize Docker layer caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon

# Copy the rest of the source code
COPY . .

# Build the application JAR
RUN gradle clean bootJar --no-daemon

# =================================================================
# Stage 2: Create the final, minimal production image
# =================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# (FIX) Install necessary packages AS ROOT, BEFORE switching user.
RUN apk add --no-cache curl

# Healthcheck to ensure the application is running correctly
# This also needs to be defined before switching user if it needs root access,
# but curl itself doesn't, so its position is flexible.
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