plugins {
    java
}

tasks.test {
    testLogging { events("passed", "failed", "skipped") }
    systemProperty("todorok.repository.root", rootProject.projectDir.absolutePath)
    inputs.file(rootProject.file(
        "infra/docker/postgres/init/001-create-service-roles.sh"))
}

sourceSets.test {
    resources.srcDir(rootProject.file("contracts"))
}

dependencies {
    testImplementation(project(":services:planner-service"))
    testImplementation(project(":services:activity-service"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.flywaydb:flyway-core")
    testImplementation("org.springframework.security:spring-security-oauth2-jose")
    testImplementation(project(":libs:messaging-support"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-kafka")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.apache.kafka:kafka-clients")
    testImplementation("org.springframework:spring-jdbc")
    testImplementation("org.postgresql:postgresql")
    testImplementation("tools.jackson.core:jackson-databind")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
