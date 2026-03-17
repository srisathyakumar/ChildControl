package com.child.app.child

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

object AppBehaviourAnalyzer {

    fun analyzeSleep(events: List<Long>): Boolean {
        for (time in events) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = time
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour >= 22 || hour <= 6) {
                return true
            }
        }
        return false
    }

    fun checkInstallAbuse() {
        val childId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("events")
            .document(childId)
            .collection("app_changes")
            .get()
            .addOnSuccessListener { snapshot ->
                val appCounter = mutableMapOf<String, Int>()
                snapshot.forEach {
                    val pkg = it.getString("package") ?: return@forEach
                    val count = appCounter[pkg] ?: 0
                    appCounter[pkg] = count + 1
                }

                appCounter.forEach { (pkg, count) ->
                    if (count >= 8) {
                        firestore.collection("alerts")
                            .add(
                                mapOf(
                                    "type" to "frequent_install_uninstall",
                                    "package" to pkg,
                                    "childId" to childId,
                                    "count" to count,
                                    "timestamp" to System.currentTimeMillis()
                                )
                            )
                    }
                }
            }
    }
}
