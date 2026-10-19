package com.puzzleblockblast.game.core.data.level

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.puzzleblockblast.game.core.engine.level.LevelBot
import com.puzzleblockblast.game.core.engine.level.LevelGame
import org.junit.Test
import java.io.File

/** Bundled pack location, relative to the module directory (Gradle's working dir for unit tests). */
internal val BUNDLED_PACK = File("src/main/assets/content/levels/pack_001.json")

internal fun bundledPack(): LevelPack {
    val result = LevelPackParser.parse(BUNDLED_PACK.readText())
    assertWithMessage("bundled pack errors").that((result as? LevelPackResult.Invalid)?.errors.orEmpty()).isEmpty()
    return (result as LevelPackResult.Valid).pack
}

/**
 * Content quality gate (docs/PLAN.md §5): every bundled level must be winnable by a bot that only
 * sees the current tray, and 3 stars must be reachable (the bot wins within the 3-star move count).
 * Writes build/level-report.json, which tools/levels/build_pack_001.py uses to calibrate stars.
 */
class LevelPackBotTest {

    @Test
    fun everyBundledLevelIsWinnableAndThreeStarsAreReachable() {
        val pack = bundledPack()
        assertThat(pack.levels).hasSize(50)

        val results = pack.levels.associate { it.id to LevelBot.play(LevelGame(it)) }

        File("build").mkdirs()
        File("build/level-report.json").writeText(
            results.entries.joinToString(",\n", "{\n", "\n}\n") { (id, r) ->
                """  "$id": {"won": ${r.won}, "fewestMoves": ${r.fewestMoves}, "stars": ${r.stars}}"""
            },
        )

        val unwinnable = results.filterValues { !it.won }.keys
        assertWithMessage("levels the bot could not win").that(unwinnable).isEmpty()
        pack.levels.forEach { level ->
            val fewest = results.getValue(level.id).fewestMoves
            assertWithMessage("${level.id}: 3 stars needs ${level.starMoves[1]} moves, bot's best is $fewest")
                .that(level.starMoves[1]).isAtLeast(fewest)
        }
    }
}
