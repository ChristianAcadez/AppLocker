package com.christianacadez.applocker.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.christianacadez.applocker.service.MonitorAccessibilityService

object AccessibilityStatus {

    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val mine = ComponentName(context, MonitorAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }

    fun openSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}