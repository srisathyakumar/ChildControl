package com.child.app.child

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppBlockerService : AccessibilityService() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event?.packageName == null) return

        val currentPackage =
            event.packageName.toString()

        val childId =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: return

        firestore.collection("screenLimits")
            .whereEqualTo("childId", childId)
            .whereEqualTo("packageName", currentPackage)
            .get()
            .addOnSuccessListener { result ->

                for (doc in result) {

                    val limit =
                        doc.getLong("limitMinutes")
                            ?: continue

                    val used =
                        getUsageMinutes(currentPackage)

                    if (used > limit) {
                        blockApp(currentPackage)
                    }
                }
            }
    }

    override fun onInterrupt() {}

    private fun blockApp(packageName: String) {

        val intent = Intent(
            this,
            BlockedAppActivity::class.java
        )

        intent.putExtra("packageName", packageName)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
        )

        startActivity(intent)
    }

    private fun getUsageMinutes(pkg: String): Long {

        val usage =
            FirebaseFirestore.getInstance()
                .collection("usage")
                .document(
                    FirebaseAuth.getInstance()
                        .currentUser?.uid ?: return 0
                )
                .collection("apps")
                .document(pkg)

        // NOTE:
        // For now return large number for test
        return 9999
    }
}