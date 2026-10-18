package com.puzzleblockblast.game.core.testing

import com.puzzleblockblast.game.core.analytics.AnalyticsEvent
import com.puzzleblockblast.game.core.analytics.AnalyticsTracker

class FakeAnalyticsTracker : AnalyticsTracker {
    private val _events = mutableListOf<AnalyticsEvent>()
    val events: List<AnalyticsEvent> get() = _events

    override fun log(event: AnalyticsEvent) {
        _events += event
    }
}
