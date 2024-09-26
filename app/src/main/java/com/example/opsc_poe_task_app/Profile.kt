package com.example.opsc_poe_task_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class Profile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val questBoardButton = findViewById<Button>(R.id.questBoardButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val createQuestButton = findViewById<Button>(R.id.createQuestButton)
        val timeSpentButton = findViewById<FloatingActionButton>(R.id.timeSpentButton)

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

        timeSpentButton.setOnClickListener {
            val intent = Intent(this, TimeSheet::class.java)
            startActivity(intent)
        }

        auth = FirebaseAuth.getInstance()

        //Initialize GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //Sign-Out Button Logic
        val signOutButton = findViewById<Button>(R.id.signOutButtonBottom)
        signOutButton.setOnClickListener {
            auth.signOut()
            //Sign out from Google
            googleSignInClient.signOut().addOnCompleteListener(this) {
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()

                //Redirect to login screen after signing out
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
