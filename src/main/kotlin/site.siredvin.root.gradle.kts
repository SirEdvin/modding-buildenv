import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import site.siredvin.peripheralium.gradle.collectSecrets

plugins {
    java
}

val secretEnv = collectSecrets()
val projectGroup = properties["projectGroup"] ?: "site.siredvin"
val rootProjectDir = projectDir

fun setupSubprojectExternal(subproject: Project) {
    subproject.apply(plugin = "maven-publish")
    subproject.apply(plugin = "com.diffplug.spotless")
    subproject.apply(plugin = "site.siredvin.base")
    subproject.apply(plugin = "site.siredvin.linting")
    subproject.apply(plugin = "idea")
    if (subprojectShaking.withKotlin.get()) {
        subproject.apply(plugin = "kotlin")
    }

    subproject.extra["curseforgeKey"] = secretEnv["CURSEFORGE_KEY"] ?: System.getenv("CURSEFORGE_KEY") ?: ""
    subproject.extra["modrinthKey"] = secretEnv["MODRINTH_KEY"] ?: System.getenv("MODRINTH_KEY") ?: ""
    subproject.extra["rootProjectDir"] = rootProjectDir
    subproject.group = projectGroup

    if (subprojectShaking.withKotlin.get()) {
        subproject.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib:${subprojectShaking.kotlinVersion.get()}")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${subprojectShaking.kotlinCoroutinesVersion.get()}")
            implementation("org.jetbrains.kotlinx:atomicfu-jvm:${subprojectShaking.kotlinAtomicfuVersion.get()}")
        }
    }
}

class SubProjectShakingExtension(targetProject: Project) {
    val withKotlin: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val kotlinVersion: Property<String> = targetProject.objects.property(String::class.java)
    val kotlinCoroutinesVersion: Property<String> = targetProject.objects.property(String::class.java)
    val kotlinAtomicfuVersion: Property<String> = targetProject.objects.property(String::class.java)

    init {
        kotlinVersion.convention("1.8.21")
        kotlinCoroutinesVersion.convention("1.6.4")
        kotlinAtomicfuVersion.convention("0.20.2")
    }

    fun setupSubproject(subproject: Project) {
        setupSubprojectExternal(subproject)
    }
}

val subprojectShaking = SubProjectShakingExtension(project)
project.extensions.add("subprojectShaking", subprojectShaking)
