package com.child.app.child.analysis

import java.util.*

object SleepAnalyzer {

    fun analyzeSleep(events: List<Long>): Boolean {

        for (time in events) {

            val cal = Calendar.getInstance()

            cal.timeInMillis = time

            val hour = cal.get(Calendar.HOUR_OF_DAY)

            if (hour >= 22 || hour <= 6) {

                return true
            }
        }

        return false
    }
    fun isNightUsage(timestamp: Long): Boolean {

        val cal = Calendar.getInstance()

        cal.timeInMillis = timestamp

        val hour = cal.get(Calendar.HOUR_OF_DAY)

        return hour >= 22 || hour <= 6
    }

    fun getSleepRisk(nightUsageCount: Int): String {

        return when {

            nightUsageCount > 3 -> "High Risk"

            nightUsageCount > 1 -> "Moderate"

            else -> "Healthy"
        }
    }
}
