package com.child.app.child

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import java.util.*

class UsageStatsActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_stats)

        listView = findViewById(R.id.listUsage)

        if (!hasPermission()) {
            startActivity(
                Intent(this, UsagePermissionActivity::class.java)
            )
            return
        }

        loadUsageStats()
    }

    private fun hasPermission(): Boolean {
        val appOps =
            getSystemService(Context.APP_OPS_SERVICE)
                    as android.app.AppOpsManager

        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )

        return mode ==
                android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun loadUsageStats() {

        val usageStatsManager =
            getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - 1000 * 60 * 60 * 24

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        )

        val usageList = ArrayList<String>()

        val pm = packageManager

        for (usage in stats) {

            if (usage.totalTimeInForeground > 0) {

                try {

                    val appInfo =
                        pm.getApplicationInfo(
                            usage.packageName,
                            0
                        )

                    val appName =
                        pm.getApplicationLabel(appInfo)

                    usageList.add(
                        "$appName : ${
                            usage.totalTimeInForeground / 1000
                        } sec"
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            usageList
        )

        listView.adapter = adapter
    }
}
