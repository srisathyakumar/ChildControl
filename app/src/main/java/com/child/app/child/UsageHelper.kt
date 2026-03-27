package com.child.app.child

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.*

object UsageHelper {

    /**
     * Data class to hold all usage info for a single day to avoid multiple USM queries.
     */
    data class DayUsageData(
        val totalMs: Long,
        val appUsageMap: Map<String, Long>,
        val hourlyUsage: FloatArray, // 24 buckets of minutes
        val categoryUsage: Map<String, Long>
    )

    /**
     * Optimized method to get ALL usage data for today in one pass.
     */
    fun getTodayUsageData(context: Context): DayUsageData {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        val usageMap = mutableMapOf<String, Long>()
        val hourlyUsage = FloatArray(24)
        
        // We query from start of day to now
        val events = usm.queryEvents(startOfDay, now)
        val event = UsageEvents.Event()
        
        val activeActivities = mutableMapOf<String, MutableSet<String>>()
        val pkgStartTime = mutableMapOf<String, Long>()
        var isScreenOn = true 

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            val clazz = event.className
            val time = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val set = activeActivities.getOrPut(pkg) { mutableSetOf() }
                    if (set.isEmpty() && isScreenOn) {
                        pkgStartTime[pkg] = time
                    }
                    set.add(clazz)
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    activeActivities[pkg]?.let { set ->
                        set.remove(clazz)
                        if (set.isEmpty()) {
                            pkgStartTime.remove(pkg)?.let { start ->
                                distributeTimeToBuckets(usageMap, hourlyUsage, pkg, start, time, startOfDay)
                            }
                        }
                    }
                }
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    isScreenOn = true
                    for ((p, activities) in activeActivities) {
                        if (activities.isNotEmpty()) pkgStartTime[p] = time
                    }
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    isScreenOn = false
                    for ((p, start) in pkgStartTime) {
                        distributeTimeToBuckets(usageMap, hourlyUsage, p, start, time, startOfDay)
                    }
                    pkgStartTime.clear()
                }
            }
        }

        // Close active sessions
        if (isScreenOn) {
            for ((p, start) in pkgStartTime) {
                distributeTimeToBuckets(usageMap, hourlyUsage, p, start, now, startOfDay)
            }
        }

        // Filter out launchers
        val launcherPkg = getLauncherPackageName(context)
        val filteredAppUsage = usageMap.filter { (pkg, time) ->
            pkg != launcherPkg && !pkg.contains("launcher", ignoreCase = true) && time > 0
        }

        // Categorize
        val categoryMap = categorizeApps(context, filteredAppUsage)

        return DayUsageData(
            totalMs = filteredAppUsage.values.sum(),
            appUsageMap = filteredAppUsage,
            hourlyUsage = hourlyUsage,
            categoryUsage = categoryMap
        )
    }

    private fun distributeTimeToBuckets(
        usageMap: MutableMap<String, Long>,
        hourlyUsage: FloatArray,
        pkg: String,
        start: Long,
        end: Long,
        startOfDay: Long
    ) {
        if (end <= start) return
        
        // Update total app usage
        usageMap[pkg] = (usageMap[pkg] ?: 0L) + (end - start)
        
        // Update hourly buckets
        var current = start
        while (current < end) {
            val currentHour = ((current - startOfDay) / 3600000L).toInt()
            if (currentHour < 0 || currentHour >= 24) break
            
            val nextHourStart = startOfDay + (currentHour + 1) * 3600000L
            val segmentEnd = minOf(end, nextHourStart)
            
            val durationMinutes = (segmentEnd - current) / 60000f
            hourlyUsage[currentHour] += durationMinutes
            
            current = segmentEnd
        }
    }

    private fun categorizeApps(context: Context, usageMap: Map<String, Long>): Map<String, Long> {
        val pm = context.packageManager
        val categoryMap = mutableMapOf<String, Long>()
        for ((pkg, time) in usageMap) {
            val category = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                when (appInfo.category) {
                    ApplicationInfo.CATEGORY_GAME -> "Games"
                    ApplicationInfo.CATEGORY_SOCIAL -> "Social Media"
                    ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO -> "Entertainment"
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                    else -> "Others"
                }
            } catch (e: Exception) { "Others" }
            categoryMap[category] = (categoryMap[category] ?: 0L) + time
        }
        return categoryMap
    }

    fun getFilteredUsage(context: Context, startTime: Long, endTime: Long): Map<String, Long> {
        // Fallback for older code if needed, but we should use getTodayUsageData
        return emptyMap() 
    }

    fun getLauncherPackageName(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName
    }

    // Deprecated helpers to be replaced by getTodayUsageData
    fun getHourlyUsage(context: Context): FloatArray = FloatArray(24)
    fun getCategoryUsage(context: Context, s: Long, e: Long): Map<String, Long> = emptyMap()
}
