package com.example.opsc_poe_task_app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class TimeSheetAdapter(
    private val context: Context,
    private val timeSheetEntries: List<TimeSheetEntry>
) : RecyclerView.Adapter<TimeSheetAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timesheetImage: ImageView = itemView.findViewById(R.id.timesheetImage)
        val timesheetDate: TextView = itemView.findViewById(R.id.timesheetDate)
        val timesheetCategory: TextView = itemView.findViewById(R.id.timesheetCategory)
        val timesheetHours: TextView = itemView.findViewById(R.id.timesheetHours)
        val timesheetDescription: TextView = itemView.findViewById(R.id.timesheetDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timesheet_entry, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return timeSheetEntries.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = timeSheetEntries[position]

        holder.timesheetDate.text = "Date: ${entry.date}"
        holder.timesheetCategory.text = "Category: ${entry.categoryName}"
        holder.timesheetHours.text = "Hours: ${String.format("%.2f", entry.getDurationInHours())}"
        holder.timesheetDescription.text = "Description: ${entry.description}"

        if (!entry.photoUrl.isNullOrEmpty()) {
            holder.timesheetImage.visibility = View.VISIBLE
            Glide.with(context)
                .load(entry.photoUrl)
                .into(holder.timesheetImage)
        } else {
            holder.timesheetImage.visibility = View.GONE
        }
    }
}
