package com.example.opsc_poe_task_app

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

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

            AlertDialog.Builder(context)
                .setTitle(quest.name)
                .setMessage(message)
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("Delete") { dialog, _ ->

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

    //In progress.... not working 100%

    private fun deleteQuest(context: Context, categoryName: String, questId: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            //Trim and validate categoryName and questId
            val trimmedCategoryName = categoryName.trim()
            val trimmedQuestId = questId.trim()

            if (trimmedCategoryName.isEmpty() || trimmedQuestId.isEmpty()) {
                Log.e("QuestAdapter", "Invalid categoryName or questId. Aborting deletion.")
                Toast.makeText(
                    context,
                    "Failed to delete quest: Invalid quest data",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val path =
                "users/${currentUser.uid}/categories/$trimmedCategoryName/quests/$trimmedQuestId"
            Log.d("QuestAdapter", "Deleting quest at path: $path")

            val questReference = FirebaseDatabase.getInstance().getReference(path)

            //Retrieve the quest data to get the photoPath
            questReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        //Get the photoPath from the quest data
                        val photoPath = dataSnapshot.child("photoPath").getValue(String::class.java)
                        Log.d("QuestAdapter", "Retrieved photoPath: $photoPath")

                        if (!photoPath.isNullOrEmpty()) {
                            //Delete the image from Firebase Storage
                            val storageReference = FirebaseStorage.getInstance().reference.child(photoPath)
                            Log.d("QuestAdapter", "Deleting image from storage: $photoPath")

                            storageReference.delete().addOnSuccessListener {
                                Log.d("QuestAdapter", "Image deleted from storage")
                                //Now delete the quest from the database
                                deleteQuestFromDatabase(context, questReference, questId)
                            }.addOnFailureListener { exception ->
                                Log.e(
                                    "QuestAdapter",
                                    "Failed to delete image: ${exception.message}"
                                )
                                Toast.makeText(
                                    context,
                                    "Failed to delete quest image: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                //Proceed to delete quest data even if image deletion fails
                                deleteQuestFromDatabase(context, questReference, questId)
                            }
                        } else {
                            Log.d("QuestAdapter", "No photoPath found. Proceeding to delete quest data.")
                            //No photoPath, just delete the quest
                            deleteQuestFromDatabase(context, questReference, questId)
                        }
                    } else {
                        Log.e("QuestAdapter", "Quest data not found at path: $path")
                        Toast.makeText(context, "Quest data not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("QuestAdapter", "Database error: ${databaseError.message}")
                    Toast.makeText(
                        context,
                        "Failed to delete quest: ${databaseError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            Log.e("QuestAdapter", "No current user. Cannot delete quest.")
        }
    }

    private fun deleteQuestFromDatabase(
        context: Context,
        questReference: DatabaseReference,
        questId: String
    ) {
        questReference.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("QuestAdapter", "Quest deleted: $questId")
                Toast.makeText(context, "Quest deleted", Toast.LENGTH_SHORT).show()

                val position = questList.indexOfFirst { it.questId == questId }
                if (position != -1) {
                    questList.removeAt(position)
                    notifyItemRemoved(position)
                }
            } else {
                Log.e("QuestAdapter", "Failed to delete quest: ${task.exception?.message}")
                Toast.makeText(context, "Failed to delete quest", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
