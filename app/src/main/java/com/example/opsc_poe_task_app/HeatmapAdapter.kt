package com.example.opsc_poe_task_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.opsc_poe_task_app.com.example.opsc_poe_task_app.DayStatus
import com.example.opsc_poe_task_app.com.example.opsc_poe_task_app.HeatmapDay
import java.text.SimpleDateFormat
import java.util.*

class HeatmapAdapter(private val days: List<HeatmapDay>) : RecyclerView.Adapter<HeatmapAdapter.HeatmapViewHolder>() {

    class HeatmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTextView: TextView = itemView.findViewById(R.id.dayTextView)
        val dayContainer: View = itemView.findViewById(R.id.dayContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeatmapViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_heatmap_day, parent, false)
        return HeatmapViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeatmapViewHolder, position: Int) {
        val day = days[position]
        val calendar = Calendar.getInstance()
        calendar.time = day.date
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        holder.dayTextView.text = dayOfMonth.toString()

        //Set background colour based on DayStatus
        val colorResId = when (day.status) {
            DayStatus.BELOW_GOAL -> R.color.colourBelowGoal
            DayStatus.WITHIN_GOAL -> R.color.colourWithinGoal
            DayStatus.ABOVE_GOAL -> R.color.colourAboveGoal
            DayStatus.NO_DATA -> R.color.colourNoData
            DayStatus.HOURS_WORKED_NO_GOAL -> R.color.colourHoursWorkedNoGoal
        }
        holder.dayContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, colorResId))

        holder.itemView.setOnClickListener {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val dateStr = dateFormat.format(day.date)
            val statusStr = when (day.status) {
                DayStatus.BELOW_GOAL -> "Below Goal"
                DayStatus.WITHIN_GOAL -> "Within Goal"
                DayStatus.ABOVE_GOAL -> "Above Goal"
                DayStatus.NO_DATA -> "No Data"
                DayStatus.HOURS_WORKED_NO_GOAL -> "Hours Worked, No Goal Set"
            }
            Toast.makeText(holder.itemView.context, "Date: $dateStr\nStatus: $statusStr", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = days.size
}
