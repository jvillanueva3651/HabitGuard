// ==============================================================================================
// LoginActivity.kt is the framework calling firebase.FirebaseAuthHelper to log in user
// Refer to the Signup Activity for initializing user
//
// This will have user enter their registered email and password
// Function: authentication, remember me, {forgot password and third party login} (NOT IMPLEMENTED)
// ==============================================================================================
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
import com.washburn.habitguard.databinding.ActivityLoginBinding
import com.washburn.habitguard.firebase.FirebaseAuthHelper

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authHelper: FirebaseAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authHelper = FirebaseAuthHelper(this)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSavedCredentials() // Remember Me
        updateNetworkStatusIndicator() // Online/Offline

        binding.btnTogglePassword.setOnClickListener { togglePasswordVisibility() }

        binding.tvForgotPassword.setOnClickListener { forgotPassword() }

        binding.btnLogin.setOnClickListener { login() } // Email Login for Online/Offline

        binding.btnGoogle.setOnClickListener { googleSignIn() }

        binding.btnGithub.setOnClickListener { githubSignIn() }

        binding.btnLinkedIn.setOnClickListener { linkedInSignIn() }

        binding.tvRedirectSignUp.setOnClickListener { signup() }
    }

    private fun loadSavedCredentials() {
        val (email, password, rememberMe) = authHelper.loadSavedCredentials()
        if (rememberMe) {
            binding.cbRememberMe.isChecked = true
            binding.etEmailAddress.setText(email)
            binding.etPassword.setText(password)
        }
    }

    private fun updateNetworkStatusIndicator() {
        if (isOnline(this)) {
            binding.ivStatusIndicator.setImageResource(R.drawable.ic_online)
        } else {
            binding.ivStatusIndicator.setImageResource(R.drawable.ic_offline)
        }
    }

    private fun togglePasswordVisibility() {
        if (binding.etPassword.transformationMethod == PasswordTransformationMethod.getInstance()) {
            binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility)
        } else {
            binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        }
        binding.etPassword.setSelection(binding.etPassword.text.length)
    }

    private fun forgotPassword() {
        val email = binding.etEmailAddress.text.toString()
        authHelper.sendPasswordResetEmail(
            email = email,
            onSuccess = {
                Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
            },
            onFailure = {
                Toast.makeText(
                    this,
                    if (email.isEmpty()) "Please enter your email. Then click Forgot Password."
                    else "Failed to send reset email.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun login() {
        val email = binding.etEmailAddress.text.toString()
        val pass = binding.etPassword.text.toString()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (isOnline(this)) {
            authHelper.loginWithEmail(
                email = email,
                password = pass,
                rememberMe = binding.cbRememberMe.isChecked,
                onSuccess = { user ->
                    Toast.makeText(this, "Log In Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SideActivity::class.java))
                    finish()
                },
                onFailure = { errorMessage ->
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            if (authHelper.checkOfflineLogin(email, pass)) {
                Toast.makeText(this, "Offline Log In Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SideActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid credentials for offline login", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun googleSignIn() {
        // Implement Google sign-in logic here
    }

    private fun githubSignIn() {
        startActivity(Intent(this, SideActivity::class.java))
        finish()
    }

    private fun linkedInSignIn() {
        // Implement LinkedIn sign-in logic here
    }

    private fun signup() {
        startActivity(Intent(this, SignupActivity::class.java))
        finish()
    }
}