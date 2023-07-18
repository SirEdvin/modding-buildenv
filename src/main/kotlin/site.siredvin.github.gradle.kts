import org.jetbrains.changelog.date
import org.jetbrains.changelog.Changelog
import site.siredvin.peripheralium.gradle.collectSecrets

plugins {
    java
    id("com.github.breadmoirai.github-release")
    id("org.jetbrains.changelog")
}

fun configureGithubAndChangelog(config: GithubShakingExtension) {
    val minecraftVersion: String by config.targetProject.extra
    val modVersion: String by config.targetProject.extra

    val secretEnv = collectSecrets()
    val githubToken = secretEnv["GITHUB_TOKEN"] ?: System.getenv("GITHUB_TOKEN") ?: ""
    config.targetProject.apply(plugin = "com.github.breadmoirai.github-release")
    config.targetProject.apply(plugin = "org.jetbrains.changelog")

    config.targetProject.changelog {
        version.set(config.targetProject.version.toString())
        path.set("CHANGELOG.md")
        header.set(provider { "[${version.get()}] - ${date()}" })
        itemPrefix.set("-")
        keepUnreleasedSection.set(true)
        unreleasedTerm.set("[Unreleased]")
        groups.set(listOf())
    }

    config.targetProject.githubRelease {
        setTagName("v$minecraftVersion-$modVersion")
        setReleaseName("v$minecraftVersion-$modVersion")
        owner.set(config.projectOwner.get())
        repo.set(config.projectRepo.get())
        setToken(githubToken)
        targetCommitish.set(config.modBranch.get())
        setBody(changelog.renderItem(
            changelog.getUnreleased().withHeader(false).withEmptySections(false),
            Changelog.OutputType.MARKDOWN
        ))
        prerelease.set(config.preRelease.get())
        dryRun.set(config.dryRun.get())
        if (config.useForge.get()) {
            releaseAssets.from(provider { project(":forge").tasks.jar })
        }
        if (config.useFabric.get()) {
            releaseAssets.from(provider { project(":fabric").tasks.getByName("remapJar") })
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
    val useForge: Property<Boolean> = targetProject.objects.property(Boolean::class.java)
    fun shake() {
        val modBaseName: String by targetProject.extra
        projectOwner.convention("siredvin")
        projectRepo.convention(modBaseName)
        useFabric.convention(true)
        useForge.convention(true)
        dryRun.convention(false)

        val modVersion: String by targetProject.extra
        val isUnstable = modVersion.split("-").size > 1
        preRelease.convention(isUnstable)

        configureGithubAndChangelog(this)
    }
}

val githubShaking: GithubShakingExtension = GithubShakingExtension(project)
project.extensions.add("githubShaking", githubShaking)
