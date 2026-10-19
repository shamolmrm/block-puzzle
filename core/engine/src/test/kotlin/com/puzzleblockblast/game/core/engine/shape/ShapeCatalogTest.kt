package com.puzzleblockblast.game.core.engine.shape

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShapeCatalogTest {

    private val catalog = ShapeCatalog.Classic

    private fun rotationsOf(base: String) = catalog.entries.count { it.shape.id.startsWith("${base}_") }

    @Test
    fun expandsDistinctRotationsOnly() {
        assertThat(rotationsOf("dot")).isEqualTo(1)
        assertThat(rotationsOf("i2")).isEqualTo(2)
        assertThat(rotationsOf("i5")).isEqualTo(2)
        assertThat(rotationsOf("sq2")).isEqualTo(1)
        assertThat(rotationsOf("sq3")).isEqualTo(1)
        assertThat(rotationsOf("c3")).isEqualTo(4)
        assertThat(rotationsOf("l4")).isEqualTo(4)
        assertThat(rotationsOf("t4")).isEqualTo(4)
        assertThat(rotationsOf("s4")).isEqualTo(2)
        assertThat(rotationsOf("c5")).isEqualTo(4)
        assertThat(rotationsOf("r6")).isEqualTo(2)
    }

    @Test
    fun noTwoEntriesHaveTheSameCells() {
        val cellSets = catalog.entries.map { it.shape.cells }

        assertThat(cellSets.toSet()).hasSize(cellSets.size)
    }

    @Test
    fun everyShapeFitsInFiveByFive() {
        catalog.entries.forEach {
            assertThat(it.shape.width).isAtMost(5)
            assertThat(it.shape.height).isAtMost(5)
        }
    }

    @Test
    fun lookupByStableId() {
        assertThat(catalog["sq2_0"]?.size).isEqualTo(4)
        assertThat(catalog["i3_1"]?.height).isEqualTo(3)
        assertThat(catalog["missing"]).isNull()
    }
}
