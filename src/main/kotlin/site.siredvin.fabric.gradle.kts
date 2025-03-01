plugins {
    java
    id("fabric-loom")
}

fun configureFabric(targetProject: Project, projectName: String, accessWidener: File?, commonProjectName: String, createRefmap: Boolean, versionMappings: Map<String, String>, stablePlayer: Boolean) {
    val minecraftVersion: String by targetProject.extra

    targetProject.dependencies {
        minecraft("com.mojang:minecraft:$minecraftVersion")
        mappings(loom.officialMojangMappings())
        if (commonProjectName.isNotEmpty()) {
            implementation(project(":$commonProjectName")) {
                exclude("cc.tweaked")
                exclude("dan200.computercraft")
            }
        }
    }

    targetProject.loom {
        if (accessWidener != null) {
            accessWidenerPath.set(accessWidener)
        }
        if (createRefmap) {
            mixin.defaultRefmapName.set("$projectName.refmap.json")
        }
        runs {
            named("client") {
                configName = "Fabric Client"
                if (stablePlayer)
                    programArgs("--username=Player")
            }
            named("server") {
                configName = "Fabric Server"
            }
            create("data") {
                client()
                vmArg("-Dfabric-api.datagen")
                vmArg("-Dfabric-api.datagen.modid=$projectName")
                vmArg("-Dfabric-api.datagen.output-dir=${file("src/generated/resources")}")
                vmArg("-Dfabric-api.datagen.strict-validation")
            }
        }
    }

    targetProject.tasks {
        val extractedLibs = targetProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        processResources {
            if (commonProjectName.isNotEmpty()) {
                from(project(":$commonProjectName").sourceSets.main.get().resources)
            }
            inputs.property("version", targetProject.version)
            val basePropertyMap = mutableMapOf(
                "version" to targetProject.version,
            )
            versionMappings.entries.forEach {
                val versionForValue = extractedLibs.findVersion(it.value)
                if (versionForValue.isEmpty) {
                    error("Cannot find version for mapping ${it.value}")
                }
                inputs.property("${it.key}Version", versionForValue.get())
                basePropertyMap["${it.key}Version"] = versionForValue.get()
            }

            filesMatching("fabric.mod.json") {
                expand(basePropertyMap)
            }
            exclude(".cache")
        }
        if (commonProjectName.isNotEmpty()) {
            withType<JavaCompile> {
                if (this.name == "compileJava") {
                    source(project(":$commonProjectName").sourceSets.main.get().allSource)
                }
            }
            withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                if (this.name == "compileKotlin") {
                    source(project(":$commonProjectName").sourceSets.main.get().allSource)
                }
            }
        }
        targetProject.ext.set("releaseJar", "remapJar")
    }
}

class FabricShakingExtension(private val targetProject: Project) {
    val commonProjectName: Property<String> = targetProject.objects.property(String::class.java)
    val accessWidener: Property<File> = targetProject.objects.property(File::class.java)
    val createRefmap: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val extraVersionMappings: MapProperty<String, String> = targetProject.objects.mapProperty(String::class.java, String::class.java)
    val stablePlayer: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val projectName: Property<String> = targetProject.objects.property(String::class.java)

    fun shake() {
        stablePlayer.convention(false)
        createRefmap.convention(false)
        extraVersionMappings.convention(emptyMap())
        if (targetProject.extra.has("modBaseName")) {
            val modBaseName: String by targetProject.extra
            projectName.convention(modBaseName)
        }
        configureFabric(targetProject, projectName.get(), accessWidener.orNull, commonProjectName.get(), createRefmap.get(), extraVersionMappings.get(), stablePlayer.get())
    }
}

val fabricShaking = FabricShakingExtension(project)
project.extensions.add("fabricShaking", fabricShaking)
