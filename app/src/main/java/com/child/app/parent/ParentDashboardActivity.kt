package com.child.app.parent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R

class ParentDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        val btnPair = findViewById<Button>(R.id.btnPairDevice)

        btnPair.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    GeneratePairCodeActivity::class.java
                )
            )
        }

        val btnViewChildren =
            findViewById<Button>(R.id.btnViewChildren)

        btnViewChildren.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PairedChildrenActivity::class.java
                )
            )
        }
    }
}