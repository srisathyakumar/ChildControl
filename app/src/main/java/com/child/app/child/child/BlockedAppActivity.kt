package com.child.app.child

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R

class BlockedAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_blocked)

        val txt = findViewById<TextView>(R.id.txtBlocked)

        val appName =
            intent.getStringExtra("packageName") ?: "App"

        txt.text = "$appName blocked.\nTime limit exceeded."
    }

    override fun onBackPressed() {
        // Disable back
    }
}