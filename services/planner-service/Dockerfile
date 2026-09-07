# syntax=docker/dockerfile:1.7
FROM node:24-bookworm-slim AS contract-node
FROM eclipse-temurin:25-jdk-noble AS builder
COPY --from=contract-node /usr/local/bin/node /usr/local/bin/node
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    chmod +x gradlew \
    && ./gradlew :services:planner-service:bootJar :services:planner-service:migrationBootJar :services:planner-service:bootstrapBootJar --no-daemon --no-configuration-cache --max-workers=1 \
    && cp services/planner-service/build/libs/*-migration.jar /tmp/migration.jar \
    && cp services/planner-service/build/libs/*-bootstrap.jar /tmp/bootstrap.jar \
    && find services/planner-service/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-migration.jar' ! -name '*-bootstrap.jar' -exec cp {} /tmp/app.jar \;

FROM eclipse-temurin:25-jre-noble
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /tmp/app.jar /app/app.jar
COPY --from=builder /tmp/migration.jar /app/migration.jar
COPY --from=builder /tmp/bootstrap.jar /app/bootstrap.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
