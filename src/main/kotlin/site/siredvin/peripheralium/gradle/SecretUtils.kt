package site.siredvin.peripheralium.gradle

import java.io.File

fun collectSecrets(path: String = ".env"): Map<String, String> {
    val env = hashMapOf<String, String>()

    val file = File(path)
    if (file.exists()) {
        file.readLines().forEach { line ->
            val splitResult = line.split("=")
            if (splitResult.size > 1) {
                env[splitResult[0].trim()] = splitResult[1].trim()
            }
        }
    }

    return env
}