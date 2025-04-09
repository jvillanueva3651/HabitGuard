/**===========================================================================================
 * ProfileEditActivity for handling profile-related tasks
 * REF    : USE_BY -> .SettingActivity
 * Purpose: Edit and see user information
 * Fun:  1. Handles camera permission requests to change icon,
 *       2. Alter user info,
 *       3. Validate user info.
============================================================================================*/
package com.washburn.habitguard.settings


import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import androidx.core.net.toUri
import com.washburn.habitguard.firebase.FirebaseAuthHelper

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.O)
class ProfileEditActivity : AppCompatActivity() {

    // Region: Properties and Initialization
    private lateinit var binding: ActivityProfileEditBinding
    private val firestoreHelper = FirestoreHelper()
    private val cameraHelper = CameraHelper(this)
    private var selectedImageUri: Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = uri
            loadImageIntoView(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCameraHelper()
        setupViews()
        loadProfileData()
    }
    // End Region

    // Region: View Setup and UI Configuration
    private fun setupViews() {
        setupPhotoButton()
        setupMapButton()
        setupSaveButton()
        setupBirthdayField()
        setupGenderSelection()
    }

    private fun setupPhotoButton() {
        binding.changePhotoButton.setOnClickListener { showPhotoSourceDialog() }
    }

    private fun setupMapButton() {
        binding.mapButton.setOnClickListener { launchMaps() }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener { attemptSave() }
    }

    private fun setupBirthdayField() {
        binding.birthdayEditText.inputType = InputType.TYPE_CLASS_NUMBER
        binding.birthdayEditText.addDateSeparator()
        binding.birthdayEditText.setOnClickListener { showDatePickerDialog() }
    }

    private fun setupGenderSelection() {
        binding.genderRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.maleRadioButton -> showToast(this, "Male selected")
                R.id.femaleRadioButton -> showToast(this, "Female selected")
            }
        }
    }

    private fun EditText.addDateSeparator() {
        this.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    val clean = s.toString().replace("[^\\d]".toRegex(), "")
                    val formatted = when (clean.length) {
                        in 1..2 -> clean
                        in 3..4 -> "${clean.substring(0, 2)}/${clean.substring(2)}"
                        in 5..8 -> "${clean.substring(0, 2)}/${clean.substring(2, 4)}/${clean.substring(4)}"
                        else -> current
                    }
                    current = formatted
                    this@addDateSeparator.setText(formatted)
                    this@addDateSeparator.setSelection(formatted.length)
                }
            }
        })
    }
    // End Region

    // Region: Profile Photo Handling
    private fun setupCameraHelper() {
        cameraHelper.onPhotoTaken = { uri ->
            selectedImageUri = uri
            loadImageIntoView(uri)
        }
        cameraHelper.onPermissionDenied = {
            showToast(this, "Camera permission denied")
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
    // End Region

    // Region: Profile Data Loading
    private fun loadProfileData() {
        firestoreHelper.getUserInfoDocumentField(firestoreHelper.getCurrentUserId().toString()).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    populateFields(document)
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
            emailEditText.isEnabled = false
            setTextSafely(emailEditText, document.getString("email"))
            setTextSafely(usernameEditText, document.getString("username"), "She sells seashells by the seashore")
            setTextSafely(firstNameEditText, document.getString("firstName"), "Chip")
            setTextSafely(lastNameEditText, document.getString("lastName"), "Munk")
            setBirthday(document.getString("birthday"))

            // Gender Selection
            when (document.getString("gender")) {
                "Male" -> maleRadioButton.isChecked = true
                "Female" -> femaleRadioButton.isChecked = true
            }

            // Address Info
            setTextSafely(addressEditText, document.getString("address"), "Sweet Home Alabama")
            setTextSafely(cityEditText, document.getString("city"), "Crawfordville")
            setTextSafely(stateEditText, document.getString("state"), "Georgia")
            setTextSafely(zipCodeEditText, document.getString("zipCode"), "30631")

            // Contact Info
            setTextSafely(phone1EditText, document.getString("phone1"))
            setTextSafely(phone2EditText, document.getString("phone2"))

            // Profile Photo
            document.getString("photoUri")?.takeIf { it.isNotBlank() }?.let { uri ->
                loadProfilePhoto(uri)
            }
        }
    }

    private fun setTextSafely(editText: EditText, text: String?, default: String = "") {
        editText.setText(text ?: default)
    }

    private fun setBirthday(dateStr: String?) {
        dateStr?.let {
            if (it.isNotEmpty()) {
                try {
                    val parts = it.split("-")
                    if (parts.size == 3) {
                        val birthDate = "${parts[1]}/${parts[2]}/${parts[0]}"
                        binding.birthdayEditText.setText(birthDate)
                    }
                } catch (_: Exception) {
                    binding.birthdayEditText.setText(dateStr)
                }
            }
        }
    }

    private fun loadProfilePhoto(uri: String) {
        Glide.with(this@ProfileEditActivity)
            .load(uri)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .circleCrop()
            .into(binding.profilePhoto)
    }
    // End Region

    // Region: Date Picker
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance().apply {
            tryParseCurrentDate(binding.birthdayEditText.text.toString())
        }

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val birthD = "%02d/%02d/%04d"
                binding.birthdayEditText.setText(birthD.format(month + 1, day, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            datePicker.minDate = Calendar.getInstance().apply { add(Calendar.YEAR, -100) }.timeInMillis
            show()
        }
    }

    private fun Calendar.tryParseCurrentDate(currentDate: String) {
        if (currentDate.isEmpty()) return

        try {
            val cleanDate = currentDate.replace("/", "")
            when {
                cleanDate.length <= 2 -> set(Calendar.MONTH, cleanDate.toInt() - 1)
                cleanDate.length <= 4 -> {
                    set(Calendar.MONTH, cleanDate.substring(0, 2).toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, cleanDate.substring(2).toInt())
                }
                else -> {
                    set(
                        cleanDate.substring(4).toInt(),
                        cleanDate.substring(0, 2).toInt() - 1,
                        cleanDate.substring(2, 4).toInt()
                    )
                }
            }
        } catch (_: Exception) {
        }
    }
    // End Region

    // Region: Data Saving and Validation
    private fun attemptSave() {
        if (!validateInputs()) return
        val updates = prepareProfileUpdates()

        if (selectedImageUri != null) {
            uploadImageAndSave(updates)
        } else {
            saveProfileToFirestore(updates)
        }
    }

    private fun validateInputs(): Boolean {
        with(binding) {
            return when {
                usernameEditText.text.isNullOrBlank() -> {
                    usernameEditText.error = "Username required"
                    false
                }
                else -> true
            }
        }
    }

    private fun prepareProfileUpdates(): HashMap<String, Any> {
        return hashMapOf(
            "email" to binding.emailEditText.text.toString(),
            "username" to binding.usernameEditText.text.toString(),
            "firstName" to binding.firstNameEditText.text.toString(),
            "lastName" to binding.lastNameEditText.text.toString(),
            "birthday" to formatBirthdayForStorage(binding.birthdayEditText.text.toString()),
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

    private fun formatBirthdayForStorage(birthdayText: String): String {
        if (birthdayText.isEmpty()) return ""

        return try {
            val cleanDate = birthdayText.replace("/", "")
            when {
                cleanDate.length <= 4 -> ""
                else -> "%04d-%02d-%02d".format(
                    cleanDate.substring(4).toInt(),
                    cleanDate.substring(0, 2).toInt(),
                    cleanDate.substring(2, 4).toInt()
                )
            }
        } catch (_: Exception) {
            ""
        }
    }
    // End Region

    // Region: Firebase Operations
    private fun uploadImageAndSave(updates: HashMap<String, Any>) {
        selectedImageUri?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.let { stream ->
                    FirebaseStorage.getInstance().reference
                        .child("profile_images/${firestoreHelper.getCurrentUserId()}.jpg")
                        .putStream(stream)
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
                showToast(this, "Profile updated successfully")
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                showToast(this, "Failed to save: ${e.message}")
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
}