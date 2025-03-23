package com.washburn.habitguard

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.washburn.habitguard.databinding.ActivitySignupBinding

@Suppress("DEPRECATION")
class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignupBinding

    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestoreHelper = FirestoreHelper()

        binding.btnTogglePassword.setOnClickListener { togglePasswordVisibility() }
        binding.btnToggleConfPassword.setOnClickListener { toggleConfPasswordVisibility() }
        binding.btnSSignUp.setOnClickListener { signUpUser() }
        binding.tvRedirectLogin.setOnClickListener { login() }
    }

    private fun signUpUser() {
        val email = binding.etSEmailAddress.text.toString()
        val pass = binding.etSPassword.text.toString()
        val confirmPassword = binding.etSConfPassword.text.toString()

        when {
            email.isBlank() -> Toast.makeText(this, "Email cannot be blank", Toast.LENGTH_SHORT).show()
            !isEmailValid(email) -> Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            pass.isBlank() -> Toast.makeText(this, "Password cannot be blank", Toast.LENGTH_SHORT).show()
            !isPasswordStrong(pass) -> Toast.makeText(
                this,
                "Password must be at least 8 characters long and include uppercase, lowercase, digit, and special character",
                Toast.LENGTH_LONG
            ).show()
            pass != confirmPassword -> Toast.makeText(
                this,
                "Password and Confirm Password do not match",
                Toast.LENGTH_SHORT
            ).show()
            else -> {
                auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: run {
                            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        // Launch CameraActivity to capture photo
                        val cameraIntent = Intent(this, CameraActivity::class.java)
                        startActivityForResult(cameraIntent, REQUEST_CAMERA)
                    } else {
                        Toast.makeText(this, "Sign Up Failed!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val photoUri = data?.getStringExtra("photoUri")
            if (photoUri != null) {
                val userId = auth.currentUser?.uid ?: return
                firestoreHelper.saveUserData(
                    userId = userId,
                    email = binding.etSEmailAddress.text.toString(),
                    username = generateDefaultUsername(binding.etSEmailAddress.text.toString()),
                    photoUri = photoUri,
                    onSuccess = {
                        Toast.makeText(this, "Successfully Signed Up", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onFailure = { e ->
                        auth.currentUser?.delete()
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
    companion object {
        private const val REQUEST_CAMERA = 1001
    }
    private fun generateDefaultUsername(email: String): String {
        return email.substringBefore("@")
    }
    private fun togglePasswordVisibility() {
        if (binding.etSPassword.transformationMethod == PasswordTransformationMethod.getInstance()){
            binding.etSPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility)
        } else{
            binding.etSPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        }
        binding.etSPassword.setSelection(binding.etSPassword.text.length)
    }
    private fun toggleConfPasswordVisibility() {
        if (binding.etSConfPassword.transformationMethod == PasswordTransformationMethod.getInstance()){
            binding.etSConfPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.btnToggleConfPassword.setImageResource(R.drawable.ic_visibility)
        } else{
            binding.etSConfPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.btnToggleConfPassword.setImageResource(R.drawable.ic_visibility_off)
        }
        binding.etSConfPassword.setSelection(binding.etSConfPassword.text.length)
    }
    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    private fun isPasswordStrong(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }
    private fun login() {
        startActivity(Intent(this, LoginActivity::class.java))
    }
}