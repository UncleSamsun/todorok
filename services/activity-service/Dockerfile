# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    chmod +x gradlew \
    && ./gradlew :services:activity-service:bootJar :services:activity-service:migrationBootJar --no-daemon --no-configuration-cache --max-workers=1 \
    && cp services/activity-service/build/libs/*-migration.jar /tmp/migration.jar \
    && find services/activity-service/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-migration.jar' -exec cp {} /tmp/app.jar \;

FROM eclipse-temurin:25-jre-noble
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /tmp/app.jar /app/app.jar
COPY --from=builder /tmp/migration.jar /app/migration.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
