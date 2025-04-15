 /**===========================================================================================
 * LoginActivity - Handles user authentication for the HabitGuard application.
 *
 * Key Features:
 * 1. Email/password authentication (both online and offline modes)
 * 2. Password reset functionality
 * 3. "Remember me" credential persistence
 * 4. Network status awareness
 * 5. TODO: Future Idea: Google/GitHub/LinkedIn integration
 *
 * Dependencies:
 * - Layout: activity_login.xml
 * - Helper: FirebaseAuthHelper (handles Firebase authentication logic)
 * - Navigation: Used by SignupActivity and SideActivity
============================================================================================*/
package com.washburn.habitguard

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.washburn.habitguard.databinding.ActivityLoginBinding
import com.washburn.habitguard.NavigationHelper.navigateTo
import com.washburn.habitguard.firebase.AuthUtils.isOnline
import com.washburn.habitguard.firebase.AuthUtils.showToast
import com.washburn.habitguard.firebase.AuthUtils.togglePasswordVisibility
import com.washburn.habitguard.firebase.FirebaseAuthHelper
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authHelper: FirebaseAuthHelper
    private lateinit var credentialManager: CredentialManager // For Google Sign-In

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authHelper = FirebaseAuthHelper(this)
        credentialManager = CredentialManager.create(baseContext)

        setupInit()
    }

    private fun setupInit() {
        binding.apply {
            ivStatusIndicator.setImageResource(
                if (isOnline(this@LoginActivity)) R.drawable.ic_online else R.drawable.ic_offline // Online/Offline
            )

            val (email, password, rememberMe) = authHelper.loadSavedCredentials() // Remember me
            if (rememberMe) {
                cbRememberMe.isChecked = true
                etEmailAddress.setText(email)
                etPassword.setText(password)
            }

            togglePasswordVisibility(etPassword, btnTogglePassword) // Password Visibility
            tvForgotPassword.setOnClickListener { forgotPassword() }
            btnLogin.setOnClickListener { login() }
            btnGoogle.setOnClickListener { googleSignIn() }
            btnGithub.setOnClickListener { githubSignIn() }
            btnLinkedIn.setOnClickListener { linkedInSignIn() }
            tvRedirectSignUp.setOnClickListener { navigateTo(this@LoginActivity, SignupActivity::class.java, false) }
        }
    }

    // Handle user login
    private fun login() {
        val email = binding.etEmailAddress.text.toString()
        val pass = binding.etPassword.text.toString()

        if (email.isEmpty() || pass.isEmpty()) {
            showToast(this, "Please fill all fields")
            return
        }

        if (isOnline(this)){
            authHelper.loginWithEmail(
                email = email,
                password = pass,
                rememberMe = binding.cbRememberMe.isChecked,
                onSuccess = {
                    authHelper.getFirestoreHelper().updateLastLogin(
                        onSuccess = {
                            showToast(this, "Log In Successful")
                            navigateTo(this, SideActivity::class.java, true)
                        },
                        onFailure = {
                            showToast(this, "Log In Successful (timestamp update failed)")
                            navigateTo(this, SideActivity::class.java, true)
                        }
                    )
                },
                onFailure = { errorMessage -> showToast(this, errorMessage) }
            )
        } else {
            if (authHelper.checkOfflineLogin(email, pass)) {
                showToast(this, "Offline Log In Successful")
                navigateTo(this, SideActivity::class.java, true)
            } else {
                showToast(this, "Invalid credentials for offline login")
            }
        }
    }

    // Handle password reset
    private fun forgotPassword() {
        val email = binding.etEmailAddress.text.toString()
        authHelper.sendPasswordResetEmail(
            email = email,
            onSuccess = { showToast(this, "Password reset email sent.") },
            onFailure = {
                showToast(this,
                    if (email.isEmpty()) "Please enter your email. Then click Forgot Password."
                    else "Failed to send reset email."
                )
            }
        )
    }

    // Handle Google sign-in
    private fun googleSignIn() {
        if (!isOnline(this)) {
            showToast(this, "Google Sign-In requires internet connection")
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch { // TODO: Figure out why the val result is having issue
            try {
                val result = credentialManager.getCredential(
                    context = baseContext,
                    request = request,
                ) // TODO: from this point
                if (result.credential is CustomCredential && result.credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    // Create Google ID Token
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)

                    // Sign in to Firebase with using the token
                    authHelper.signInWithGoogle(
                        idToken = googleIdTokenCredential.idToken,
                        onSuccess = { user ->
                            showToast(this@LoginActivity, "Google Sign-In successful")
                            navigateTo(this@LoginActivity, SideActivity::class.java, true)
                            finish()
                        },
                        onFailure = { errorMessage ->
                            showToast(this@LoginActivity, "Google Sign-In failed: $errorMessage")
                        }
                    )
                } else {
                    showToast(this@LoginActivity, "Credential is not of type Google ID!")
                }
            } catch (e: GetCredentialException) {
                showToast(this@LoginActivity, "Couldn't retrieve user's credentials: ${e.localizedMessage}")
            }
        }
    }

    // Handle GitHub sign-in
    private fun githubSignIn() {
        // Implement GitHub sign-in logic here
    }
    // Handle LinkedIn sign-in
    private fun linkedInSignIn() {
        // Implement LinkedIn sign-in logic here
    }
}