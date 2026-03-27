package com.child.app.child

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.child.app.R
import com.google.firebase.firestore.FirebaseFirestore

class LimitManager(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun checkLimits(usageMap: Map<String, Long>) {
        val childUid = prefs.getString("childUid", null) ?: return

        firestore.collection("limits")
            .document(childUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val dailyLimit = snapshot.getLong("dailyLimitMinutes") ?: 0
                val appLimits = snapshot.get("appLimits") as? Map<*, *> ?: emptyMap<Any, Any>()

                var totalMillis = 0L

                for ((pkg, time) in usageMap) {
                    totalMillis += time
                    val minutes = time / 60000

                    val appLimit = (appLimits[pkg] as? Number)?.toLong()
                    if (appLimit != null && appLimit > 0 && minutes >= appLimit) {
                        showNotification("App limit reached: $pkg", pkg.hashCode())
                    }
                }

                val totalMinutes = totalMillis / 60000
                if (dailyLimit in 1..totalMinutes) {
                    showNotification("Daily screen time limit reached", 1001)
                }
            }
    }

    private fun showNotification(message: String, notificationId: Int) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "limit_channel",
                "Limit Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat.Builder(context, "limit_channel")
                .setContentTitle("Parental Control")
                .setContentText(message)
                .setSmallIcon(R.drawable.logo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

        manager.notify(notificationId, notification)
    }
}
