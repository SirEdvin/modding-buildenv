# Modding buildenv

So, this is collection of gradle pre-compile scripts to setup multi-loader minecraft modding env.

For now it very specific, so please, don't use it by youself :)

## Publishing

Repsy (`https://repo.repsy.io/siredvin/default`) is the only repository for `site.siredvin` artifacts. The former SirEdvin Maven (release and snapshots) is retired and is no longer referenced anywhere.

GitHub Actions publishes to Repsy when `projectVersion` in `gradle.properties` changes and the workflow runs from `main`, `master`, a version-like tag, or manual dispatch. It runs `./gradlew --no-daemon check`, then probes the Repsy POM for the version. If it already exists, publishing is skipped to avoid duplicate publish failures; otherwise it runs `./gradlew --no-daemon publishAllPublicationsToRepsyRepository`.

Required GitHub repository secret: `REPSY_TOKEN` (Repsy deploy token; the workflow maps it to `ORG_GRADLE_PROJECT_RepsyPassword`, with `RepsyUsername` = `siredvin`). A missing token now fails the publish step instead of silently skipping it.

### Changes in 0.9.4 for consumers

| Before | Now |
| --- | --- |
| `publishAllPublicationsToSirEdvinRepository` / `publishToSirEdvinRepository` tasks | **Removed.** Use `publishAllPublicationsToRepsyRepository` / `publishMavenPublicationToRepsyRepository`. |
| Gradle properties `SirEdvinUsername` / `SirEdvinPassword` (secrets `SIR_EDVIN_MAVEN_USERNAME`, `SIR_EDVIN_MAVEN_PASSWORD`) | **Removed.** Use `RepsyUsername` / `RepsyPassword` (secret `REPSY_TOKEN`). |
| Repository named `SirEdvin` (publishing) and `SirEdvin's Minecraft repository` (resolution) | **Removed.** Replaced by `Repsy`. |
| Versions with a `-suffix` (for example `-rc.1`) went to the snapshots repository | Go to Repsy like any release. Only a literal `-SNAPSHOT` version is rejected, and only when a `publish*ToMavenRepository` task is in the task graph (`publishToMavenLocal` still works). |
| `site.siredvin.base` resolved `cc.tweaked`, `dan200.computercraft`, `org.squiddev`, `fuzs.forgeconfigapiport`, `site.siredvin` from the mirror | Resolves them from Repsy (`site.siredvin*`), `maven.squiddev.cc`, and Fuzz's modresources, plus Forge, NeoForged, ParchmentMC, Sponge and Fabric upstreams, all with content filters. |

The `Repsy` repository name and `RepsyUsername`/`RepsyPassword` credentials are unchanged. Consumers must drop the retired SirEdvin Maven from their own `pluginManagement.repositories`, and keep `Repsy` plus upstream plugin repositories (Forge, NeoForged, ParchmentMC, Sponge, Fabric) there with `site.siredvin.*` mapped to `site.siredvin:modding-buildenv:<version>`.

## Stonecutter consumers

Stonecutter is owned by the consuming project's settings, not this toolkit. Stable 0.9.8 works with Gradle 9 consumers; the toolkit itself retains its Gradle 8 wrapper because generating the legacy ForgeGradle convention accessors is not supported on Gradle 9. Build the toolkit with its own wrapper and use a local file Maven repository for consumer verification, rather than a Gradle 9 composite build. Do not commit local repositories or consume an unpublished version in CI.

Version 0.9.3 tracks the complete loader metadata expansion map and shared build script as `processResources` inputs for Fabric, Forge, and NeoForge. Changing a raw dependency version must regenerate metadata even if the mod's own version is unchanged. Test this without `clean` or `--rerun-tasks`, then inspect the resulting resource/JAR.

Consumers still own unique internal module identities, public Maven coordinates/capabilities, shared-source overlay precedence, effective access-transformer inputs, and `createMinecraftArtifacts` ordering after `stonecutterGenerate`. Check all version/loader nodes in both active states, plus fresh nonempty GameTest reports. A successful Minecraft process can still have failed during mod loading.
