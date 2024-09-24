package com.example.opsc_poe_task_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Profile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient



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
            //Already on Create Quest screen, no action needed
        }

        createQuestButton.setOnClickListener {
            val intent = Intent(this, CreateQuest::class.java)
            startActivity(intent)
        }

        auth = FirebaseAuth.getInstance()

        //Initialize GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // Ensure you have this string in your resources
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val createCategoryButton = findViewById<Button>(R.id.createCategoryButton)
        val categoryInputField = findViewById<EditText>(R.id.categoryInputField)

        //Category creation logic
        createCategoryButton.setOnClickListener {
            val categoryName = categoryInputField.text.toString().trim()

            if (categoryName.length < 5) {
                Toast.makeText(this, "Category name must be 5 letters or more", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                val uid = currentUser.uid
                val databaseReference = FirebaseDatabase.getInstance().getReference("users/$uid/categories")

                // Check if category already exists
                databaseReference.child(categoryName).get().addOnSuccessListener {
                    if (it.exists()) {
                        Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show()
                    } else {
                        // Add the new category
                        databaseReference.child(categoryName).setValue(true).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
                                categoryInputField.text.clear()  // Clear the input field
                            } else {
                                Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

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
