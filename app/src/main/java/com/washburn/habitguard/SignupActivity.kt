// ==============================================================================================
// SignupActivity.kt is the framework calling firebase.FirebaseAuthHelper to initializing user
// Refer to the Login Activity for authentication
//
// This will have user enter their registered email, password, and confirm password
// Function: registration and {third party registration} (NOT IMPLEMENTED)
// ==============================================================================================
package com.washburn.habitguard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.washburn.habitguard.databinding.ActivitySignupBinding
import com.washburn.habitguard.firebase.FirebaseAuthHelper

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.O)
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var authHelper: FirebaseAuthHelper
    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authHelper = FirebaseAuthHelper(this)
        firestoreHelper = FirestoreHelper()

        binding.btnTogglePassword.setOnClickListener {
            togglePasswordVisibility(binding.etSPassword, binding.btnTogglePassword)
        }

        binding.btnToggleConfPassword.setOnClickListener {
            togglePasswordVisibility(binding.etSConfPassword, binding.btnToggleConfPassword)
        }

        binding.btnSSignUp.setOnClickListener { signUpUser() }

        binding.tvRedirectLogin.setOnClickListener { login() }
    }

    private fun signUpUser() {
        val email = binding.etSEmailAddress.text.toString()
        val pass = binding.etSPassword.text.toString()
        val confirmPassword = binding.etSConfPassword.text.toString()

        when {
            email.isBlank() -> {
                Toast.makeText(this, "Email cannot be blank", Toast.LENGTH_SHORT).show()
                return
            }
            pass.isBlank() -> {
                Toast.makeText(this, "Password cannot be blank", Toast.LENGTH_SHORT).show()
                return
            }
            pass != confirmPassword -> {
                Toast.makeText(
                    this,
                    "Password and Confirm Password do not match",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            else -> {
                authHelper.signupWithEmail(
                    email = email,
                    password = pass,
                    onSuccess = {
                        // Launch CameraActivity to capture photo
                        val cameraIntent = Intent(this, CameraActivity::class.java)
                        startActivityForResult(cameraIntent, REQUEST_CAMERA)
                    },
                    onFailure = { errorMessage ->
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
    @Deprecated("Deprecated in favor of Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val photoUri = data?.getStringExtra("photoUri")
            if (photoUri != null) {
                firestoreHelper.saveUserInfo(
                    userId = authHelper.getCurrentUser()?.uid ?: return,
                    email = binding.etSEmailAddress.text.toString(),
                    username = binding.etSEmailAddress.text.toString().substringBefore("@"),
                    photoUri = photoUri,
                    onSuccess = {
                        Toast.makeText(this, "Successfully Signed Up", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onFailure = { e ->
                        authHelper.getCurrentUser()?.delete()
                        Toast.makeText(
                            this,
                            "Failed to create user profile: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    private fun togglePasswordVisibility(passwordEditText: android.widget.EditText, toggleButton: android.widget.ImageButton) {
        if (passwordEditText.transformationMethod == PasswordTransformationMethod.getInstance()) {
            passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            toggleButton.setImageResource(R.drawable.ic_visibility)
        } else {
            passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            toggleButton.setImageResource(R.drawable.ic_visibility_off)
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun login() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
    }
}