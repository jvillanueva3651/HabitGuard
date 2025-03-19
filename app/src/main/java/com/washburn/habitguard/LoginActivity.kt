package com.washburn.habitguard

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.washburn.habitguard.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.tvRedirectSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmailAddress.text.toString()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter your email. Then click Forgot Password.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        binding.btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google login clicked", Toast.LENGTH_SHORT).show()
        }

        binding.btnGithub.setOnClickListener {
            Toast.makeText(this, "GitHub login clicked", Toast.LENGTH_SHORT).show()
        }

        binding.btnLinkedIn.setOnClickListener {
            Toast.makeText(this, "LinkedIn login clicked", Toast.LENGTH_SHORT).show()
        }

        loadSavedCredentials()

    }

    private fun login() {
        val email = binding.etEmailAddress.text.toString()
        val pass = binding.etPassword.text.toString()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (isOnline(this)) {
            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    if (uid != null) {
                        if (binding.cbRememberMe.isChecked) {
                            saveCredentials(email, pass)
                        } else {
                            clearCredentials()
                        }

                        Toast.makeText(this, "Log In Successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, SideActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to retrieve user ID", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    val errorMessage = when (it.exception) {
                        is FirebaseAuthInvalidUserException -> "User not found."
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                        else -> "Log In failed."
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        } else {    // Offline login NOT IMPLEMENTED YET
            val sharedPreferences = EncryptedSharedPreferences.create(
                applicationContext,
                "SecureLoginPrefs",
                MasterKey.Builder(applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val savedEmail = sharedPreferences.getString("email", "")
            val savedPassword = sharedPreferences.getString("password", "")

            if (email == savedEmail && pass == savedPassword) {
                Toast.makeText(this, "Offline Log In Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SideActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid credentials for offline login", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun togglePasswordVisibility() {
        if (binding.etPassword.transformationMethod == PasswordTransformationMethod.getInstance()){
            binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility)
        } else{
            binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        }
        binding.etPassword.setSelection(binding.etPassword.text.length)
    }

    private fun saveCredentials(email: String, password: String) {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Initialize EncryptedSharedPreferences
        val sharedPreferences = EncryptedSharedPreferences.create(
            applicationContext,
            "SecureLoginPrefs", // Name of the preferences file
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        with(sharedPreferences.edit()) {
            putString("email", email)
            putString("password", password)
            putBoolean("rememberMe", true)
            apply()
        }
    }

    private fun loadSavedCredentials() {
        // Create or retrieve the MasterKey
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Initialize EncryptedSharedPreferences
        val sharedPreferences = EncryptedSharedPreferences.create(
            applicationContext,
            "SecureLoginPrefs", // Name of the preferences file
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val email = sharedPreferences.getString("email", "")
        val password = sharedPreferences.getString("password", "")
        val rememberMe = sharedPreferences.getBoolean("rememberMe", false)

        if (rememberMe) {
            binding.cbRememberMe.isChecked = true
            binding.etEmailAddress.setText(email)
            binding.etPassword.setText(password)
        }
    }

    private fun clearCredentials() {
        // Create or retrieve the MasterKey
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Initialize EncryptedSharedPreferences
        val sharedPreferences = EncryptedSharedPreferences.create(
            applicationContext,
            "SecureLoginPrefs", // Name of the preferences file
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        with(sharedPreferences.edit()) {
            remove("email")
            remove("password")
            remove("rememberMe")
            apply()
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

}