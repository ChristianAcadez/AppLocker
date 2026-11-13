package com.christianacadez.applocker.data

import android.content.Context

data class SafetyValues(
    val waitSeconds: Int,
    val extraMinutes: Int,
    val maxUnlocksPerDay: Int
)

class SafetySettings(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("safety_settings", Context.MODE_PRIVATE)

    fun load() = SafetyValues(
        waitSeconds = prefs.getInt(KEY_WAIT, DEFAULT.waitSeconds).coerceIn(WAIT_RANGE),
        extraMinutes = prefs.getInt(KEY_EXTRA, DEFAULT.extraMinutes).coerceIn(EXTRA_RANGE),
        maxUnlocksPerDay = prefs.getInt(KEY_MAX, DEFAULT.maxUnlocksPerDay).coerceIn(MAX_RANGE)
    )

    fun save(values: SafetyValues) {
        prefs.edit()
            .putInt(KEY_WAIT, values.waitSeconds)
            .putInt(KEY_EXTRA, values.extraMinutes)
            .putInt(KEY_MAX, values.maxUnlocksPerDay)
            .apply()
    }

    companion object {
        val WAIT_RANGE = 10..600
        val EXTRA_RANGE = 1..60
        val MAX_RANGE = 0..10
        val DEFAULT = SafetyValues(waitSeconds = 60, extraMinutes = 5, maxUnlocksPerDay = 2)

        private const val KEY_WAIT = "wait_seconds"
        private const val KEY_EXTRA = "extra_minutes"
        private const val KEY_MAX = "max_unlocks"
    }
}