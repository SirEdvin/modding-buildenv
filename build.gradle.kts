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
}

val projectVersion: String by extra
val projectGroup: String by extra
val projectName: String by extra

base {
    group = projectGroup
    archivesName.set(projectName)
    version = projectVersion
}

// Duplicated in settings.gradle.kts
repositories {
    mavenCentral()
    gradlePluginPortal()

    maven("https://mvn.siredvin.site/minecraft") {
        name = "SirEdvin's Minecraft repository"
        content {
            includeGroup("net.minecraftforge")
            includeGroup("net.minecraftforge.gradle")
            includeGroup("org.parchmentmc")
            includeGroup("org.parchmentmc.feather")
            includeGroup("org.parchmentmc.data")
            includeGroup("org.spongepowered")
            includeGroup("net.fabricmc")
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
        maven("https://mvn.siredvin.site/minecraft") {
            name = "SirEdvin"
            credentials(PasswordCredentials::class)
        }
    }
}
