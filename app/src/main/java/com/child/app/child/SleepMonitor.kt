package com.child.app.child

import java.util.Calendar

object SleepMonitor {

    fun isBedTime(): Boolean {
        val hour = Calendar.getInstance()
            .get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour <= 6
    }

    fun isSleepDisturbance(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour <= 6
    }
}
