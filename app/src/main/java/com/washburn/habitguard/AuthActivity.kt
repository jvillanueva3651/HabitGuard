package com.washburn.habitguard

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Get UI elements
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val loginButton = findViewById<Button>(R.id.loginButton)

        // Sign Up Button Click
        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (validateInputs(email, password)) {
                registerUser(email, password)
            }
        }

        // Login Button Click
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }
    }

    // Validate email and password
    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Invalid email")
            return false
        }
        if (password.isEmpty() || password.length < 6) {
            showToast("Password must be at least 6 characters")
            return false
        }
        return true
    }

    // Register a new user
    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Registration successful!")
                    // Redirect to MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showToast("Registration failed: ${task.exception?.message}")
                }
            }
    }

    // Login existing user
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Login successful!")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showToast("Login failed: ${task.exception?.message}")
                }
            }
    }

    // Show Toast messages
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Check if user is already logged in
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Redirect to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}