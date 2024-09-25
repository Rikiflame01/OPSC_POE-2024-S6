package com.example.opsc_poe_task_app

data class Quest(
    val name: String = "",
    val description: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val difficulty: Int = 0,
    val minGoal: Int = 0,
    val maxGoal: Int = 0,
    val daysOfWeek: List<String> = listOf(),
    val color: String = "None"
)
