package com.child.app.child

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            FirebaseFirestore.getInstance()
                .collection("unlocks")
                .add(
                    mapOf(
                        "timestamp" to System.currentTimeMillis()
                    )
                )
        }
    }
}
