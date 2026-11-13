package com.christianacadez.applocker.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

data class UsageSnapshot(
    val totals: Map<String, Long>,   // milisegundos de uso por paquete desde countFrom
    val foreground: Set<String>      // paquetes abiertos ahora mismo
)

class UsageReader(context: Context) {

    private val usm =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun usageSince(start: Long, packages: Set<String>): Map<String, Long> =
        snapshot(start, packages).totals

    /** Cuenta solo el tiempo posterior a [countFrom], pero mira atrás para detectar apps ya abiertas. */
    @Suppress("DEPRECATION")
    fun snapshot(countFrom: Long, packages: Set<String>): UsageSnapshot {
        val end = System.currentTimeMillis()
        val queryStart = countFrom - LOOKBACK_MS
        val events = usm.queryEvents(queryStart, end)
        val event = UsageEvents.Event()
        val lastResume = mutableMapOf<String, Long>()
        val totals = mutableMapOf<String, Long>()

        fun add(pkg: String, from: Long, to: Long) {
            val start = maxOf(from, countFrom)
            if (to > start) totals[pkg] = (totals[pkg] ?: 0L) + (to - start)
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            if (pkg !in packages) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    lastResume[pkg] = event.timeStamp

                UsageEvents.Event.MOVE_TO_BACKGROUND ->
                    add(pkg, lastResume.remove(pkg) ?: queryStart, event.timeStamp)
            }
        }
        lastResume.forEach { (pkg, from) -> add(pkg, from, end) }
        return UsageSnapshot(totals, lastResume.keys.toSet())
    }

    companion object {
        private const val LOOKBACK_MS = 12 * 60 * 60 * 1000L

        fun startOfToday(): Long =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}