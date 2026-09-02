plugins {
    `java-library`
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    api(project(":libs:event-contracts"))
    implementation("org.springframework:spring-jdbc")
    implementation("tools.jackson.core:jackson-databind")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.postgresql:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
