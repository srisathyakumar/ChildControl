package com.child.app.child

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class UsageEventsCollector(private val context: Context) {
    fun getAppOpenCount(): Map<String, Int> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - (24 * 60 * 60 * 1000)

        val events = usm.queryEvents(start, end)
        val map = mutableMapOf<String, Int>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val pkg = event.packageName
                map[pkg] = (map[pkg] ?: 0) + 1
            }
        }
        return map
    }
}
