package com.example.opsc_poe_task_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class Profile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var profilePicture: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase Auth and Views
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        profilePicture = findViewById(R.id.profilePicture)
        userNameTextView = findViewById(R.id.userName)

        // Check if user is signed in and retrieve user information
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUserProfile(currentUser)
        } else {
            Toast.makeText(this, "No user is logged in", Toast.LENGTH_SHORT).show()
        }

        // Initialize UI elements
        val questBoardButton = findViewById<Button>(R.id.questBoardButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val createQuestButton = findViewById<Button>(R.id.createQuestButton)
        val timeSpentButton = findViewById<FloatingActionButton>(R.id.timeSpentButton)
        val checkGraphsButton = findViewById<Button>(R.id.checkGraphsButton) // Added this line

        // Set click listeners
        questBoardButton.setOnClickListener {
            val intent = Intent(this, QuestBoard::class.java)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            // Already on Profile screen, no action needed
        }

        createQuestButton.setOnClickListener {
            val intent = Intent(this, CreateQuest::class.java)
            startActivity(intent)
        }

        timeSpentButton.setOnClickListener {
            val intent = Intent(this, TimeSheet::class.java)
            startActivity(intent)
        }

        // **Add OnClickListener for Check Graphs Button**
        checkGraphsButton.setOnClickListener {
            val intent = Intent(this, TimeSheetGraphActivity::class.java)
            startActivity(intent)
        }

        // Initialize GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Ensure this ID exists in your resources
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Sign-Out
        val signOutButton = findViewById<Button>(R.id.signOutButtonBottom)
        signOutButton.setOnClickListener {
            auth.signOut()
            googleSignInClient.signOut().addOnCompleteListener(this) {
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                // Redirect to login screen
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    // Update user profile based on sign-in method
    private fun updateUserProfile(user: FirebaseUser) {
        var isGoogleUser = false
        for (userInfo in user.providerData) {
            if (userInfo.providerId == "google.com") {
                isGoogleUser = true
                break
            }
        }

        if (isGoogleUser) {
            // If signed in via Google, sync the Google profile picture and name
            val googleUser = GoogleSignIn.getLastSignedInAccount(this)
            if (googleUser != null) {
                val googleProfilePic = googleUser.photoUrl
                val googleDisplayName = googleUser.displayName

                userNameTextView.text = googleDisplayName ?: "Username"

                if (googleProfilePic != null) {
                    Glide.with(this).load(googleProfilePic).into(profilePicture)
                } else {
                    profilePicture.setImageResource(R.drawable.ic_profile_placeholder) // Placeholder image
                }
            } else {
                Log.d("Profile", "Google user is null")
            }
        } else {
            // If signed in via email/password, fetch username from the database
            fetchUsernameForNonGoogleUser(user)
        }
    }

    private fun fetchUsernameForNonGoogleUser(user: FirebaseUser) {
        val uid = user.uid
        val usersRef = database.getReference("users")

        // Query to find the username where uid matches
        val query = usersRef.orderByChild("uid").equalTo(uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Loop through the results (should be only one)
                    for (userSnapshot in dataSnapshot.children) {
                        val username = userSnapshot.child("username").getValue(String::class.java)
                        if (username != null) {
                            userNameTextView.text = username
                            break
                        }
                    }
                } else {
                    Toast.makeText(this@Profile, "Username not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Exception handling
                Toast.makeText(this@Profile, "Database error occurred.", Toast.LENGTH_SHORT).show()
            }
        })

        profilePicture.setImageResource(R.drawable.ic_profile_placeholder)
    }
}
