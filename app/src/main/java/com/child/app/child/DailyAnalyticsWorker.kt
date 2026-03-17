package com.child.app.child

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.child.app.child.analysis.BehaviourAnalyzer
import com.child.app.child.analysis.GamingAnalyzer
import com.child.app.child.analysis.SleepAnalyzer
import com.child.app.child.analysis.SocialMediaAnalyzer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyAnalyticsWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val childId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - (24 * 60 * 60 * 1000)

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        )

        var totalMinutes = 0L
        val appUsage = mutableMapOf<String, Long>()
        var nightUsageCount = 0

        for (s in stats) {
            val minutes = s.totalTimeInForeground / 60000
            if (minutes > 0) {
                totalMinutes += minutes
                appUsage[s.packageName] = minutes
                
                // Approximate night usage from last time used
                if (SleepAnalyzer.isNightUsage(s.lastTimeUsed)) {
                    nightUsageCount++
                }
            }
        }

        val gamingMinutes = GamingAnalyzer.getGamingMinutes(appUsage)
        val socialMinutes = SocialMediaAnalyzer.getSocialMinutes(appUsage)

        val gamingRisk = GamingAnalyzer.detectRisk(gamingMinutes)
        val socialRisk = SocialMediaAnalyzer.detectRisk(socialMinutes, totalMinutes.coerceAtLeast(1L))
        val sleepRisk = SleepAnalyzer.getSleepRisk(nightUsageCount)

        val behaviourScore = BehaviourAnalyzer.calculateScore(
            gamingRisk,
            socialRisk,
            sleepRisk
        )

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("childAnalytics")
            .document(childId)
            .collection("daily")
            .document(date)
            .set(
                mapOf(
                    "totalScreenMinutes" to totalMinutes,
                    "gamingMinutes" to gamingMinutes,
                    "socialMinutes" to socialMinutes,
                    "sleepRisk" to sleepRisk,
                    "gamingRisk" to gamingRisk,
                    "socialRisk" to socialRisk,
                    "behaviourScore" to behaviourScore,
                    "timestamp" to System.currentTimeMillis()
                )
            )

        return Result.success()
    }
}
