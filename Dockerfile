# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build

# Copy the wrapper and the POM first so the dependency download is cached
# independently of source changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests clean package

# ---------- Stage 2: extract layers ----------
# Splits the fat jar into layers so unchanged dependencies stay cached.
FROM eclipse-temurin:25-jdk-alpine AS extractor
WORKDIR /extract
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---------- Stage 3: runtime ----------
FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S spring && adduser -S -G spring spring

WORKDIR /app
COPY --from=extractor --chown=spring:spring /extract/extracted/dependencies/ ./
COPY --from=extractor --chown=spring:spring /extract/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=spring:spring /extract/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /extract/extracted/application/ ./

USER spring:spring
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
