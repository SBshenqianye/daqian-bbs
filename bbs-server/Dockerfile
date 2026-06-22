# ============================================
# BBS Server - Dockerfile (Multi-stage build)
# ============================================
# Build stage: compile with Maven + JDK 8
FROM maven:3.8.6-openjdk-8 AS build

WORKDIR /build

# 1. Download dependencies first (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Build application
COPY src ./src
RUN mvn package -DskipTests -B

# ============================================
# Runtime stage: minimal Java 8 runtime
# Use Red Hat UBI 7 for RHEL 7 compatibility
FROM registry.access.redhat.com/ubi7/openjdk-8:latest

WORKDIR /app

COPY --from=build /build/target/bbs-server.jar app.jar

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8083/bbs-server/ || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=podman"]
