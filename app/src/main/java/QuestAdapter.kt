package com.example.opsc_poe_task_app

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

data class QuestWithCategory(
    val quest: Quest,
    val categoryName: String,
    val questId: String
)

class QuestAdapter(
    private val questList: MutableList<QuestWithCategory>
) : RecyclerView.Adapter<QuestAdapter.QuestViewHolder>() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

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
        val questWithCategory = questList[position]
        val quest = questWithCategory.quest
        val categoryName = questWithCategory.categoryName
        val questId = questWithCategory.questId

        holder.questNameTextView.text = quest.name
        holder.questDateTextView.text = quest.date
        holder.questTimeTextView.text = "${quest.startTime} - ${quest.endTime}"

        try {
            holder.questColorView.setBackgroundColor(android.graphics.Color.parseColor(quest.color))
        } catch (e: IllegalArgumentException) {
            holder.questColorView.setBackgroundColor(android.graphics.Color.GRAY)
        }

        holder.viewDetailsButton.setOnClickListener {
            val message = """
                Description: ${quest.description}
                Difficulty: ${quest.difficulty} stars
                Min Daily Goal: ${quest.minGoal} hrs
                Max Daily Goal: ${quest.maxGoal} hrs
                Days of the Week: ${quest.daysOfWeek.joinToString(", ")}
            """.trimIndent()

            val context = holder.itemView.context

            //Show a dialog with the quest details and the delete button
            AlertDialog.Builder(context)
                .setTitle(quest.name)
                .setMessage(message)
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("Delete") { dialog, _ ->
                    //Confirm deletion
                    AlertDialog.Builder(context)
                        .setTitle("Confirm Deletion")
                        .setMessage("Are you sure you want to delete this quest?")
                        .setPositiveButton("Yes") { _, _ ->
                            deleteQuest(context, categoryName, questId)
                        }
                        .setNegativeButton("No") { confirmDialog, _ ->
                            confirmDialog.dismiss()
                        }
                        .show()
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun getItemCount(): Int = questList.size

    private fun deleteQuest(context: Context, categoryName: String, questId: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            //Trim and validate categoryName and questId
            val trimmedCategoryName = categoryName.trim()
            val trimmedQuestId = questId.trim()

            if (trimmedCategoryName.isEmpty() || trimmedQuestId.isEmpty()) {
                Log.e("QuestAdapter", "Invalid categoryName or questId. Aborting deletion.")
                Toast.makeText(context, "Failed to delete quest: Invalid quest data", Toast.LENGTH_SHORT).show()
                return
            }

            val path = "users/${currentUser.uid}/categories/$trimmedCategoryName/quests/$trimmedQuestId"
            Log.d("QuestAdapter", "Deleting quest at path: $path")

            val questReference = FirebaseDatabase.getInstance().getReference(path)

            //Remove the specific quest from Firebase
            questReference.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("QuestAdapter", "Quest deleted: $questId")
                    Toast.makeText(context, "Quest deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("QuestAdapter", "Failed to delete quest: ${task.exception?.message}")
                    Toast.makeText(context, "Failed to delete quest", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.e("QuestAdapter", "No current user. Cannot delete quest.")
        }
    }
}
