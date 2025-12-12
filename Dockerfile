FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Runtime
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL maintainer="MercadoPublicoMonitor"
LABEL description="Monitor de licitaciones públicas chilenas con RSS feed"
LABEL version="1.0.0"

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# Environment variables with defaults
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

# Entry point with dynamic port support (Railway compatibility)
# Railway injects PORT env var, Spring Boot uses SERVER_PORT
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-${SERVER_PORT}} -jar /app/app.jar"]
