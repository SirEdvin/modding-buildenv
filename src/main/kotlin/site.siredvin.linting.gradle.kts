import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.spotless.LineEnding
import java.nio.charset.StandardCharsets

plugins {
    `java-library`
    id("com.diffplug.spotless")
}

spotless {
    encoding = StandardCharsets.UTF_8
    lineEndings = LineEnding.UNIX

    fun FormatExtension.defaults() {
        endWithNewline()
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
    }

    java {
        defaults()
        removeUnusedImports()
    }

    val ktlintConfig = mapOf(
        "ktlint_standard_no-wildcard-imports" to "disabled",
        "ktlint_standard_package-name" to "disabled",
    )

    kotlinGradle {
        defaults()
        ktlint().editorConfigOverride(ktlintConfig)
    }

    kotlin {
        defaults()
        ktlint().editorConfigOverride(ktlintConfig)
    }
}
