package com.christianacadez.applocker.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

class UsageReader(context: Context) {

    private val usm =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /** Milisegundos de uso en primer plano por paquete, desde [start] hasta ahora. */
    @Suppress("DEPRECATION")
    fun usageSince(start: Long, packages: Set<String>): Map<String, Long> {
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
                    // Si no vimos el inicio, la app ya estaba abierta desde [start]
                    val from = lastResume.remove(pkg) ?: start
                    totals[pkg] = (totals[pkg] ?: 0L) + (event.timeStamp - from)
                }
            }
        }
        // Apps que siguen abiertas ahora mismo
        lastResume.forEach { (pkg, from) ->
            totals[pkg] = (totals[pkg] ?: 0L) + (end - from)
        }
        return totals
    }

    companion object {
        fun startOfToday(): Long =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}