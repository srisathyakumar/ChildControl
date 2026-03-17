package com.child.app.child

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R

class BlockedAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)
    }

    override fun onBackPressed() {
        // prevent exit
    }
}
