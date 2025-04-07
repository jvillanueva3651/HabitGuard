package com.washburn.habitguard.settings

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.washburn.habitguard.CameraHelper
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.ActivityProfileEditBinding
import com.washburn.habitguard.firebase.AuthUtils.showToast
import java.util.*
import android.util.Patterns
import androidx.core.net.toUri
import java.text.SimpleDateFormat

@RequiresApi(Build.VERSION_CODES.O)
class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private val firestoreHelper = FirestoreHelper()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser get() = auth.currentUser
    private val cameraHelper = CameraHelper(this)
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedImageUri = uri
            loadImageIntoView(uri) }
    }

    private var selectedImageUri: Uri? = null
    private var originalEmail: String = ""
    private var isEmailChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCameraHelper()
        setupViews()
        loadProfileData()
    }

    // Region: Setup Methods
    private fun setupCameraHelper() {
        cameraHelper.onPhotoTaken = { uri ->
            selectedImageUri = uri
            loadImageIntoView(uri)
        }
        cameraHelper.onPermissionDenied = {
            showToast(this, "Camera permission denied")
        }
    }

    private fun setupViews() {
        binding.changePhotoButton.setOnClickListener { showPhotoSourceDialog() }
        binding.mapButton.setOnClickListener { launchMaps() }
        binding.saveButton.setOnClickListener { attemptSave() }
        binding.birthdayEditText.setOnClickListener { showDatePickerDialog() }
        binding.genderRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.maleRadioButton -> showToast(this, "Male selected")
                R.id.femaleRadioButton -> showToast(this, "Female selected")
            }
        }
    }

    private fun showPhotoSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Update Profile Picture")
            .setItems(arrayOf("Take Photo", "Choose from Gallery", "Cancel")) { _, which ->
                when (which) {
                    0 -> cameraHelper.checkCameraPermissionAndLaunch()
                    1 -> imagePicker.launch("image/*")
                }
            }
            .show()
    }

    private fun loadImageIntoView(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .circleCrop()
            .into(binding.profilePhoto)
    }

    private fun loadProfileData() {
        firestoreHelper.getUserInfoDocumentField(firestoreHelper.getCurrentUserId().toString()).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    populateFields(document)
                    originalEmail = document.getString("email") ?: currentUser?.email ?: ""
                } else {
                    showToast(this, "No profile data found")
                }
            }
            .addOnFailureListener { e ->
                showToast(this, "Failed to load profile: ${e.message}")
            }
    }

    private fun populateFields(document: DocumentSnapshot) {
        with(binding) {
            // Personal Info
            emailEditText.setText(document.getString("email") ?: "")
            usernameEditText.setText(document.getString("username") ?: "")
            firstNameEditText.setText(document.getString("firstName") ?: "")
            lastNameEditText.setText(document.getString("lastName") ?: "")

            document.getString("birthday")?.let { dateStr ->
                birthdayEditText.setText(dateStr)
            }

            // Gender Selection
            when (document.getString("gender")) {
                "Male" -> maleRadioButton.isChecked = true
                "Female" -> femaleRadioButton.isChecked = true
            }

            // Address Info
            addressEditText.setText(document.getString("address") ?: "")
            cityEditText.setText(document.getString("city") ?: "")
            stateEditText.setText(document.getString("state") ?: "")
            zipCodeEditText.setText(document.getString("zipCode") ?: "")

            // Contact Info
            phone1EditText.setText(document.getString("phone1") ?: "")
            phone2EditText.setText(document.getString("phone2") ?: "")

            // Profile Photo
            document.getString("photoUri")?.takeIf { it.isNotBlank() }?.let { uri ->
                Glide.with(this@ProfileEditActivity)
                    .load(uri)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .circleCrop()
                    .into(profilePhoto)
            }
        }
    }
    // End Region

    // Region: Data Saving
    private fun attemptSave() {
        if (!validateInputs()) return

        val updates = prepareProfileUpdates()

        if (selectedImageUri != null) {
            uploadImageAndSave(updates)
        } else {
            saveProfileToFirestore(updates)
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        try {
            val currentDate = binding.birthdayEditText.text.toString()
            if (currentDate.isNotEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.parse(currentDate)?.let { date ->
                    calendar.time = date
                }
            }
        } catch (_: Exception) {
        }

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                // Format the selected date
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(selectedDate.time)

                // Update the EditText
                binding.birthdayEditText.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.maxDate = System.currentTimeMillis()

        val minDate = Calendar.getInstance()
        minDate.add(Calendar.YEAR, -100)
        datePicker.datePicker.minDate = minDate.timeInMillis

        datePicker.show()
    }

    private fun prepareProfileUpdates(): HashMap<String, Any> {
        val newEmail = binding.emailEditText.text.toString()
        isEmailChanged = newEmail != originalEmail

        return hashMapOf(
            "email" to newEmail,
            "username" to binding.usernameEditText.text.toString(),
            "firstName" to binding.firstNameEditText.text.toString(),
            "lastName" to binding.lastNameEditText.text.toString(),
            "birthday" to binding.birthdayEditText.text.toString(),
            "gender" to getSelectedGender(),
            "address" to binding.addressEditText.text.toString(),
            "city" to binding.cityEditText.text.toString(),
            "state" to binding.stateEditText.text.toString(),
            "zipCode" to binding.zipCodeEditText.text.toString(),
            "phone1" to binding.phone1EditText.text.toString(),
            "phone2" to binding.phone2EditText.text.toString(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun uploadImageAndSave(updates: HashMap<String, Any>) {
        selectedImageUri?.let { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri)
                inputStream?.let { stream ->
                    val storageRef = FirebaseStorage.getInstance().reference
                    val imageRef = storageRef.child("profile_images/${firestoreHelper.getCurrentUserId()}.jpg")

                    imageRef.putStream(stream)
                        .addOnSuccessListener { taskSnapshot ->
                            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                                updates["photoUri"] = downloadUri.toString()
                                saveProfileToFirestore(updates)
                            }
                        }
                        .addOnFailureListener { e ->
                            showToast(this, "Image upload failed: ${e.message}")
                            saveProfileToFirestore(updates)
                        }
                } ?: run {
                    showToast(this, "Could not open image file")
                    saveProfileToFirestore(updates)
                }
            } catch (e: Exception) {
                showToast(this, "Error: ${e.message}")
                saveProfileToFirestore(updates)
            }
        } ?: run {
            saveProfileToFirestore(updates)
        }
    }

    private fun saveProfileToFirestore(updates: HashMap<String, Any>) {
        firestoreHelper.getUserInfoDocumentField(firestoreHelper.getCurrentUserId().toString())
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                if (isEmailChanged) {
                    verifyPasswordForEmailUpdate(updates["email"] as String)
                } else {
                    completeSave("Profile updated successfully")
                }
            }
            .addOnFailureListener { e ->
                showToast(this, "Failed to save: ${e.message}")
            }
    }

    private fun verifyPasswordForEmailUpdate(newEmail: String) {
        val passwordField = EditText(this).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Current password"
        }

        AlertDialog.Builder(this)
            .setTitle("Verify Password")
            .setMessage("To update your email, please enter your current password")
            .setView(passwordField)
            .setPositiveButton("Confirm") { _, _ ->
                val password = passwordField.text.toString()
                if (password.isNotBlank()) {
                    reauthenticateAndUpdateEmail(newEmail, password)
                } else {
                    showToast(this, "Password cannot be empty")
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                completeSave("Profile updated (email not changed)")
            }
            .show()
    }

    private fun reauthenticateAndUpdateEmail(newEmail: String, password: String) {
        val credential = EmailAuthProvider.getCredential(originalEmail, password)
        currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    currentUser?.updateEmail(newEmail)
                        ?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                completeSave("Profile and email updated successfully")
                            } else {
                                showToast(this, "Failed to update email: ${updateTask.exception?.message}")
                            }
                        }
                } else {
                    showToast(this, "Authentication failed: ${task.exception?.message}")
                }
            }
    }
    // End Region

    // Region: Helper Methods
    private fun getSelectedGender(): String = when (binding.genderRadioGroup.checkedRadioButtonId) {
        R.id.maleRadioButton -> "Male"
        R.id.femaleRadioButton -> "Female"
        else -> "Not specified"
    }

    private fun launchMaps() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "geo:0,0?q=".toUri()))
        } catch (_: ActivityNotFoundException) {
            showToast(this, "No maps app found")
        }
    }

    private fun validateInputs(): Boolean {
        with(binding) {
            return when {
                !isValidEmail(emailEditText.text.toString()) -> {
                    emailEditText.error = "Invalid email format"
                    false
                }
                usernameEditText.text.isNullOrBlank() -> {
                    usernameEditText.error = "Username required"
                    false
                }
                else -> true
            }
        }
    }

    private fun completeSave(message: String) {
        showToast(this, message)
        setResult(RESULT_OK)
        finish()
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    // End Region
}