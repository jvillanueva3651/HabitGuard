package com.washburn.habitguard.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.ActivityProfileEditBinding
import java.util.*
import androidx.core.net.toUri

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.O)
class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var firestoreHelper: FirestoreHelper
    private val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .circleCrop()
                .into(binding.profilePhoto)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreHelper = FirestoreHelper()

        setupViews()
        loadProfileData()

        // Handle Birthday CalendarView
        binding.birthdayCalendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$year-${month + 1}-$dayOfMonth"
            // Store the selected date in a hidden EditText or directly use it
            Toast.makeText(this, "Selected Date: $selectedDate", Toast.LENGTH_SHORT).show()
        }

        // Handle Gender RadioGroup
        binding.genderRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.maleRadioButton -> Toast.makeText(this, "Male selected", Toast.LENGTH_SHORT).show()
                R.id.femaleRadioButton -> Toast.makeText(this, "Female selected", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle Map Button for Address
        binding.mapButton.setOnClickListener {
            // Launch Google Maps or another map activity to fetch the address
            val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=".toUri())
            startActivity(intent)
        }

    }

    private fun setupViews() {
        binding.changePhotoButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        } // Handle Change Photo Button

        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                saveProfileData()
            }
        } // Handle Save Button

//        binding.backButton.setOnClickListener {
//            onBackPressed()
//        }
    }

    private fun loadProfileData() {
        currentUser?.uid?.let { userId ->
            firestoreHelper.getUserDocument(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        populateFields(document)
                    } else {
//                        createNewProfileDocument()
                    }
                }
                .addOnFailureListener { e ->
                    showErrorToast("Failed to load profile: ${e.message}")
                }
        } ?: showErrorToast("User not authenticated")
    }

    private fun populateFields(document: DocumentSnapshot) {
        with(binding) {
            emailEditText.setText(document.getString("email") ?: currentUser?.email ?: "")
            usernameEditText.setText(document.getString("username") ?: "")
            firstNameEditText.setText(document.getString("firstName") ?: "")
            lastNameEditText.setText(document.getString("lastName") ?: "")

            document.getString("birthday")?.let { birthday ->
                birthdayCalendarView.date = parseDateToMillis(birthday)
            }

            when (document.getString("gender")) {
                "Male" -> maleRadioButton.isChecked = true
                "Female" -> femaleRadioButton.isChecked = true
            }

            addressEditText.setText(document.getString("address") ?: "")
            cityEditText.setText(document.getString("city") ?: "")
            stateEditText.setText(document.getString("state") ?: "")
            zipCodeEditText.setText(document.getString("zipCode") ?: "")
            phone1EditText.setText(document.getString("phone1") ?: "")
            phone2EditText.setText(document.getString("phone2") ?: "")

            document.getString("photoUri")?.takeIf { it.isNotEmpty() }?.let { uri ->
                Glide.with(this@ProfileEditActivity)
                    .load(uri)
                    .circleCrop()
                    .into(profilePhoto)
            }
        }
    }

//    private fun createNewProfileDocument() {
//        currentUser?.uid?.let { userId ->
//            firestoreHelper.saveUserInfo(
//                userId,
//                currentUser.email ?: "",
//                currentUser.displayName ?: "John Doe",
//                null, // photoUri
//                { loadProfileData() },
//                { e -> showErrorToast("Failed to create profile: ${e.message}") }
//            )
//        } ?: showErrorToast("User not authenticated")
//    }

    private fun validateInputs(): Boolean {
        with(binding) {
            val email = emailEditText.text.toString()
            if (!isValidEmail(email)) {
                emailEditText.error = "Invalid email format"
                return false
            }

            if (usernameEditText.text.isNullOrEmpty()) {
                usernameEditText.error = "Username cannot be empty"
                return false
            }

            return true
        }
    }

    private fun saveProfileData() {
        currentUser?.uid?.let { userId ->
            val updates = prepareProfileUpdates()

            if (selectedImageUri != null) {
                uploadImageAndSaveProfile(userId, updates)
            } else {
                saveProfileToFirestore(userId, updates)
            }
        } ?: showErrorToast("User not authenticated")
    }

    private fun prepareProfileUpdates(): HashMap<String, Any> {
        return hashMapOf(
            "email" to binding.emailEditText.text.toString(),
            "username" to binding.usernameEditText.text.toString(),
            "firstName" to binding.firstNameEditText.text.toString(),
            "lastName" to binding.lastNameEditText.text.toString(),
            "birthday" to formatDateFromMillis(binding.birthdayCalendarView.date),
            "gender" to when (binding.genderRadioGroup.checkedRadioButtonId) {
                R.id.maleRadioButton -> "Male"
                R.id.femaleRadioButton -> "Female"
                else -> "Not specified"
            },
            "address" to binding.addressEditText.text.toString(),
            "city" to binding.cityEditText.text.toString(),
            "state" to binding.stateEditText.text.toString(),
            "zipCode" to binding.zipCodeEditText.text.toString(),
            "phone1" to binding.phone1EditText.text.toString(),
            "phone2" to binding.phone2EditText.text.toString(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun uploadImageAndSaveProfile(userId: String, updates: HashMap<String, Any>) {
        selectedImageUri?.let { uri ->
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("profile_images/${userId}.jpg")

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        updates["photoUri"] = downloadUri.toString()
                        saveProfileToFirestore(userId, updates)
                    }
                }
                .addOnFailureListener { e ->
                    showErrorToast("Image upload failed: ${e.message}")
                    saveProfileToFirestore(userId, updates) // Save without image update
                }
        }
    }

    private fun saveProfileToFirestore(userId: String, updates: HashMap<String, Any>) {
        firestoreHelper.saveUserProfile(
            userId,
            updates,
            {
                checkEmailUpdateNeeded(updates["email"] as String)
            },
            { e ->
                showErrorToast("Failed to save profile: ${e.message}")
            }
        )
    }

    private fun checkEmailUpdateNeeded(newEmail: String) {
        if (currentUser?.email != newEmail) {
            showPasswordDialogForEmailUpdate(newEmail)
        } else {
            showSuccessToast("Profile updated successfully")
        }
    }

    private fun showPasswordDialogForEmailUpdate(newEmail: String) {
        val passwordEditText = androidx.appcompat.widget.AppCompatEditText(this).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Enter your current password"
        }

        AlertDialog.Builder(this)
            .setTitle("Verify Password")
            .setMessage("To update your email, please verify your current password")
            .setView(passwordEditText)
            .setPositiveButton("Confirm") { _, _ ->
                val password = passwordEditText.text?.toString() ?: ""
                if (password.isNotEmpty()) {
                    reauthenticateAndUpdateEmail(newEmail, password)
                } else {
                    showErrorToast("Password cannot be empty")
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                showSuccessToast("Profile updated (email not changed)")
            }
            .show()
    }

    private fun reauthenticateAndUpdateEmail(newEmail: String, password: String) {
        val credential = EmailAuthProvider.getCredential(currentUser?.email ?: "", password)
        currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    currentUser.updateEmail(newEmail)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                showSuccessToast("Profile and email updated successfully")
                            } else {
                                showErrorToast("Email update failed: ${updateTask.exception?.message}")
                            }
                        }
                } else {
                    showErrorToast("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    // Helper functions
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun parseDateToMillis(date: String): Long {
        val parts = date.split("-").map { it.toInt() }
        val calendar = Calendar.getInstance().apply {
            set(parts[0], parts[1] - 1, parts[2])
        }
        return calendar.timeInMillis
    }

    private fun formatDateFromMillis(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun showSuccessToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}