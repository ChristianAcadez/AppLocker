package com.christianacadez.applocker.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

data class UsageSnapshot(
    val totals: Map<String, Long>,   // milisegundos de uso por paquete
    val foreground: Set<String>      // paquetes abiertos ahora mismo
)

class UsageReader(context: Context) {

    private val usm =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun usageSince(start: Long, packages: Set<String>): Map<String, Long> =
        snapshot(start, packages).totals

    @Suppress("DEPRECATION")
    fun snapshot(start: Long, packages: Set<String>): UsageSnapshot {
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()
        val lastResume = mutableMapOf<String, Long>()
        val totals = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            if (pkg !in packages) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    lastResume[pkg] = event.timeStamp

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val from = lastResume.remove(pkg) ?: start
                    totals[pkg] = (totals[pkg] ?: 0L) + (event.timeStamp - from)
                }
            }
        }
        // Apps que siguen abiertas ahora mismo
        lastResume.forEach { (pkg, from) ->
            totals[pkg] = (totals[pkg] ?: 0L) + (end - from)
        }
        return UsageSnapshot(totals, lastResume.keys.toSet())
    }

    companion object {
        fun startOfToday(): Long =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}