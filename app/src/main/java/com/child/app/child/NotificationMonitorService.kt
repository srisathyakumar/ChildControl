package com.child.app.child

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.google.firebase.firestore.FirebaseFirestore

class NotificationMonitorService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("notifications")
            .add(
                mapOf(
                    "package" to pkg,
                    "timestamp" to System.currentTimeMillis()
                )
            )
    }
}
