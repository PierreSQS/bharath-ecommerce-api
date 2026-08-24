# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM maven:3.9.16-eclipse-temurin-25-alpine AS builder
WORKDIR /build

# Dependency layer: re-resolved only when pom.xml changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

# Application layer.
COPY src ./src
# Tests start Docker Compose (spring.docker.compose.skip.in-tests=false), so they cannot run here.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

# Split the fat jar into cache-friendly layers. Renaming first keeps the runtime
# stage independent of the project version (*.jar does not match *.jar.original).
RUN cp target/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination /build/extracted

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S spring && adduser -S -G spring spring
WORKDIR /app

# Ordered least- to most-frequently-changing.
COPY --from=builder --chown=spring:spring /build/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /build/extracted/application/ ./

USER spring:spring
EXPOSE 8080

# Read automatically by the JVM, so ENTRYPOINT stays exec-form and the JVM keeps PID 1 for SIGTERM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# start-period covers Flyway migrations plus Hibernate schema validation on first boot.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

# extract --layers produces a slim jar whose manifest Class-Path points at lib/,
# so it is launched with -jar rather than with JarLauncher.
ENTRYPOINT ["java", "-jar", "app.jar"]
