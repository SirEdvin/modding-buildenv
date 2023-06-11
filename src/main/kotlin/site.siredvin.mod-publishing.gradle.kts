import net.darkhax.curseforgegradle.TaskPublishCurseForge
import org.jetbrains.changelog.date

plugins {
    id("net.darkhax.curseforgegradle")
    id("com.modrinth.minotaur")
    id("org.jetbrains.changelog")
}

class ModPublishingExtension(private val targetProject: Project) {
    val output: Property<AbstractArchiveTask> = targetProject.objects.property(AbstractArchiveTask::class.java)
    val requiredDependencies: ListProperty<String> = targetProject.objects.listProperty(String::class.java)
    val requiredDependenciesCurseforge: ListProperty<String> = targetProject.objects.listProperty(String::class.java)
    val requiredDependenciesModrinth: ListProperty<String> = targetProject.objects.listProperty(String::class.java)
    init {
        requiredDependencies.convention(emptyList())
        requiredDependenciesCurseforge.convention(emptyList())
        requiredDependenciesModrinth.convention(emptyList())
        output.finalizeValueOnRead()
    }

    fun shake() {
        val rootProjectDir: File by targetProject.extra
        val minecraftVersion: String by targetProject.extra
        val modVersion: String by targetProject.extra
        val CURSEFORGE_RELEASE_TYPE: String by targetProject.extra
        val CURSEFORGE_ID: String by targetProject.extra
        val curseforgeKey: String by targetProject.extra
        val MODRINTH_ID: String by targetProject.extra
        val MODRINTH_RELEASE_TYPE: String by targetProject.extra
        val modrinthKey: String by targetProject.extra

        val isUnstable = modVersion.split("-").size > 1

        if (!isUnstable) {
            targetProject.changelog {
                version.set(modVersion)
                path.set("$rootProjectDir/CHANGELOG.md")
                header.set(provider { "[${version.get()}] - ${date()}" })
                itemPrefix.set("-")
                keepUnreleasedSection.set(true)
                unreleasedTerm.set("[Unreleased]")
                groups.set(listOf())
            }

            val publishCurseForge by targetProject.tasks.registering(TaskPublishCurseForge::class) {
                group = PublishingPlugin.PUBLISH_TASK_GROUP
                description = "Upload artifacts to CurseForge"
                apiToken = curseforgeKey
                enabled = apiToken != ""

                val mainFile = upload(CURSEFORGE_ID, output.get().archiveFile)
                mainFile.releaseType = CURSEFORGE_RELEASE_TYPE
                mainFile.gameVersions.add(minecraftVersion)
                mainFile.changelog = closureOf<String> {
                    targetProject.changelog.renderItem(targetProject.changelog.get(modVersion).withHeader(false))
                }
                mainFile.changelogType = "markdown"
                requiredDependencies.get().forEach(mainFile::addRequirement)
                requiredDependenciesCurseforge.get().forEach(mainFile::addRequirement)
                try {
                    val checkTask = targetProject.tasks.getByName("check")
                    this.dependsOn(checkTask)
                } catch (ignored: UnknownTaskException) {
                }
            }

            targetProject.modrinth {
                token.set(modrinthKey)
                projectId.set(MODRINTH_ID)
                versionNumber.set("$minecraftVersion-${targetProject.version}")
                versionName.set("$minecraftVersion-${targetProject.version}")
                versionType.set(MODRINTH_RELEASE_TYPE)
                uploadFile.set(output.get())
                gameVersions.add(minecraftVersion)
                changelog.set(targetProject.provider {
                        targetProject.changelog.renderItem(
                            targetProject.changelog.get(modVersion).withHeader(false),
                        )
                    }
                )
                dependencies {
                    requiredDependencies.get().forEach(required::project)
                    requiredDependenciesModrinth.get().forEach(required::project)
                }
            }

            try {
                targetProject.tasks.getByName("modrinth").dependsOn(targetProject.tasks.getByName("check"))
            } catch (ignored: UnknownTaskException) {
            }
        }
    }
}

val modPublishing = ModPublishingExtension(project)
project.extensions.add("modPublishing", modPublishing)
