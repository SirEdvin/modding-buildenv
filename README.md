# Modding buildenv

So, this is collection of gradle pre-compile scripts to setup multi-loader minecraft modding env.

For now it very specific, so please, don't use it by youself :)

## Publishing

GitHub Actions publishes artifacts to the SirEdvin Maven repository when `projectVersion` in `gradle.properties` changes and the workflow runs from `main`, `master`, a version-like tag, or manual dispatch.

The workflow checks `https://mvn.siredvin.site/minecraft/site/siredvin/modding-buildenv/<version>/` first. If that version already exists, it still runs `./gradlew --no-daemon check` but skips publishing to avoid duplicate publish failures. If the version is absent, it runs `./gradlew --no-daemon publishAllPublicationsToSirEdvinRepository`.

Configure these GitHub repository secrets before publishing:

- `SIR_EDVIN_MAVEN_USERNAME`
- `SIR_EDVIN_MAVEN_PASSWORD`
