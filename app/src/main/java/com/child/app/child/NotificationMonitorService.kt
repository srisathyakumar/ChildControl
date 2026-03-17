package com.child.app.child

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.google.firebase.firestore.FirebaseFirestore

class NotificationMonitorService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        val isInappropriate = ContentFilter.isBlocked(title) || ContentFilter.isBlocked(text)
        
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("notifications")
            .add(
                mapOf(
                    "package" to pkg,
                    "title" to title,
                    "text" to text,
                    "isInappropriate" to isInappropriate,
                    "timestamp" to System.currentTimeMillis()
                )
            )
    }
}
