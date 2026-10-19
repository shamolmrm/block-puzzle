package com.puzzleblockblast.game.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val PACKAGE = "com.puzzleblockblast.game"
private const val WAIT_MS = 5_000L

/** Cold start to the Home screen. */
fun MacrobenchmarkScope.startToHome() {
    pressHome()
    startActivityAndWait()
    device.wait(Until.hasObject(By.text("Classic")), WAIT_MS)
}

/**
 * The paths most players take right after launch: open Classic (through the first-run tutorial,
 * skipped), drag a piece, back out, then open the level list and the Stats screen.
 */
fun MacrobenchmarkScope.criticalJourney() {
    startToHome()
    device.findObject(By.text("Classic"))?.click()
    device.wait(Until.hasObject(By.text("Skip")), WAIT_MS)
    device.findObject(By.text("Skip"))?.click()
    device.wait(Until.hasObject(By.desc("Pause")), WAIT_MS)
    // Drag the first tray piece onto the board's centre.
    val width = device.displayWidth
    val height = device.displayHeight
    device.swipe(width / 6, (height * 0.78).toInt(), width / 2, height / 2, 40)
    device.waitForIdle()
    device.pressBack()
    device.wait(Until.hasObject(By.text("Levels")), WAIT_MS)
    device.findObject(By.text("Levels"))?.click()
    device.wait(Until.hasObject(By.text("1")), WAIT_MS)
    device.pressBack()
    device.wait(Until.hasObject(By.text("Stats")), WAIT_MS)
    device.findObject(By.text("Stats"))?.click()
    device.waitForIdle()
    device.pressBack()
}
