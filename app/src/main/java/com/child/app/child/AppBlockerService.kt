package com.child.app.child

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AppBlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        checkIfBlocked(packageName)
    }

    override fun onInterrupt() {}

    private fun checkIfBlocked(pkg: String) {

        val blockedApps = listOf(
            "com.instagram.android",
            "com.facebook.katana"
        )

        if (blockedApps.contains(pkg)) {

            lockApp(pkg)
        }
    }

    private fun lockApp(packageName: String) {

        val intent = Intent(
            this,
            BlockedAppActivity::class.java
        )

        intent.putExtra("packageName", packageName)

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)
    }

    fun getUsageMinutes(pkg: String): Long {

        val usm =
            getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        val end = System.currentTimeMillis()

        val start = end - (24 * 60 * 60 * 1000)

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        )

        val app = stats.firstOrNull {
            it.packageName == pkg
        }

        return (app?.totalTimeInForeground ?: 0L) / 60000
    }
}