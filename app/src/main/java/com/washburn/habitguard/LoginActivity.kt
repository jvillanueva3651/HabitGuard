/**===========================================================================================
 * LoginActivity for login in user
 * REF    : USE_BY -> .SignupActivity (other half) & .SideActivity (main)
 *          USING  -> .firebase/FirebaseAuthHelper (authentication)
 *          LAYOUT -> layout/activity_login.xml
 * Purpose: Handles user authentication with email and password.
 * Fun    :  1. Email & password login (online/offline)
 *           2. TODO: Third-party provider logins (Google, GitHub, LinkedIn)
 *           3. Password reset functionality
 *           4. Network status awareness
 *           5. Remember me
 *           6. Authentication functionality through .firebase/FirebaseAuthHelper
============================================================================================*/
package com.washburn.habitguard


import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.washburn.habitguard.NavigationHelper.navigateTo
import com.washburn.habitguard.databinding.ActivityLoginBinding
import com.washburn.habitguard.firebase.AuthUtils.isOnline
import com.washburn.habitguard.firebase.AuthUtils.showToast
import com.washburn.habitguard.firebase.AuthUtils.togglePasswordVisibility
import com.washburn.habitguard.firebase.FirebaseAuthHelper
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var authHelper: FirebaseAuthHelper

    private lateinit var credentialManager: CredentialManager // Google ID Token

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authHelper = FirebaseAuthHelper(this)

        credentialManager = CredentialManager.create(baseContext) // Google ID Token

        setupInit() // Initialized view
        loadSavedCredentials() // Remember Me
        updateNetworkStatusIndicator() // Online/Offline
    }

    private fun setupInit() {
        togglePasswordVisibility(binding.etPassword, binding.btnTogglePassword)

        binding.tvForgotPassword.setOnClickListener { forgotPassword() }
        binding.btnLogin.setOnClickListener { login() }
        binding.btnGoogle.setOnClickListener { googleSignIn() }
        binding.btnGithub.setOnClickListener { githubSignIn() }
        binding.btnLinkedIn.setOnClickListener { linkedInSignIn() }
        binding.tvRedirectSignUp.setOnClickListener { signup() }
    }

    // Handle user login
    private fun login() {
        val email = binding.etEmailAddress.text.toString()
        val pass = binding.etPassword.text.toString()

        if (email.isEmpty() || pass.isEmpty()) {
            showToast(this, "Please fill all fields")
            return
        }

        if (isOnline(this)) performOnlineLogin(email, pass) else performOfflineLogin(email, pass)
    }

    private fun performOnlineLogin(email: String, password: String) {
        authHelper.loginWithEmail(
            email = email,
            password = password,
            rememberMe = binding.cbRememberMe.isChecked,
            onSuccess = { updateLastLoginAndProceed() },
            onFailure = { errorMessage -> showToast(this, errorMessage) }
        )
    }

    private fun performOfflineLogin(email: String, password: String) {
        if (authHelper.checkOfflineLogin(email, password)) {
            showToast(this, "Offline Log In Successful")
            navigateTo(this, SideActivity::class.java, true)
        } else {
            showToast(this, "Invalid credentials for offline login")
        }
    }

    private fun updateLastLoginAndProceed() {
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
    }

    // Update network status indicator
    private fun updateNetworkStatusIndicator() {
        binding.ivStatusIndicator.setImageResource(
            if (isOnline(this)) R.drawable.ic_online else R.drawable.ic_offline
        )
    }

    // Load saved credentials from SharedPreferences
    private fun loadSavedCredentials() {
        val (email, password, rememberMe) = authHelper.loadSavedCredentials()
        if (rememberMe) {
            binding.cbRememberMe.isChecked = true
            binding.etEmailAddress.setText(email)
            binding.etPassword.setText(password)
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
        // TODO: Figure out why the val result is having issue
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = baseContext,
                    request = request,
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
//                showToast("Couldn't retrieve user's credentials: ${e.localizedMessage}")
            }
        }
    }
    private fun handleSignIn(credential: Credential) {
        // Check if credential is of type Google ID
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            // Create Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
//            showToast("Credential is not of type Google ID!")
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        authHelper.signInWithGoogle(
            idToken = idToken,
            onSuccess = { user ->
//                showToast("Google Sign-In successful")
                startActivity(Intent(this, SideActivity::class.java))
                finish()
            },
            onFailure = { errorMessage ->
//                showToast("Google Sign-In failed: $errorMessage")
            }
        )
    }

    // Handle GitHub sign-in
    private fun githubSignIn() {
        // Implement GitHub sign-in logic here
    }
    // Handle LinkedIn sign-in
    private fun linkedInSignIn() {
        // Implement LinkedIn sign-in logic here
    }

    // Handle signup
    private fun signup() {
        navigateTo(this, SignupActivity::class.java, false)
    }
}