# Modding buildenv

So, this is collection of gradle pre-compile scripts to setup multi-loader minecraft modding env.

For now it very specific, so please, don't use it by youself :)

## Publishing

GitHub Actions publishes artifacts to the SirEdvin Maven repository when `projectVersion` in `gradle.properties` changes and the workflow runs from `main`, `master`, a version-like tag, or manual dispatch.

The workflow checks `https://mvn.siredvin.site/minecraft/site/siredvin/modding-buildenv/<version>/` first. If that version already exists, it still runs `./gradlew --no-daemon check` but skips publishing to avoid duplicate publish failures. If the version is absent, it runs `./gradlew --no-daemon publishAllPublicationsToSirEdvinRepository`.

Configure these GitHub repository secrets before publishing:

- `SIR_EDVIN_MAVEN_USERNAME`
- `SIR_EDVIN_MAVEN_PASSWORD`

## Stonecutter consumers

Stonecutter is owned by the consuming project's settings, not this toolkit. Stable 0.9.8 works with Gradle 9 consumers; the toolkit itself retains its Gradle 8 wrapper because generating the legacy ForgeGradle convention accessors is not supported on Gradle 9. Build the toolkit with its own wrapper and use a local file Maven repository for consumer verification, rather than a Gradle 9 composite build. Do not commit local repositories or consume an unpublished version in CI.

Version 0.9.3 tracks the complete loader metadata expansion map and shared build script as `processResources` inputs for Fabric, Forge, and NeoForge. Changing a raw dependency version must regenerate metadata even if the mod's own version is unchanged. Test this without `clean` or `--rerun-tasks`, then inspect the resulting resource/JAR.

Consumers still own unique internal module identities, public Maven coordinates/capabilities, shared-source overlay precedence, effective access-transformer inputs, and `createMinecraftArtifacts` ordering after `stonecutterGenerate`. Check all version/loader nodes in both active states, plus fresh nonempty GameTest reports. A successful Minecraft process can still have failed during mod loading.
