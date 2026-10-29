package com.puzzleblockblast.game.core.data.level

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.puzzleblockblast.game.core.engine.level.LevelBot
import com.puzzleblockblast.game.core.engine.level.LevelGame
import org.junit.Test
import java.io.File

/** Same content gate as the level pack: every event level winnable, 3 stars reachable. */
class EventPackBotTest {

    @Test
    fun bundledEventsAreValidWinnableAndThreeStarsAreReachable() {
        val result = EventPackParser.parse(File("src/main/assets/content/events/events_001.json").readText())
        assertWithMessage("event pack errors").that((result as? EventPackResult.Invalid)?.errors.orEmpty()).isEmpty()
        val events = (result as EventPackResult.Valid).events
        assertThat(events.map { it.id }).containsExactly("E01", "E02", "E03").inOrder()
        assertThat(events.all { it.levels.size == 8 }).isTrue()

        val levels = events.flatMap { it.levels }
        val results = levels.associate { it.id to LevelBot.play(LevelGame(it)) }
        File("build").mkdirs()
        File("build/event-report.json").writeText(
            results.entries.joinToString(",\n", "{\n", "\n}\n") { (id, r) ->
                """  "$id": {"won": ${r.won}, "fewestMoves": ${r.fewestMoves}, "stars": ${r.stars}}"""
            },
        )

        assertWithMessage("event levels the bot could not win").that(results.filterValues { !it.won }.keys).isEmpty()
        levels.forEach { level ->
            val fewest = results.getValue(level.id).fewestMoves
            assertWithMessage("${level.id}: 3 stars needs ${level.starMoves[1]}, bot needs $fewest")
                .that(level.starMoves[1]).isAtLeast(fewest)
        }
    }

    @Test
    fun rejectsBadEventFiles() {
        assertThat(EventPackParser.parse("{nope")).isInstanceOf(EventPackResult.Invalid::class.java)
        val wrongPrefix = """{"schemaVersion":1,"contentVersion":1,"events":[{"id":"E01","title":"X","levels":[
            {"id":"L0001","index":1,"boardSize":10,"initialBoard":[${List(10) { "\"..........\"" }.joinToString()}],
             "pieces":{"seed":1,"pool":["dot"]},"objectives":[{"type":"clearLines","count":1}],
             "limits":{"maxMoves":10},"stars":[8,5]}]}]}"""
        val result = EventPackParser.parse(wrongPrefix) as EventPackResult.Invalid
        assertThat(result.errors.joinToString()).contains("must start with E01L")
    }
}
