package com.puzzleblockblast.game.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test + timing of the optimized (R8) build on devices that block `cmd package compile`
 * (some OEM ROMs, e.g. ColorOS): CompilationMode.Ignore leaves the app as installed.
 * Runs the full critical journey, so an R8 keep-rule problem crashes this test.
 * `gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=…ReleaseSmokeBenchmark`
 */
@OptIn(ExperimentalMacrobenchmarkApi::class) // CompilationMode.Ignore
@RunWith(AndroidJUnit4::class)
class ReleaseSmokeBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartAsInstalled() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Ignore(),
        iterations = 6,
        startupMode = StartupMode.COLD,
    ) {
        startToHome()
    }

    @Test
    fun criticalJourneyRunsOnTheOptimizedBuild() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Ignore(),
        iterations = 2,
        startupMode = StartupMode.COLD,
    ) {
        criticalJourney()
    }
}
