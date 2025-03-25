package com.washburn.habitguard.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.ActivityProfileEditBinding
import java.util.*

@Suppress("DEPRECATION")
class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var firestoreHelper: FirestoreHelper
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreHelper = FirestoreHelper()

        loadProfileData()

        // Handle Change Photo Button
        binding.changePhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
            Toast.makeText(this, "Change Photo clicked", Toast.LENGTH_SHORT).show()
        }

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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
            startActivity(intent)
        }

        // Handle Save Button
        binding.saveButton.setOnClickListener { saveProfileData() }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            uploadImageToFirebaseStorage(imageUri)
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri?) {
        if (imageUri == null) return

        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("profile_images/${userId}.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val updates = hashMapOf<String, Any>("photoUri" to uri.toString())
                    firestoreHelper.updateUserData(userId!!, updates,
                        onSuccess = {
                            Glide.with(this)
                                .load(uri.toString())
                                .circleCrop()
                                .into(binding.profilePhoto)
                            Toast.makeText(this, "Profile photo updated successfully", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { exception ->
                            Toast.makeText(this, "Failed to update profile photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfileData() {
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        firestoreHelper.getUserDocument(userId).addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val email = document.getString("email") ?: "-"
                val username = document.getString("username") ?: "-"
                val photoUri = document.getString("photoUri") ?: "-"
                val firstName = document.getString("firstName") ?: "-"
                val lastName = document.getString("lastName") ?: "-"
                val birthday = document.getString("birthday") ?: "-"
                val gender = document.getString("gender") ?: "-"
                val address = document.getString("address") ?: "-"
                val city = document.getString("city") ?: "-"
                val state = document.getString("state") ?: "-"
                val zipCode = document.getString("zipCode") ?: "-"
                val phone1 = document.getString("phone1") ?: "-"
                val phone2 = document.getString("phone2") ?: "-"

                binding.emailEditText.setText(email)
                binding.usernameEditText.setText(username)
                binding.firstNameEditText.setText(firstName)
                binding.lastNameEditText.setText(lastName)
                binding.birthdayCalendarView.date = parseDateToMillis(birthday) // Set CalendarView date
                binding.addressEditText.setText(address)
                binding.cityEditText.setText(city)
                binding.stateEditText.setText(state)
                binding.zipCodeEditText.setText(zipCode)
                binding.phone1EditText.setText(phone1)
                binding.phone2EditText.setText(phone2)

                // Set gender radio button
                when (gender) {
                    "Male" -> binding.maleRadioButton.isChecked = true
                    "Female" -> binding.femaleRadioButton.isChecked = true
                }

                // Load profile photo
                if (photoUri.isNotEmpty()) {
                    Glide.with(this)
                        .load(photoUri)
                        .circleCrop()
                        .into(binding.profilePhoto)
                } else {
                    binding.profilePhoto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                Toast.makeText(this, "No existing profile found. Start fresh!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to load profile: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileData() {
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedEmail = binding.emailEditText.text.toString()
        val updatedUsername = binding.usernameEditText.text.toString()
        val updatedFirstName = binding.firstNameEditText.text.toString()
        val updatedLastName = binding.lastNameEditText.text.toString()
        val updatedBirthday = formatDateFromMillis(binding.birthdayCalendarView.date)
        val updatedGender = when (binding.genderRadioGroup.checkedRadioButtonId) {
            R.id.maleRadioButton -> "Male"
            R.id.femaleRadioButton -> "Female"
            else -> "-"
        }
        val updatedAddress = binding.addressEditText.text.toString()
        val updatedCity = binding.cityEditText.text.toString()
        val updatedState = binding.stateEditText.text.toString()
        val updatedZipCode = binding.zipCodeEditText.text.toString()
        val updatedPhone1 = binding.phone1EditText.text.toString()
        val updatedPhone2 = binding.phone2EditText.text.toString()

        // Validate email format
        if (!isValidEmail(updatedEmail)) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf(
            "email" to updatedEmail,
            "username" to updatedUsername,
            "firstName" to updatedFirstName,
            "lastName" to updatedLastName,
            "birthday" to updatedBirthday,
            "gender" to updatedGender,
            "address" to updatedAddress,
            "city" to updatedCity,
            "state" to updatedState,
            "zipCode" to updatedZipCode,
            "phone1" to updatedPhone1,
            "phone2" to updatedPhone2,
            "createdAt" to FieldValue.serverTimestamp()
        )

        // Update Firestore document
        firestoreHelper.updateUserData(userId, updates,
            onSuccess = {
                // Reauthenticate the user before updating the email in Firebase Authentication
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null && user.email != updatedEmail) {
                    showPasswordDialog(user, updatedEmail)
                } else {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { exception ->
                Toast.makeText(this, "Failed to update profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        )

        startActivity(Intent(this, SettingActivity::class.java))
        finish()
    }

    private fun reauthenticateAndUpdateEmail(user: FirebaseUser, newEmail: String, password: String) {
        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                user.updateEmail(newEmail).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Toast.makeText(this, "Profile and email updated successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to update email: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Reauthentication failed: ${reauthTask.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPasswordDialog(user: FirebaseUser, newEmail: String) {
        val passwordEditText = EditText(this)
        passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Reauthentication Required")
            .setMessage("Please enter your current password to update your email.")
            .setView(passwordEditText)
            .setPositiveButton("Submit") { _, _ ->
                val password = passwordEditText.text.toString()
                if (password.isNotEmpty()) {
                    reauthenticateAndUpdateEmail(user, newEmail, password)
                } else {
                    Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
        return email.matches(emailRegex.toRegex())
    }

    private fun parseDateToMillis(date: String): Long {
        val calendar = Calendar.getInstance()
        val parts = date.split("-")
        if (parts.size == 3) {
            calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        return calendar.timeInMillis
    }

    private fun formatDateFromMillis(millis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
}