package com.child.app.child

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.child.app.child.ChildUsageUploader

class ChildDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_dashboard)

        val btnPair = findViewById<Button>(R.id.btnEnterCode)

        btnPair.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    EnterPairCodeActivity::class.java
                )
            )
        }

        val btnUsage = findViewById<Button>(R.id.btnUsage)

        btnUsage.setOnClickListener {
            startActivity(
                Intent(this, UsageStatsActivity::class.java)
            )
        }

        val uploader = ChildUsageUploader(this)
        uploader.uploadTodayUsage()

        startActivity(
            Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
        )


    }
}
