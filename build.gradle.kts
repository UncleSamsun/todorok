plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false
    id("org.openapi.generator") version "7.24.0"
}

import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import java.io.File

val contractsOutputRoot = providers.gradleProperty("contractsOutputRoot")
    .orElse(layout.projectDirectory.asFile.absolutePath)

fun registerContractTask(
    taskName: String,
    generator: String,
    spec: String,
    config: String,
    output: String,
) = tasks.register<GenerateTask>(taskName) {
    cleanupOutput.set(true)
    openapiGeneratorIgnoreList.set(listOf(".gitignore"))
    generatorName.set(generator)
    if (taskName == "generatePlannerSpring") {
        templateDir.set(layout.projectDirectory.dir("contracts/generator/planner-spring").asFile.absolutePath)
        inputs.dir("contracts/generator/planner-spring")
    }
    inputSpec.set(layout.projectDirectory.file(spec).asFile.absolutePath)
    configFile.set(layout.projectDirectory.file(config).asFile.absolutePath)
    outputDir.set(layout.dir(contractsOutputRoot.map { root -> File(root, output) }))
    doLast {
        outputDir.get().asFile.walkTopDown()
            .filter(File::isFile)
            .forEach { file ->
                val content = file.readText(Charsets.UTF_8)
                val normalized = Regex("[ \\t]+(?=\\r?$)", RegexOption.MULTILINE)
                    .replace(content, "")
                    .trimEnd('\r', '\n') + "\n"
                if (normalized != content) {
                    file.writeText(normalized, Charsets.UTF_8)
                }
            }
    }
}

registerContractTask(
    "generatePlannerSpring",
    "spring",
    "contracts/openapi/planner-v1.yaml",
    "contracts/generator/planner-spring.yaml",
    "services/planner-service/src/generated",
)
registerContractTask(
    "generateActivitySpring",
    "spring",
    "contracts/openapi/activity-v1.yaml",
    "contracts/generator/activity-spring.yaml",
    "services/activity-service/src/generated",
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

registerContractTask(
    "generateTemplateInternalSpring",
    "spring",
    "contracts/openapi/template-internal-v1.yaml",
    "contracts/generator/template-internal-spring.yaml",
    "libs/web-support/src/generated",
)

tasks.register<Exec>("generateEventV2Java") {
    inputs.dir("contracts/events")
    inputs.file("scripts/generate-event-v2.mjs")
    outputs.dir(contractsOutputRoot.map { File(it, "libs/event-contracts/src/generated") })
    commandLine("node", "scripts/generate-event-v2.mjs", contractsOutputRoot.get())
}

tasks.register("generateContracts") {
    dependsOn(
        "generatePlannerSpring",
        "generateActivitySpring",
        "generatePlannerTypeScript",
        "generateActivityTypeScript",
        "generateTemplateInternalSpring",
        "generateEventV2Java",
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
