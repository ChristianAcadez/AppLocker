package com.christianacadez.applocker.domain

import java.time.Instant
import java.time.ZoneId

/** Franja con su límite. Minutos desde las 00:00; endMinute puede ser 1440 (24:00). */
data class Rule(val startMinute: Int, val endMinute: Int, val limitMinutes: Int)

/** Una ocurrencia concreta de una franja, con fechas y horas reales. */
data class Window(val startMs: Long, val endMs: Long, val limitMinutes: Int)

object LimitEngine {

    /** La franja en curso en [nowMs], o null si ahora no hay ninguna. */
    fun activeWindow(rules: List<Rule>, nowMs: Long, zone: ZoneId): Window? {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        for (rule in rules) {
            // Se prueba la franja que empezó hoy y la de ayer (por si cruza la medianoche)
            for (offset in 0L downTo -1L) {
                val dayStart = today.plusDays(offset).atStartOfDay(zone)
                val endDay = if (rule.endMinute > rule.startMinute) dayStart else dayStart.plusDays(1)
                val startMs = dayStart.plusMinutes(rule.startMinute.toLong()).toInstant().toEpochMilli()
                val endMs = endDay.plusMinutes(rule.endMinute.toLong()).toInstant().toEpochMilli()
                if (nowMs in startMs until endMs) {
                    return Window(startMs, endMs, rule.limitMinutes)
                }
            }
        }
        return null
    }

    /** Milisegundos que quedan en la franja (<= 0 significa que hay que bloquear). */
    fun remainingMs(window: Window, usedMs: Long, extraMinutes: Int): Long =
        (window.limitMinutes + extraMinutes) * 60_000L - usedMs

    /** Tramos [inicio, fin) dentro de las 24 h en que la franja está activa. */
    private fun segments(rule: Rule): List<Pair<Int, Int>> =
        if (rule.endMinute > rule.startMinute) {
            listOf(rule.startMinute to rule.endMinute)
        } else {
            buildList {
                add(rule.startMinute to 1440)
                if (rule.endMinute > 0) add(0 to rule.endMinute)
            }
        }

    /** True si las dos franjas coinciden en algún momento del día. */
    fun overlaps(a: Rule, b: Rule): Boolean =
        segments(a).any { x -> segments(b).any { y -> x.first < y.second && y.first < x.second } }
}