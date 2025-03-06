import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    `maven-publish`
}

class PublishingShakingExtension(private val targetProject: Project) {
    val projectVersion: Property<String> = targetProject.objects.property(String::class.java)

    fun shake() {
        if (targetProject.extra.has("modVersion")) {
            val modVersion: String by targetProject.extra
            projectVersion.convention(modVersion)
        }
        targetProject.publishing {
            publications {
                register<MavenPublication>("maven") {
                    artifactId = targetProject.base.archivesName.get()
                    from(components["java"])
                }
            }

            repositories {
                val isUnstable = projectVersion.get().split("-").size > 1
                if (isUnstable) {
                    maven("https://mvn.siredvin.site/snapshots") {
                        name = "SirEdvin"
                        credentials(PasswordCredentials::class)
                    }
                } else {
                    maven("https://mvn.siredvin.site/minecraft") {
                        name = "SirEdvin"
                        credentials(PasswordCredentials::class)
                    }
                }
            }
        }
        targetProject.tasks.withType(PublishToMavenRepository::class.java) {
            this.dependsOn(targetProject.tasks.check)
        }
        targetProject.tasks.withType(PublishToMavenLocal::class.java) {
            this.dependsOn(targetProject.tasks.check)
        }
    }
}

val publishingShaking = PublishingShakingExtension(project)
project.extensions.add("publishingShaking", publishingShaking)
