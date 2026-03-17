package com.child.app.child.analysis

object SocialMediaAnalyzer {

    private val socialApps = listOf(

        "com.instagram.android",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.whatsapp"
    )

    fun getSocialMinutes(appUsage: Map<String, Long>): Long {

        var total = 0L

        for ((pkg, minutes) in appUsage) {

            if (socialApps.contains(pkg)) {

                total += minutes
            }
        }

        return total
    }

    fun detectRisk(
        socialMinutes: Long,
        totalMinutes: Long
    ): String {

        val percent = (socialMinutes * 100) / totalMinutes

        return when {

            percent > 60 -> "High Risk"

            percent > 40 -> "Moderate"

            else -> "Healthy"
        }
    }
}