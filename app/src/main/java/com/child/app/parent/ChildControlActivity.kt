package com.child.app.parent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R

class ChildControlActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_control)

        val childUid = intent.getStringExtra("childUid")!!

        findViewById<Button>(R.id.btnUsage).setOnClickListener {
            val intent = Intent(this, ParentUsageActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnGeoFence).setOnClickListener {
            val intent = Intent(this, SetGeoFenceActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnMap).setOnClickListener {
            val intent = Intent(this, ParentMapActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnAlerts).setOnClickListener {
            val intent = Intent(this, AlertActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }
    }
}