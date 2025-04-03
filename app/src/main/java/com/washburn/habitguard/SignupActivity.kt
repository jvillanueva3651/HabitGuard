/**===========================================================================================
 * SignupActivity for signing up user
 * REF    : USE_BY -> .LoginActivity (other half)
 *          USING  -> .firebase/FirebaseAuthHelper (authentication) & .FirestoreHelper (database)
 *          LAYOUT -> layout/activity_signup.xml
 * Purpose: Handles user authentication with email and password.
 * Fun:  1. Email, password, and confirm password signing up
 *       2. Camera permission request for profile picture
 *       3. TODO: Third-party provider signup (Google, GitHub, LinkedIn)
 *       4. Authentication functionality through .firebase/FirebaseAuthHelper
============================================================================================*/
package com.washburn.habitguard

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.text.method.PasswordTransformationMethod
import android.text.method.HideReturnsTransformationMethod
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.washburn.habitguard.firebase.FirebaseAuthHelper
import com.washburn.habitguard.databinding.ActivitySignupBinding

@RequiresApi(Build.VERSION_CODES.O)
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    private lateinit var authHelper: FirebaseAuthHelper

    private lateinit var firestoreHelper: FirestoreHelper

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            handleCameraResult(result.data)
        }
    }

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            showToast("Profile picture is set to default")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authHelper = FirebaseAuthHelper(this)

        firestoreHelper = FirestoreHelper()

        setupPasswordToggle(binding.etSPassword, binding.btnTogglePassword)
        setupPasswordToggle(binding.etSConfPassword, binding.btnToggleConfPassword)

        binding.btnSSignUp.setOnClickListener { signUpUser() }

        binding.tvRedirectLogin.setOnClickListener { login() }
    }

    // Validate and process user registration
    private fun signUpUser() {
        val email = binding.etSEmailAddress.text.toString()
        val pass = binding.etSPassword.text.toString()
        val confirmPassword = binding.etSConfPassword.text.toString()

        when {
            email.isBlank() -> showToast("Email cannot be blank")
            pass.isBlank() -> showToast("Password cannot be blank")
            pass != confirmPassword -> showToast("Password and Confirm Password do not match")
            else -> authHelper.signupWithEmail(
                email = email,
                password = pass,
                onSuccess = { checkCameraPermissionAndLaunch() },
                onFailure = { errorMessage -> showToast(errorMessage) }
            )
        }
    }

    // Check and request camera permission
    private fun checkCameraPermissionAndLaunch() {
        AlertDialog.Builder(this)
            .setTitle("Profile Picture")
            .setMessage("Would you like to add a profile picture?")
            .setPositiveButton("Take Photo") { _, _ ->
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        launchCamera()
                    }
                    else -> {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }
            }
            .setNegativeButton("Use Default") { _, _ ->
                saveUserProfile()
            }
            .show()
    }
    // Launch camera
    private fun launchCamera() {
        val cameraIntent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(cameraIntent)
    }
    // Handle camera result
    private fun handleCameraResult(data: Intent?) {
        val photoUri = data?.getStringExtra("photoUri")
        saveUserProfile(photoUri)
    }

    // Save user profile to Firestore
    private fun saveUserProfile(photoUri: String? = null) {

        if (photoUri == null) {
            showToast("Failed to capture photo, photo set to default")
        }

        val photoToSave = photoUri ?: "drawable://${R.drawable.ic_launcher_foreground}"

        firestoreHelper.saveUserInfo(
            email = binding.etSEmailAddress.text.toString(),
            username = binding.etSEmailAddress.text.toString().substringBefore("@"),
            photoUri = photoToSave,
            onSuccess = {
                showToast("Successfully Signed Up")
                login()
            },
            onFailure = { e ->
                showToast("Failed to create user profile: ${e.message}")
            }
        )
    }

    // Toggle password visibility
    private fun setupPasswordToggle(
        editText: android.widget.EditText,
        toggleButton: android.widget.ImageButton
    ) {
        toggleButton.setOnClickListener {
            val isPasswordVisible = editText.transformationMethod == PasswordTransformationMethod.getInstance()
            editText.transformationMethod = if (isPasswordVisible) {
                toggleButton.setImageResource(R.drawable.ic_visibility)
                HideReturnsTransformationMethod.getInstance()
            } else {
                toggleButton.setImageResource(R.drawable.ic_visibility_off)
                PasswordTransformationMethod.getInstance()
            }
            editText.setSelection(editText.text.length)
        }
    }

    // Direct to login
    private fun login() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}