package com.puzzleblockblast.game

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Guards against text that was decoded with the wrong charset and saved back ("★" becoming "â˜…"),
 * which once shipped in the level result screen. Scans every source and resource file in the repo.
 */
class SourceEncodingTest {

    /** A Latin-1 lead byte of a UTF-8 sequence followed by a continuation-range character. */
    private val mojibake = Regex("[ÃÂâð][\\u0080-\\u2122]")
    private val extensions = setOf("kt", "kts", "xml", "toml", "json", "md", "py", "rules", "mjs")

    @Test
    fun noDoubleEncodedTextInTheRepo() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }
        val offenders = root.walkTopDown()
            .onEnter { it.name !in setOf("build", ".gradle", ".git", ".kotlin", "node_modules", ".idea") }
            .filter { it.isFile && it.extension in extensions && it.name != "SourceEncodingTest.kt" }
            .flatMap { file ->
                file.readLines().withIndex().filter { (_, line) -> mojibake.containsMatchIn(line) }
                    .map { (i, line) -> "${file.relativeTo(root)}:${i + 1}: ${line.trim()}" }
            }
            .toList()

        assertWithMessage("double-encoded text").that(offenders).isEmpty()
    }
}
