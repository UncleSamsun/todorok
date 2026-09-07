plugins {
    java
}

tasks.test {
    systemProperty("todorok.repository.root", rootProject.projectDir.absolutePath)
}

sourceSets.test {
    resources.srcDir(rootProject.file("contracts"))
}

dependencies {
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
