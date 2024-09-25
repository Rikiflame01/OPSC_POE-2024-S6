package com.example.opsc_poe_task_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class QuestBoard : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var questRecyclerView: RecyclerView
    private lateinit var questAdapter: QuestAdapter
    private val questList = ArrayList<Quest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest_board)

        val questBoardButton = findViewById<Button>(R.id.questBoardButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val createQuestButton = findViewById<Button>(R.id.createQuestButton)


        questBoardButton.setOnClickListener {
            //Already on Quest Board screen, no action needed
        }

        profileButton.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        createQuestButton.setOnClickListener {
            val intent = Intent(this, CreateQuest::class.java)
            startActivity(intent)
        }


        //Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/categories")
        }

        //RecyclerView
        questRecyclerView = findViewById(R.id.questRecyclerView)
        questRecyclerView.layoutManager = LinearLayoutManager(this)
        questAdapter = QuestAdapter(questList)
        questRecyclerView.adapter = questAdapter

        loadQuests()

    }

    private fun loadQuests() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                questList.clear()

                for (categorySnapshot in snapshot.children) {
                    val categoryName = categorySnapshot.key
                    val questsSnapshot = categorySnapshot.child("quests")

                    for (questSnapshot in questsSnapshot.children) {
                        val quest = questSnapshot.getValue(Quest::class.java)
                        Log.d("QuestBoard", "Quest Loaded: ${quest?.name}")  // Add this line
                        quest?.let {
                            questList.add(it)
                        }
                    }
                }

                if (questList.isEmpty()) {
                    Log.d("QuestBoard", "No quests found")  // Log if no quests are found
                }

                questAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("QuestBoard", "Failed to load quests: ${error.message}")  // Add this line
            }
        })
    }

}
