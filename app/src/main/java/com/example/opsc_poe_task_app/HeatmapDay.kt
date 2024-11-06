package com.example.opsc_poe_task_app.com.example.opsc_poe_task_app

import java.util.Date

data class HeatmapDay(
    val date: Date,
    var status: DayStatus
)

enum class DayStatus {
    BELOW_GOAL,
    WITHIN_GOAL,
    ABOVE_GOAL,
    NO_DATA,
    HOURS_WORKED_NO_GOAL
}

