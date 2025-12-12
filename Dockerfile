# ================================
# Stage 1: Build Stage
# ================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy dependency descriptor first to leverage Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application JAR (skip tests for faster production builds)
RUN mvn clean package -DskipTests -B

# ================================
# Stage 2: Runtime Stage
# ================================
FROM eclipse-temurin:21-jre-alpine

# Add metadata labels
LABEL maintainer="licitaciones-sapo" \
      description="Chilean public tender monitoring system with RSS feed" \
      version="1.0.0"

# Install wget for healthcheck
RUN apk add --no-cache wget

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose application port
EXPOSE 8080

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Environment variables with sensible defaults
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

# Entry point with dynamic port support
# Supports Railway (PORT) and standard Spring Boot (SERVER_PORT) configurations
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-${SERVER_PORT}} -jar /app/app.jar"]
