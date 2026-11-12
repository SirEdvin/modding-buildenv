//import com.diffplug.gradle.spotless.FormatExtension
//import com.diffplug.spotless.LineEnding
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import kotlin.io.path.Path

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("groovy-gradle-plugin")
    id("com.github.ben-manes.versions") version "0.51.0"
}

val projectVersion: String by extra
val projectGroup: String by extra
val projectName: String by extra

base {
    group = projectGroup
    archivesName.set(projectName)
    version = projectVersion
}
java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

// Duplicated in settings.gradle.kts
repositories {
    mavenCentral()
    gradlePluginPortal()

    // Upstream repositories for plugin resolution; the retired SirEdvin mirror is no longer used
    maven("https://maven.minecraftforge.net") {
        content {
            includeGroup("net.minecraftforge")
            includeGroup("net.minecraftforge.gradle")
        }
    }
    maven("https://maven.neoforged.net/releases") {
        content {
            includeGroup("net.neoforged")
            includeGroupByRegex("net\\.neoforged\\..*")
        }
    }
    maven("https://maven.parchmentmc.org") {
        content {
            includeGroup("org.parchmentmc")
            includeGroupByRegex("org\\.parchmentmc\\..*")
        }
    }
    maven("https://repo.spongepowered.org/repository/maven-public") {
        content {
            includeGroup("org.spongepowered")
            includeGroupByRegex("org\\.spongepowered\\..*")
        }
    }
    maven("https://maven.fabricmc.net") {
        content {
            includeGroup("net.fabricmc")
            includeGroup("fabric-loom")
            includeGroupByRegex("net\\.fabricmc\\..*")
        }
    }
}

dependencies {
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.spotless)
    implementation(libs.plugin.vanillaGradle)
    implementation(libs.plugin.loom)
    implementation(libs.plugin.curseForgeGradle)
    implementation(libs.plugin.minotaur)
    implementation(libs.plugin.changelog)
    implementation(libs.plugin.forgeGradle)
    implementation(libs.plugin.modDevGradle)
    implementation(libs.plugin.librarian)
    implementation(libs.plugin.mixinGradle)
    implementation(libs.plugin.taskTree)
    implementation(libs.plugin.github)
}

//spotless {
//    encoding = StandardCharsets.UTF_8
//    lineEndings = LineEnding.UNIX
//
//    fun FormatExtension.defaults() {
//        endWithNewline()
//        trimTrailingWhitespace()
//        indentWithSpaces(4)
//    }
//
//    java {
//        defaults()
//        removeUnusedImports()
//    }
//
//    val ktlintConfig = mapOf(
//        "ktlint_standard_no-wildcard-imports" to "disabled",
//    )
//
//    kotlinGradle {
//        defaults()
//        ktlint().editorConfigOverride(ktlintConfig)
//    }
//
//    kotlin {
//        defaults()
//        ktlint().editorConfigOverride(ktlintConfig)
//    }
//}

gradlePlugin {
    website.set("https://github.com/SirEdvin")
    vcsUrl.set("https://github.com/SirEdvin/Peripheralium")
    group = projectGroup
    version = projectVersion

}

tasks.publish {
    dependsOn("check")
}

publishing {
    repositories {
        // Repsy accepts an API/deploy token as the password
        maven("https://repo.repsy.io/siredvin/default") {
            name = "Repsy"
            credentials(PasswordCredentials::class)
        }
    }
}
