// ==============================================================================================
// FirebaseAuthHelper.kt is the firestore database for both login and signup
// Refer to the FirestoreHelper for saving and updating data
//
// This is where firebase authentication is being called for login and signup
// Function: logic behind login and signup firebase authentication
// ==============================================================================================
package com.washburn.habitguard.firebase

import android.content.Context
import android.util.Patterns
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.washburn.habitguard.FirestoreHelper

class FirebaseAuthHelper(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestoreHelper = FirestoreHelper()

    // Login Activity ===========================================================================
    fun loginWithEmail(
        email: String,
        password: String,
        rememberMe: Boolean,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Verify user exists in Firestore
                        firestoreHelper.getUserDocument(user.uid).addOnCompleteListener { firestoreTask ->
                            if (firestoreTask.isSuccessful && firestoreTask.result.exists()) {
                                if (rememberMe) {
                                    saveCredentials(email, password)
                                } else {
                                    clearCredentials()
                                }
                                onSuccess(user)
                            } else {
                                // User authenticated but doesn't exist in Firestore
                                auth.signOut()
                                onFailure("User account not properly set up")
                            }
                        }
                    } else {
                        onFailure("Failed to retrieve user ID")
                    }
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "User not found."
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                        else -> "Log In failed."
                    }
                    onFailure(errorMessage)
                }
            }
    }

    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        if (email.isNotEmpty()) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        onFailure()
                    }
                }
        } else {
            onFailure()
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() {
        auth.signOut()
    }

    private fun saveCredentials(email: String, password: String) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "SecureLoginPrefs",
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

    private fun clearCredentials() {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "SecureLoginPrefs",
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

    fun loadSavedCredentials(): Triple<String?, String?, Boolean> {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "SecureLoginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val email = sharedPreferences.getString("email", "")
        val password = sharedPreferences.getString("password", "")
        val rememberMe = sharedPreferences.getBoolean("rememberMe", false)

        return Triple(email, password, rememberMe)
    }

    fun checkOfflineLogin(email: String, password: String): Boolean {
        val (savedEmail, savedPassword, _) = loadSavedCredentials()
        return email == savedEmail && password == savedPassword
    }
    // Login Activity ===========================================================================

    // Signup Activity ==========================================================================
    fun signupWithEmail(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isEmailValid(email)) {
            onFailure("Invalid email format")
            return
        }

        if (!isPasswordStrong(password)) {
            onFailure("Password must be at least 8 characters long and include uppercase, lowercase, digit, and special character")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onFailure("Authentication error")
                    }
                } else {
                    onFailure("Sign Up Failed: ${task.exception?.message ?: "Unknown error"}")
                }
            }
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordStrong(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }
    // Signup Activity ==========================================================================
}