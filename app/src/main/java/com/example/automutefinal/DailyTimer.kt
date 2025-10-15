
package com.example.automutefinal

import java.util.UUID

data class DailyTimer(
    val id: String = UUID.randomUUID().toString(),
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: List<Int>
)