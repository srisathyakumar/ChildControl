package com.child.app.child

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R

class BedTimeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_locked)
    }

    override fun onBackPressed() {
        // disable back
    }
}
