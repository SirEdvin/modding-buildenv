import groovy.json.JsonSlurper
import org.gradle.internal.impldep.com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.changelog.date
import org.jetbrains.changelog.Changelog
import site.siredvin.peripheralium.gradle.collectSecrets
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Paths
import kotlin.io.path.pathString

plugins {
    java
    id("com.github.breadmoirai.github-release")
    id("org.jetbrains.changelog")
}

fun configureGithubAndChangelog(config: GithubShakingExtension) {
    val minecraftVersion: String by config.targetProject.extra
    val isUnstable = config.projectVersion.get().split("-").size > 1

    val secretEnv = collectSecrets()
    val githubToken = secretEnv["GITHUB_TOKEN"] ?: System.getenv("GITHUB_TOKEN") ?: ""
    val mastodonToken = secretEnv["MASTODON_TOKEN"] ?: System.getenv("MASTODON_TOKEN") ?: ""
    config.targetProject.apply(plugin = "com.github.breadmoirai.github-release")
    config.targetProject.apply(plugin = "org.jetbrains.changelog")

    config.targetProject.changelog {
        version.set(config.targetProject.version.toString())
        path.set(Paths.get(config.targetProject.rootDir.absolutePath, "CHANGELOG.md").pathString)
        header.set(provider { "[${version.get()}] - ${date()}" })
        itemPrefix.set("-")
        keepUnreleasedSection.set(true)
        unreleasedTerm.set("[Unreleased]")
        groups.set(listOf())
    }

    config.targetProject.githubRelease {
        this.tagName.set("v$minecraftVersion-${config.projectVersion.get()}")
        this.releaseName.set("v$minecraftVersion-${config.projectVersion.get()}")
        owner.set(config.projectOwner.get())
        repo.set(config.projectRepo.get())
        setToken(githubToken)
        targetCommitish.set(config.modBranch.get())
        this.body.set(provider {
            if (isUnstable) {
                changelog.renderItem(
                    changelog.getUnreleased().withHeader(false).withEmptySections(false),
                    Changelog.OutputType.MARKDOWN
                )
            } else {
                changelog.renderItem(
                    changelog.getLatest().withHeader(false).withEmptySections(false),
                    Changelog.OutputType.MARKDOWN
                )
            }
        })
        prerelease.set(config.preRelease.get())
        dryRun.set(config.dryRun.get())
        if (config.useRoot.get()) {
            releaseAssets.from(provider { config.targetProject.tasks.getByName( config.targetProject.ext.get("releaseJar")?.toString() ?: config.rootJarName.get()) })
        }
        if (config.useForge.get()) {
            releaseAssets.from(provider { project(":forge").tasks.getByName(project(":forge").ext.get("releaseJar")?.toString() ?: "jar") })
        }
        if (config.useFabric.get()) {
            releaseAssets.from(provider { project(":fabric").tasks.getByName(project(":fabric").ext.get("releaseJar")?.toString() ?: "remapJar") })
        }
    }

    config.targetProject.tasks.register("notifyMastodon") {
        doLast {
            val objectMapper = ObjectMapper()
            val requestBody: String = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(
                    mapOf(
                        "status" to """
            New #${config.mastodonProjectName.get()}, addon for #ComputerCraft  release! Version ${config.projectVersion.get()} for minecraft $minecraftVersion released!
            
            You can find more details by github release notes: https://github.com/${config.projectOwner.get()}/${config.projectRepo.get()}/releases/tag/v$minecraftVersion-${config.projectVersion.get()}
            
            #ModdedMinecraft #fabricmc #minecraftforge
        """.trimIndent()
                    )
                )
            val url = URL("https://mastodon.social/api/v1/statuses")
            val req = HttpRequest.newBuilder(url.toURI())
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $mastodonToken")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)).build()
            val res = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
            if (res.statusCode() >= 400)
                throw Exception(res.body())
            JsonSlurper().parseText(res.body())
        }
    }
}

class GithubShakingExtension(val targetProject: Project) {
    val projectOwner: Property<String> = targetProject.objects.property(String::class.java)
    val projectRepo: Property<String> = targetProject.objects.property(String::class.java)
    val modBranch: Property<String> = targetProject.objects.property(String::class.java)
    val preRelease: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val dryRun: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val useFabric: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val useRoot: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val rootJarName: Property<String> = targetProject.objects.property(String::class.java)
    val useForge: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val useForgeJarJar: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    val mastodonProjectName: Property<String> = targetProject.objects.property(String::class.java)
    val mastodonTags: ListProperty<String> = targetProject.objects.listProperty(String::class.java)
    val projectVersion: Property<String> = targetProject.objects.property(String::class.java)
    fun shake() {
        val modBaseName: String by targetProject.extra
        projectOwner.convention("siredvin")
        projectRepo.convention(modBaseName)
        useFabric.convention(true)
        useForge.convention(true)
        useRoot.convention(false)
        rootJarName.convention("remapJar")
        useForgeJarJar.convention(false)
        dryRun.convention(false)
        mastodonProjectName.convention(provider {
            projectRepo.get().capitalize()
        })
        mastodonTags.convention(listOf())

        if (targetProject.extra.has("modVersion")) {
            val modVersion: String by targetProject.extra
            projectVersion.convention(modVersion)
        }

        val isUnstable = projectVersion.get().split("-").size > 1
        preRelease.convention(isUnstable)

        configureGithubAndChangelog(this)
    }
}

val githubShaking: GithubShakingExtension = GithubShakingExtension(project)
project.extensions.add("githubShaking", githubShaking)
