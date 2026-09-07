# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    chmod +x gradlew \
    && ./gradlew :services:notification-service:bootJar :services:notification-service:migrationBootJar --no-daemon --no-configuration-cache --max-workers=1 \
    && cp services/notification-service/build/libs/*-migration.jar /tmp/migration.jar \
    && find services/notification-service/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-migration.jar' -exec cp {} /tmp/app.jar \;

FROM eclipse-temurin:25-jre-noble
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /tmp/app.jar /app/app.jar
COPY --from=builder /tmp/migration.jar /app/migration.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
