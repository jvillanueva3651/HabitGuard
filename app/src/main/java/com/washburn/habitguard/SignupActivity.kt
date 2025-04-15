/**===========================================================================================
 * SignupActivity - Handles user registration for the HabitGuard application.
 *
 * Key Features:
 * 1. Email/password registration with confirmation
 * 2. Profile picture capture (with camera permission handling)
 * 3. User profile creation in Firestore
 * 4. Navigation to login after successful registration
 *
 * Dependencies:
 * - Layout: activity_signup.xml
 * - Helpers: FirebaseAuthHelper (authentication), FirestoreHelper (database)
 * - Navigation: Used by LoginActivity
============================================================================================*/
package com.washburn.habitguard

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.washburn.habitguard.NavigationHelper.navigateTo
import com.washburn.habitguard.databinding.ActivitySignupBinding
import com.washburn.habitguard.firebase.AuthUtils.showToast
import com.washburn.habitguard.firebase.AuthUtils.togglePasswordVisibility
import com.washburn.habitguard.firebase.FirebaseAuthHelper

@RequiresApi(Build.VERSION_CODES.O)
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    private lateinit var authHelper: FirebaseAuthHelper

    private lateinit var firestoreHelper: FirestoreHelper

    private lateinit var cameraHelper: CameraHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authHelper = FirebaseAuthHelper(this)
        firestoreHelper = FirestoreHelper()
        cameraHelper = CameraHelper(this).apply {
            onPhotoTaken = { uri -> saveUserProfile(uri.toString()) }
            onPermissionDenied = {
                showToast(this@SignupActivity, "Profile picture is set to default")
                saveUserProfile()
            }
        }

        setupInit() // Initialized view
    }

    private fun setupInit() {
        binding.apply {
        togglePasswordVisibility(etSPassword, btnTogglePassword)
        togglePasswordVisibility(etSConfPassword, btnToggleConfPassword)

        btnSSignUp.setOnClickListener { signUpUser() }
        tvRedirectLogin.setOnClickListener { navigateTo(this@SignupActivity, LoginActivity::class.java, true) }
        }
    }

    // Validate and process user registration
    private fun signUpUser() {
        val email = binding.etSEmailAddress.text.toString()
        val pass = binding.etSPassword.text.toString()
        val confirmPassword = binding.etSConfPassword.text.toString()

        when {
            email.isBlank() -> showToast(this, "Email cannot be blank")
            pass.isBlank() -> showToast(this, "Password cannot be blank")
            pass != confirmPassword -> showToast(this, "Password and Confirm Password do not match")
            else -> authHelper.signupWithEmail(
                email = email,
                password = pass,
                onSuccess = {
                    AlertDialog.Builder(this)
                        .setTitle("Profile Picture")
                        .setMessage("Would you like to add a profile picture?")
                        .setPositiveButton("Take Photo") { _, _ ->
                            cameraHelper.checkCameraPermissionAndLaunch()
                        }
                        .setNegativeButton("Use Default") { _, _ ->
                            saveUserProfile()
                        }
                        .show()
                },
                onFailure = { errorMessage -> showToast(this, errorMessage) }
            )
        }
    }

    // Save user profile to Firestore
    private fun saveUserProfile(photoUri: String? = null) {
        firestoreHelper.saveUserInfo(
            email = binding.etSEmailAddress.text.toString(),
            username = binding.etSEmailAddress.text.toString().substringBefore("@"),
            photoUri = photoUri ?: "drawable://${R.drawable.ic_launcher_foreground}",
            onSuccess = {
                showToast(this, "Successfully Signed Up")
                navigateTo(this, LoginActivity::class.java, true)
            },
            onFailure = { e ->
                showToast(this, "Failed to create user profile: ${e.message}")
            }
        )
    }
}