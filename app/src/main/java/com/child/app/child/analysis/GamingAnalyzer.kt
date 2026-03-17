package com.child.app.child.analysis

object GamingAnalyzer {

    private val gamingApps = listOf(
        "com.tencent.ig",
        "com.dts.freefireth",
        "com.activision.callofduty.shooter"
    )

    fun getGamingMinutes(appUsage: Map<String, Long>): Long {

        var total = 0L

        for ((pkg, minutes) in appUsage) {

            if (gamingApps.contains(pkg)) {

                total += minutes
            }
        }

        return total
    }

    fun detectRisk(minutes: Long): String {

        return when {

            minutes > 240 -> "High Risk"

            minutes > 120 -> "Moderate"

            else -> "Healthy"
        }
    }
}