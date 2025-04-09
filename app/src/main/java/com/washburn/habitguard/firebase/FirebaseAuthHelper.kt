/**============================================================================================
 * FirebaseAuthHelper.kt - Firebase authentication helper for login and signup
 * REF    : USE_BY -> .LoginActivity (login) & .SignupActivity (signup)
 *          USING  -> Firebase Authentication SDK & .FirestoreHelper (for user data)
 * Purpose: Handles all Firebase authentication operations including:
 *          - Email/password authentication
 *          - Credential management (remember me)
 *          - Password reset
 *          - TODO: Third-party authentication (Google)
 * Fun    :  1. Secure credential storage using EncryptedSharedPreferences
 *           2. Email validation
 *           3. Password strength validation
 *           4. Offline login capability
 *           5. Google Sign-In integration
============================================================================================*/
package com.washburn.habitguard.firebase


import android.content.Context
import android.os.Build
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.washburn.habitguard.FirestoreHelper

import com.google.firebase.auth.GoogleAuthProvider

@RequiresApi(Build.VERSION_CODES.O)
class FirebaseAuthHelper(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val firestoreHelper = FirestoreHelper()

    fun getFirestoreHelper(): FirestoreHelper = firestoreHelper

    // Sign Out
    fun signOut() = auth.signOut()

    /**============================================================================================
    * Login Activity
    *============================================================================================*/
    // signInWithEmailAndPassword Login
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
                        firestoreHelper.getUserDocument(user.uid).get().addOnCompleteListener { firestoreTask ->
                            if (firestoreTask.isSuccessful && firestoreTask.result.exists()) {
                                if (rememberMe) saveCredentials(email, password) else clearCredentials()
                                onSuccess(user)
                            } else {
                                auth.signOut()
                                onFailure("User account not properly set up")
                            }
                        }
                    } else {
                        onFailure("Failed to retrieve user ID")
                    }
                } else {
                    onFailure(when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "User not found."
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                        else -> "Log In failed."
                    })
                }
            }
    }

    // Forgot sendPasswordResetEmail
    fun sendPasswordResetEmail(
        email: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (email.isNotEmpty()) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess("Check your email for a password reset link.")
                    } else {
                        onFailure("Failed to send password reset email.")
                    }
                }
        } else {
            onFailure("Please enter your email.")
        }
    }

    // EncryptedSharedPreferences
    private fun getEncryptedSharedPreferences() = EncryptedSharedPreferences.create(
        context,
        "SecureLoginPrefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    // Save sharedPreferences
    fun saveCredentials(email: String, password: String) {
        getEncryptedSharedPreferences().edit().apply {
            putString("email", email)
            putString("password", password)
            putBoolean("rememberMe", true)
            apply()
        }
    }
    // Clear sharedPreferences
    fun clearCredentials() {
        getEncryptedSharedPreferences().edit().apply {
            remove("email")
            remove("password")
            remove("rememberMe")
            apply()
        }
    }
    // Load sharedPreferences
    fun loadSavedCredentials(): Triple<String?, String?, Boolean> {
        val prefs = getEncryptedSharedPreferences()
        return Triple(
            prefs.getString("email", ""),
            prefs.getString("password", ""),
            prefs.getBoolean("rememberMe", false)
        )
    }
    // Offline Login
    fun checkOfflineLogin(email: String, password: String): Boolean {
        val (savedEmail, savedPassword, _) = loadSavedCredentials()
        return email == savedEmail && password == savedPassword
    }
    //=============================================================================================
    //=============================================================================================

    /**============================================================================================
    * Signup Activity
    *============================================================================================*/
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
            onFailure("Password must be 8+ chars with: A-Z, a-z, 0-9, and special char")
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

    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordStrong(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }
    // ============================================================================================

    /**
     * Third Party Login
    */
    // Google Sign In =============================================================================
    // TODO: See LoginActivity for Google Sign In
    fun signInWithGoogle(
        idToken: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onFailure("Google Sign-In failed: No user returned")
                    }
                } else {
                    onFailure("Google authentication failed: ${task.exception?.message}")
                }
            }
    }

}