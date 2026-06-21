plugins {
    java
    id("net.neoforged.moddev")
}

fun configureNeoForge(
    targetProject: Project,
    projectName: String,
    commonProjectName: String,
    useAT: Boolean,
    useMixins: Boolean,
    useRawJar: Boolean,
    versionMappings: Map<String, String>,
    rawVersionMappings: Map<String, String>,
) {
    val extractedLibs = targetProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
    val neoforgeVersion = extractedLibs.findVersion("neoforge").get().requiredVersion

    targetProject.neoForge {
        version = neoforgeVersion
        parchment {
            minecraftVersion = extractedLibs.findVersion("parchmentMc").get().requiredVersion
            mappingsVersion = extractedLibs.findVersion("parchment").get().requiredVersion
        }
        if (useAT) {
            validateAccessTransformers = true
        }
        runs {
            configureEach {
                systemProperty("neoforge.logging.markers", "REGISTRIES")
                systemProperty("neoforge.logging.console.level", "debug")
            }

            create("client") {
                client()
                gameDirectory = targetProject.file("run")
            }

            create("server") {
                server()
                gameDirectory = targetProject.file("run/server")
                programArgument("--nogui")
            }

            create("data") {
                data()
                gameDirectory = targetProject.file("run")
                programArgument("--mod")
                programArgument(projectName)
                programArgument("--all")
                programArgument("--output")
                programArgument(targetProject.file("src/generated/resources").absolutePath)
                if (commonProjectName.isNotEmpty()) {
                    programArgument("--existing")
                    programArgument(targetProject.project(":$commonProjectName").file("src/main/resources").absolutePath)
                }
                programArgument("--existing")
                programArgument(targetProject.file("src/main/resources").absolutePath)
            }
        }
        mods {
            create(projectName) {
                sourceSet(targetProject.sourceSets.main.get())
            }
        }
    }

    targetProject.dependencies {
        if (commonProjectName.isNotEmpty()) {
            compileOnly(project(":$commonProjectName")) {
                exclude("cc.tweaked")
                exclude("fuzs.forgeconfigapiport")
                exclude("dan200.computercraft")
            }
        }
    }

    if (useMixins) {
        targetProject.dependencies {
            annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
        }
    }

    targetProject.tasks {
        processResources {
            if (commonProjectName.isNotEmpty()) {
                from(project(":$commonProjectName").sourceSets.main.get().resources)
            }

            inputs.property("version", targetProject.version)
            inputs.property("neoforgeVersion", neoforgeVersion)
            val basePropertyMap = mutableMapOf<String, Any>(
                "neoforgeVersion" to neoforgeVersion,
                "neoforge_version" to neoforgeVersion,
                "file" to mapOf("jarVersion" to targetProject.version),
                "version" to targetProject.version,
            )
            rawVersionMappings.entries.forEach {
                basePropertyMap["${it.key}Version"] = it.value
            }
            versionMappings.entries.forEach {
                val versionForValue = extractedLibs.findVersion(it.value)
                if (versionForValue.isEmpty) {
                    error("Cannot find version for mapping ${it.value}")
                }
                inputs.property("${it.key}Version", versionForValue.get())
                basePropertyMap["${it.key}Version"] = versionForValue.get()
            }
            filesMatching("META-INF/neoforge.mods.toml") {
                expand(basePropertyMap)
            }
            exclude(".cache")
        }
        if (commonProjectName.isNotEmpty()) {
            withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                if (name == "compileKotlin") {
                    source(project(":$commonProjectName").sourceSets.main.get().allSource)
                }
            }
            withType<JavaCompile> {
                if (name == "compileJava") {
                    source(project(":$commonProjectName").sourceSets.main.get().allSource)
                }
            }
        }
    }

    targetProject.ext.set("releaseJar", "jar")

    if (useRawJar) {
        targetProject.configurations.create("raw") {
            isCanBeConsumed = true
        }

        val rawJar = targetProject.tasks.register<Jar>("rawJar") {
            dependsOn(targetProject.tasks.named("jar"))
            archiveBaseName.set(archiveBaseName.get() + "-raw")
            archiveClassifier.set("raw")
            from(targetProject.sourceSets.main.get().output)
        }

        targetProject.tasks.named("jar") {
            finalizedBy(rawJar)
        }

        targetProject.artifacts {
            add("raw", rawJar) {
                classifier = "raw"
            }
        }
    }
}

class NeoForgeShakingExtension(private val targetProject: Project) {
    val commonProjectName: Property<String> = targetProject.objects.property(String::class.java)
    val projectName: Property<String> = targetProject.objects.property(String::class.java)
    val useAT: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val useMixins: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val useRawJar: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val publishRawJar: Property<Boolean> = useRawJar
    val extraVersionMappings: MapProperty<String, String> = targetProject.objects.mapProperty(String::class.java, String::class.java)
    val extraRawVersionMappings: MapProperty<String, String> = targetProject.objects.mapProperty(String::class.java, String::class.java)

    fun shake() {
        commonProjectName.convention("")
        useAT.convention(false)
        useMixins.convention(false)
        useRawJar.convention(false)
        extraVersionMappings.convention(emptyMap())
        extraRawVersionMappings.convention(emptyMap())
        if (targetProject.extra.has("modBaseName")) {
            val modBaseName: String by targetProject.extra
            projectName.convention(modBaseName)
        }
        configureNeoForge(
            targetProject,
            projectName.get(),
            commonProjectName.get(),
            useAT.get(),
            useMixins.get(),
            useRawJar.get(),
            extraVersionMappings.get(),
            extraRawVersionMappings.get(),
        )
    }
}

val neoforgeShaking = NeoForgeShakingExtension(project)
project.extensions.add("neoforgeShaking", neoforgeShaking)
