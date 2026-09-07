plugins {
    `java-library`
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    api("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation("tools.jackson.core:jackson-databind")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.networknt:json-schema-validator:3.0.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main { java.srcDir("src/generated/java") }
    test {
        resources.srcDir(rootProject.file("contracts"))
    }
}

tasks.named("compileJava") { dependsOn(rootProject.tasks.named("generateEventV2Java")) }
