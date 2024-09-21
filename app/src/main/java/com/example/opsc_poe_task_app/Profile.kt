package com.example.opsc_poe_task_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Profile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val questBoardButton = findViewById<Button>(R.id.questBoardButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val createQuestButton = findViewById<Button>(R.id.createQuestButton)

        questBoardButton.setOnClickListener {
            val intent = Intent(this, QuestBoard::class.java)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            //Already on Profile screen, no action needed
        }

        createQuestButton.setOnClickListener {
            val intent = Intent(this, CreateQuest::class.java)
            startActivity(intent)
        }
    }
}
