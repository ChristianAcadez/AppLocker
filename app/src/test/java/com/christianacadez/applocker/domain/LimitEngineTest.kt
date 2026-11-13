package com.christianacadez.applocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class LimitEngineTest {

    private val zone = ZoneId.of("America/Mexico_City")
    private val rules = listOf(
        Rule(startMinute = 6 * 60, endMinute = 11 * 60, limitMinutes = 15),   // 06:00–11:00
        Rule(startMinute = 22 * 60, endMinute = 60, limitMinutes = 30)        // 22:00–01:00
    )

    private fun ms(hour: Int, minute: Int = 0, dayOffset: Long = 0): Long =
        LocalDate.of(2026, 11, 11).plusDays(dayOffset)
            .atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun afternoon_has_no_window() {
        assertNull(LimitEngine.activeWindow(rules, ms(15), zone))
    }

    @Test
    fun morning_window() {
        val w = LimitEngine.activeWindow(rules, ms(7, 30), zone)!!
        assertEquals(ms(6), w.startMs)
        assertEquals(ms(11), w.endMs)
        assertEquals(15, w.limitMinutes)
    }

    @Test
    fun night_window_before_midnight() {
        val w = LimitEngine.activeWindow(rules, ms(23), zone)!!
        assertEquals(ms(22), w.startMs)
        assertEquals(ms(1, dayOffset = 1), w.endMs)
        assertEquals(30, w.limitMinutes)
    }

    @Test
    fun night_window_after_midnight_belongs_to_previous_evening() {
        val w = LimitEngine.activeWindow(rules, ms(0, 30), zone)!!
        assertEquals(ms(22, dayOffset = -1), w.startMs)
        assertEquals(ms(1), w.endMs)
    }

    @Test
    fun remaining_includes_extra_minutes() {
        val w = Window(startMs = 0, endMs = 1, limitMinutes = 15)
        assertEquals(5 * 60_000L, LimitEngine.remainingMs(w, usedMs = 15 * 60_000L, extraMinutes = 5))
    }

    @Test
    fun overlap_detection() {
        assertTrue(LimitEngine.overlaps(Rule(6 * 60, 11 * 60, 15), Rule(10 * 60, 12 * 60, 15)))
        // Se tocan en las 11:00 pero no se solapan
        assertFalse(LimitEngine.overlaps(Rule(6 * 60, 11 * 60, 15), Rule(11 * 60, 12 * 60, 15)))
        // 22:00–01:00 contra 00:00–00:30 (cruza la medianoche)
        assertTrue(LimitEngine.overlaps(Rule(22 * 60, 60, 30), Rule(0, 30, 10)))
        assertFalse(LimitEngine.overlaps(Rule(22 * 60, 60, 30), Rule(60, 6 * 60, 10)))
    }
}