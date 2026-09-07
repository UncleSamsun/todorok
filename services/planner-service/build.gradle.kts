import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

jacoco { toolVersion = "0.8.14" }
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports { xml.required.set(true); html.required.set(true) }
    classDirectories.setFrom(sourceSets.main.get().output.asFileTree.matching { exclude("io/todorok/planner/api/**") })
}
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            element = "CLASS"
            includes = listOf("io.todorok.planner.series.NextOccurrencePolicy", "io.todorok.planner.task.TaskTransitionPolicy")
            limit { counter = "BRANCH"; minimum = "1.0".toBigDecimal() }
        }
    }
}
tasks.named("check") { dependsOn(tasks.jacocoTestCoverageVerification) }

springBoot {
    mainClass.set("io.todorok.planner.PlannerApplication")
}

tasks.register<BootJar>("migrationBootJar") {
    group = "build"
    archiveClassifier.set("migration")
    targetJavaVersion.set(JavaVersion.VERSION_25)
    mainClass.set("io.todorok.migration.planner.PlannerMigrationApplication")
    classpath(sourceSets.main.get().runtimeClasspath)
}

tasks.named("assemble") {
    dependsOn("migrationBootJar")
    dependsOn("bootstrapBootJar")
}

tasks.register<BootJar>("bootstrapBootJar") {
    group = "build"
    archiveClassifier.set("bootstrap")
    targetJavaVersion.set(JavaVersion.VERSION_25)
    mainClass.set("io.todorok.planner.auth.BootstrapCommand")
    classpath(sourceSets.main.get().runtimeClasspath)
}

dependencies {
    implementation(project(":libs:event-contracts"))
    implementation(project(":libs:messaging-support"))
    implementation(project(":libs:web-support"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        java.srcDir("src/generated/java")
    }
}

tasks.named("compileJava") {
    dependsOn(rootProject.tasks.named("generateContracts"))
}

tasks.test {
    systemProperty("todorok.repository.root", rootProject.projectDir.absolutePath)
    inputs.file(rootProject.file(
        "infra/docker/postgres/init/001-create-service-roles.sh"))
}
