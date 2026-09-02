plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false
    id("org.openapi.generator") version "7.24.0"
}

import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

val contractsOutputRoot = providers.gradleProperty("contractsOutputRoot")
    .orElse(layout.projectDirectory.asFile.absolutePath)

fun registerContractTask(
    taskName: String,
    generator: String,
    spec: String,
    config: String,
    output: String,
) = tasks.register<GenerateTask>(taskName) {
    generatorName.set(generator)
    inputSpec.set(layout.projectDirectory.file(spec).asFile.absolutePath)
    configFile.set(layout.projectDirectory.file(config).asFile.absolutePath)
    outputDir.set(layout.dir(contractsOutputRoot.map { root -> file("$root/$output") }))
}

registerContractTask(
    "generatePlannerSpring",
    "spring",
    "contracts/openapi/planner-v1.yaml",
    "contracts/generator/planner-spring.yaml",
    "services/planner-service",
)
registerContractTask(
    "generateActivitySpring",
    "spring",
    "contracts/openapi/activity-v1.yaml",
    "contracts/generator/activity-spring.yaml",
    "services/activity-service",
)
registerContractTask(
    "generatePlannerTypeScript",
    "typescript-fetch",
    "contracts/openapi/planner-v1.yaml",
    "contracts/generator/planner-typescript.yaml",
    "packages/api-client/src/generated/planner",
)
registerContractTask(
    "generateActivityTypeScript",
    "typescript-fetch",
    "contracts/openapi/activity-v1.yaml",
    "contracts/generator/activity-typescript.yaml",
    "packages/api-client/src/generated/activity",
)

tasks.register("generateContracts") {
    dependsOn(
        "generatePlannerSpring",
        "generateActivitySpring",
        "generatePlannerTypeScript",
        "generateActivityTypeScript",
    )
}

subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
