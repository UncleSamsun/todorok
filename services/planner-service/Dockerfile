FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew \
    && ./gradlew :services:planner-service:bootJar --no-daemon --max-workers=1

FROM eclipse-temurin:25-jre-noble
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /workspace/services/planner-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
