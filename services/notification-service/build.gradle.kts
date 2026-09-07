import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

springBoot {
    mainClass.set("io.todorok.notification.NotificationApplication")
}

tasks.register<BootJar>("migrationBootJar") {
    group = "build"
    archiveClassifier.set("migration")
    targetJavaVersion.set(JavaVersion.VERSION_25)
    mainClass.set("io.todorok.migration.notification.NotificationMigrationApplication")
    classpath(sourceSets.main.get().runtimeClasspath)
}

tasks.named("assemble") {
    dependsOn("migrationBootJar")
}

dependencies {
    implementation(project(":libs:event-contracts"))
    implementation(project(":libs:messaging-support"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    systemProperty("todorok.repository.root", rootProject.projectDir.absolutePath)
    inputs.file(rootProject.file(
        "infra/docker/postgres/init/001-create-service-roles.sh"))
}
