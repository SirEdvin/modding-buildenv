plugins {
    java
}

repositories {
    mavenCentral()
    maven("https://repo.repsy.io/siredvin/default") {
        name = "Repsy"
        content {
            includeGroupByRegex("site\\.siredvin.*")
        }
    }
    maven("https://maven.squiddev.cc/") {
        content {
            includeGroup("cc.tweaked")
            includeGroup("dan200.computercraft")
            includeGroup("org.squiddev")
        }
    }
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") {
        content {
            includeGroup("fuzs.forgeconfigapiport")
        }
    }
    maven("https://maven.minecraftforge.net/") {
        content {
            includeGroup("net.minecraftforge")
            includeGroup("net.minecraftforge.gradle")
        }
    }
    maven("https://maven.neoforged.net/releases/") {
        content {
            includeGroup("net.neoforged")
            includeGroupByRegex("net\\.neoforged\\..*")
        }
    }
    maven("https://maven.parchmentmc.org/") {
        content {
            includeGroup("org.parchmentmc")
            includeGroupByRegex("org\\.parchmentmc\\..*")
        }
    }
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        content {
            includeGroup("org.spongepowered")
            includeGroupByRegex("org\\.spongepowered\\..*")
        }
    }
    maven("https://maven.fabricmc.net/") {
        content {
            includeGroup("net.fabricmc")
            includeGroup("fabric-loom")
            includeGroupByRegex("net\\.fabricmc\\..*")
        }
    }
}

fun connectIntegrationRepositories(targetProject: Project) {
    targetProject.repositories {
        maven {
            url = uri("https://www.cursemaven.com")
            name = "Curse Maven"
            content {
                includeGroup("curse.maven")
            }
        }
        maven {
            url = uri("https://api.modrinth.com/maven")
            name = "Modrinth"
            content {
                includeGroup("maven.modrinth")
            }
        }
        maven {
            url = uri("https://maven.architectury.dev/")
            content {
                includeGroup("dev.architectury")
            }
        }
        maven {
            url = uri("https://maven.shedaniel.me/")
            content {
                includeGroup("me.shedaniel.cloth")
                includeGroup("me.shedaniel")
            }
        }
    }
}

class BaseShakingExtension(private val targetProject: Project) {
    val projectPart: Property<String> = targetProject.objects.property(String::class.java)
    val integrationRepositories: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val projectName: Property<String> = targetProject.objects.property(String::class.java)
    val projectVersion: Property<String> = targetProject.objects.property(String::class.java)

    fun shake() {
        val minecraftVersion: String by targetProject.extra

        integrationRepositories.convention(false)

        if (targetProject.extra.has("modBaseName")) {
            val modBaseName: String by targetProject.extra
            projectName.convention(modBaseName)
        }
        if (targetProject.extra.has("modVersion")) {
            val modVersion: String by targetProject.extra
            projectVersion.convention(modVersion)
        }

        if (integrationRepositories.get()) {
            connectIntegrationRepositories(targetProject)
        }
        targetProject.base {
            archivesName.set("${projectName.get()}-${projectPart.get()}-$minecraftVersion")
            version = projectVersion.get()
        }

        targetProject.sourceSets.main.configure {
            resources.srcDir("src/generated/resources")
        }
    }
}

val baseShaking = BaseShakingExtension(project)
project.extensions.add("baseShaking", baseShaking)
