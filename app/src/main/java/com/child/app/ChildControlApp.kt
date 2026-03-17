package com.child.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class ChildControlApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val mode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
