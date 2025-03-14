plugins {
    `java`
    id("net.minecraftforge.gradle")
    id("org.parchmentmc.librarian.forgegradle")
    id("org.spongepowered.mixin")
}

fun configureForge(targetProject: Project, projectName: String, useAT: Boolean, commonProjectName: String, useMixins: Boolean, useJarJar: Boolean, versionMappings: Map<String, String>,rawVersionMappings: Map<String, String>) {
    val minecraftVersion: String by targetProject.extra

    targetProject.minecraft {
        val extractedLibs = targetProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        mappings(
            "parchment",
            "${extractedLibs.findVersion("parchmentMc").get()}-${
                extractedLibs.findVersion("parchment").get()
            }-$minecraftVersion",
        )

        if (useAT) {
            accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))
        }

        runs {
            all {
                property("forge.logging.markers", "REGISTRIES")
                property("forge.logging.console.level", "debug")
                property("mixin.env.remapRefMap", "true")
                property("mixin.env.refMapRemappingFile", "${targetProject.projectDir}/build/createSrgToMcp/output.srg")
            }

            val client by registering {
                workingDirectory(file("run"))
            }

            val server by registering {
                workingDirectory(file("run/server"))
                arg("--nogui")
            }

            val data by registering {
                workingDirectory(file("run"))
                args(
                    "--mod", projectName, "--all",
                    "--output", file("src/generated/resources/"),
                    "--existing", project(":$commonProjectName").file("src/main/resources/"),
                    "--existing", file("src/main/resources/"),
                )
            }
        }
    }

    targetProject.dependencies {
        val extractedLibs = targetProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        minecraft("net.minecraftforge:forge:$minecraftVersion-${extractedLibs.findVersion("forge").get()}")

        if (commonProjectName.isNotEmpty()) {

            compileOnly(project(":$commonProjectName")) {
                exclude("cc.tweaked")
                exclude("fuzs.forgeconfigapiport")
                exclude("dan200.computercraft")
            }
        }
    }

    if (useMixins) {
        targetProject.mixin {
            add(targetProject.sourceSets.main.get(), "$projectName.refmap.json")
            config("$projectName.mixins.json")
        }
        targetProject.dependencies {
            annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
        }
    }

    targetProject.tasks {
        val extractedLibs = targetProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        val forgeVersion = extractedLibs.findVersion("forge").get()

        processResources {
            if (commonProjectName.isNotEmpty()) {
                from(project(":$commonProjectName").sourceSets.main.get().resources)
            }

            inputs.property("version", targetProject.version)
            inputs.property("forgeVersion", forgeVersion)
            val basePropertyMap = mutableMapOf(
                "forgeVersion" to forgeVersion,
                "file" to mapOf("jarVersion" to targetProject.version),
                "version" to targetProject.version,
            )
            rawVersionMappings.entries.forEach {
                basePropertyMap["${it.key}Version"] = it.value
            }
            versionMappings.entries.forEach {
                inputs.property("${it.key}Version", extractedLibs.findVersion(it.value).get())
                basePropertyMap["${it.key}Version"] = extractedLibs.findVersion(it.value).get()
            }
            filesMatching("META-INF/mods.toml") {
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

    if (useJarJar) {
        targetProject.ext.set("releaseJar", "jarJar")
        jarJar.enable()
        tasks.jarJar {
            finalizedBy("reobfJarJar")
            archiveClassifier.set("")
        }
        tasks.jar {
            finalizedBy("reobfJar")
            archiveClassifier.set("slim")
        }

        tasks.assemble { dependsOn("jarJar") }

    } else {
        tasks.jar {
            finalizedBy("reobfJar")
            archiveClassifier.set("")
        }
        targetProject.ext.set("releaseJar", "jar")
    }
}

class ForgeShakingExtension(private val targetProject: Project) {
    val commonProjectName: Property<String> = targetProject.objects.property(String::class.java)
    val projectName: Property<String> = targetProject.objects.property(String::class.java)
    val useAT: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val useMixins: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val useJarJar: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val extraVersionMappings: MapProperty<String, String> = targetProject.objects.mapProperty(String::class.java, String::class.java)
    val extraRawVersionMappings: MapProperty<String, String> = targetProject.objects.mapProperty(String::class.java, String::class.java)

    fun shake() {
        useMixins.convention(false)
        useJarJar.convention(false)
        extraVersionMappings.convention(emptyMap())
        extraRawVersionMappings.convention(emptyMap())
        if (targetProject.extra.has("modBaseName")) {
            val modBaseName: String by targetProject.extra
            projectName.convention(modBaseName)
        }
        configureForge(targetProject, projectName.get(), useAT.get(), commonProjectName.get(), useMixins.get(), useJarJar.get(), extraVersionMappings.get(), extraRawVersionMappings.get())
    }
}

val forgeShaking: ForgeShakingExtension = ForgeShakingExtension(project)
project.extensions.add("forgeShaking", forgeShaking)

repositories {
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        content {
            includeGroup("thedarkcolour")
        }
    }
}