package com.example.opsc_poe_task_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

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
            val username = usernameEditText.text.toString().lowercase()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (email.isNotEmpty() && password == confirmPassword) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val currentUser = auth.currentUser
                            val uid = currentUser?.uid ?: "" // Use UID if no username is provided

                            val databaseReference = if (username.isNotEmpty()) {
                                //Store using the provided username
                                FirebaseDatabase.getInstance().getReference("users/$username")
                            } else {
                                //Store using the Firebase UID
                                FirebaseDatabase.getInstance().getReference("users/$uid")
                            }

                            val userMap = mapOf("email" to email)
                            databaseReference.setValue(userMap)

                            Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()

                            //Redirect to Profile
                            val intent = Intent(this, Profile::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please ensure all fields are filled and passwords match", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
