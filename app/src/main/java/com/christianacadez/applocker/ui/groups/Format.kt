package com.christianacadez.applocker.ui.groups

import com.christianacadez.applocker.data.db.GroupRule

fun formatMinuteOfDay(minute: Int): String =
    "%02d:%02d".format((minute / 60) % 24, minute % 60)

fun formatLimit(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

fun describeRange(start: Int, end: Int): String {
    val normalizedEnd = end % 1440
    return when {
        start == normalizedEnd && start == 0 -> "Todo el día"
        start == normalizedEnd -> "24 h desde ${formatMinuteOfDay(start)}"
        else -> "${formatMinuteOfDay(start)}–${formatMinuteOfDay(normalizedEnd)}"
    }
}

fun describeRule(rule: GroupRule): String =
    "${describeRange(rule.startMinute, rule.endMinute)} · ${formatLimit(rule.limitMinutes)} por app"