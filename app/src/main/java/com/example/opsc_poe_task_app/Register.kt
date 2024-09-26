package com.example.opsc_poe_task_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val backButton = findViewById<Button>(R.id.registerBackButton)
        val submitButton = findViewById<Button>(R.id.registerSubmitButton)
        val usernameEditText = findViewById<EditText>(R.id.registerUsernameEditText)
        val emailEditText = findViewById<EditText>(R.id.registerEmailEditText)
        val passwordEditText = findViewById<EditText>(R.id.registerPasswordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.registerConfirmPasswordEditText)

        backButton.setOnClickListener {
            finish()
        }

        submitButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim().lowercase()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    checkUsernameUniqueness(username, email, password)
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please ensure all fields are filled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUsernameUniqueness(username: String, email: String, password: String) {
        val usernameRef = database.getReference("users/$username")
        usernameRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    //Username already exists
                    Toast.makeText(
                        this@Register,
                        "Username already exists. Please choose another one.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    //Username is unique, proceed to create the user
                    createUser(username, email, password)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                //Exception handling
                Toast.makeText(this@Register, "Database error occurred.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentUser = auth.currentUser
                    val uid = currentUser?.uid ?: ""

                    val databaseReference = database.getReference("users/$username")

                    val userMap = mapOf(
                        "email" to email,
                        "username" to username,
                        "uid" to uid
                    )
                    databaseReference.setValue(userMap)

                    Toast.makeText(this@Register, "Account created", Toast.LENGTH_SHORT).show()

                    //Redirect to Profile
                    val intent = Intent(this@Register, Profile::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@Register, "Registration failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
