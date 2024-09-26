package com.example.opsc_poe_task_app

data class TimeSheetEntry(
    var entryId: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val description: String = "",
    val photoUrl: String? = null,
    var categoryName: String = ""
) {
    //Function to calculate the duration in hours
    fun getDurationInHours(): Double {
        val startParts = startTime.split(":").map { it.toIntOrNull() ?: 0 }
        val endParts = endTime.split(":").map { it.toIntOrNull() ?: 0 }

        val startMinutes = startParts[0] * 60 + startParts[1]
        val endMinutes = endParts[0] * 60 + endParts[1]

        var durationMinutes = endMinutes - startMinutes
        if (durationMinutes < 0) {
            durationMinutes += 24 * 60
        }

        return durationMinutes / 60.0
    }
}
