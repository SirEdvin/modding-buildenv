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
                // Repsy accepts an API/deploy token as the password
                maven("https://repo.repsy.io/siredvin/default") {
                    name = "Repsy"
                    credentials(PasswordCredentials::class)
                }
            }
        }
        // There is no snapshots repository: refuse before anything is built or uploaded
        targetProject.gradle.taskGraph.whenReady {
            if (projectVersion.get().endsWith("-SNAPSHOT") && allTasks.any { it is PublishToMavenRepository && it.project == targetProject }) {
                throw GradleException("Refusing to publish ${projectVersion.get()}: -SNAPSHOT versions cannot be published, the snapshots repository was retired. Use publishToMavenLocal or a release version.")
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
