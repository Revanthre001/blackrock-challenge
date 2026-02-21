# Build command: docker build -t blk-hacking-ind-{name-lastname} .
#
# Multi-stage build for minimal final image size.
# Stage 1: Maven build using official Maven+JDK Alpine image (no wrapper needed)
# Stage 2: Runtime using Eclipse Temurin JRE 17 Alpine (lean, production-grade)
#
# Why Alpine Linux?
#   - Minimal attack surface (~5MB base vs ~120MB Debian)
#   - Faster pull/push in CI/CD pipelines
#   - Complies with the "Linux distribution" requirement

# --- Stage 1: Build -------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy POM first to cache dependency layer (only re-downloads if pom.xml changes)
COPY pom.xml ./

# Download all dependencies offline (Docker layer cache)
RUN mvn dependency:go-offline -B -q

# Copy source code and build the fat JAR
COPY src ./src
RUN mvn package -DskipTests -B -q

# --- Stage 2: Runtime -----------------------------------------------------
# Eclipse Temurin 17 JRE on Alpine: smallest secure JRE-only image
FROM eclipse-temurin:17-jre-alpine

# Install wget for Docker Compose healthcheck probe
RUN apk add --no-cache wget

# Security: run as non-root user
RUN addgroup -S blackrock && adduser -S blkapp -G blackrock
USER blkapp

WORKDIR /app

# Copy the fat JAR from build stage
COPY --from=builder /build/target/blackrock-challenge-1.0.0.jar app.jar

# Port required by the challenge specification
EXPOSE 5477

# JVM tuning for containerized deployment:
#   UseContainerSupport     — respect Docker memory limits, not host RAM
#   MaxRAMPercentage=75.0   — use up to 75% of container memory as JVM heap
#   UseG1GC                 — low-latency GC suitable for REST API workloads
#   security.egd            — fast SecureRandom seed for Spring startup
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseG1GC", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
