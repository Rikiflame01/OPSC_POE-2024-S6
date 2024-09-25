package com.example.opsc_poe_task_app

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuestAdapter(private val questList: List<Quest>) : RecyclerView.Adapter<QuestAdapter.QuestViewHolder>() {

    class QuestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val questNameTextView: TextView = itemView.findViewById(R.id.questName)
        val questDateTextView: TextView = itemView.findViewById(R.id.questDate)
        val questTimeTextView: TextView = itemView.findViewById(R.id.questTime)
        val questColorView: View = itemView.findViewById(R.id.questColor)
        val viewDetailsButton: Button = itemView.findViewById(R.id.viewDetailsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quest, parent, false)
        return QuestViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestViewHolder, position: Int) {
        val quest = questList[position]

        holder.questNameTextView.text = quest.name
        holder.questDateTextView.text = quest.date
        holder.questTimeTextView.text = "${quest.startTime} - ${quest.endTime}"

        try {
            holder.questColorView.setBackgroundColor(android.graphics.Color.parseColor(quest.color))
        } catch (e: IllegalArgumentException) {
            holder.questColorView.setBackgroundColor(android.graphics.Color.GRAY) // Set to default if invalid color
        }

        holder.viewDetailsButton.setOnClickListener {
            val message = """
                Description: ${quest.description}
                Difficulty: ${quest.difficulty} stars
                Min Daily Goal: ${quest.minGoal} hrs
                Max Daily Goal: ${quest.maxGoal} hrs
                Days of the Week: ${quest.daysOfWeek.joinToString(", ")}
            """.trimIndent()

            //Show a dialog with the quest details
            AlertDialog.Builder(holder.itemView.context)
                .setTitle(quest.name)
                .setMessage(message)
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun getItemCount(): Int = questList.size
}
