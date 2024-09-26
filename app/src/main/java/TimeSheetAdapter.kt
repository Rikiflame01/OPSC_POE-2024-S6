package com.example.opsc_poe_task_app

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase

class TimeSheetAdapter(
    private val context: Context,
    private val timeSheetEntries: MutableList<TimeSheetEntry>
) : RecyclerView.Adapter<TimeSheetAdapter.ViewHolder>() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timesheetImage: ImageView = itemView.findViewById(R.id.timesheetImage)
        val timesheetDate: TextView = itemView.findViewById(R.id.timesheetDate)
        val timesheetCategory: TextView = itemView.findViewById(R.id.timesheetCategory)
        val timesheetHours: TextView = itemView.findViewById(R.id.timesheetHours)
        val timesheetDescription: TextView = itemView.findViewById(R.id.timesheetDescription)
        val deleteEntryButton: Button = itemView.findViewById(R.id.deleteEntryButton)
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

        holder.deleteEntryButton.setOnClickListener {
            //Show confirmation dialog
            AlertDialog.Builder(context)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this timesheet entry?")
                .setPositiveButton("Yes") { dialog, _ ->
                    deleteTimeSheetEntry(entry)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun deleteTimeSheetEntry(entry: TimeSheetEntry) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val categoryName = entry.categoryName.trim()
            val entryId = entry.entryId

            if (entryId.isNotEmpty()) {
                val entryReference = FirebaseDatabase.getInstance()
                    .getReference("users/$uid/categories/$categoryName/TimesheetEntries/$entryId")

                //Delete the entry
                entryReference.removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                        //data reload in the adapter via onDataChange
                    } else {
                        Toast.makeText(context, "Failed to delete entry", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Invalid entry ID", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

}
