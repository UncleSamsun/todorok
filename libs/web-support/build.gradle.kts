plugins {
    `java-library`
}

sourceSets.main { java.srcDir("src/generated/java") }
tasks.named("compileJava") { dependsOn(rootProject.tasks.named("generateTemplateInternalSpring")) }

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    api("org.springframework:spring-webmvc")
    api("org.springframework.security:spring-security-config")
    api("org.springframework.security:spring-security-oauth2-resource-server")
    api("org.springframework.security:spring-security-oauth2-jose")
    implementation("tools.jackson.core:jackson-databind")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.slf4j:slf4j-api")
    implementation("jakarta.servlet:jakarta.servlet-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("jakarta.annotation:jakarta.annotation-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-validation")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
