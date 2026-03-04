package com.child.app.child

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore

class AppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val prefs =
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val childUid = prefs.getString("childUid", null) ?: return
        val firestore = FirebaseFirestore.getInstance()

        val packageName = intent.data?.schemeSpecificPart ?: return

        val type = when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> "installed"
            Intent.ACTION_PACKAGE_REMOVED -> "uninstalled"
            else -> return
        }

        firestore.collection("events")
            .document(childUid)
            .collection("app_changes")
            .add(
                mapOf(
                    "package" to packageName,
                    "type" to type,
                    "timestamp" to System.currentTimeMillis()
                )
            )
    }
}