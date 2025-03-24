package com.washburn.habitguard.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.ActivityProfileEditBinding

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

        binding.changePhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
            Toast.makeText(this, "Change Photo clicked", Toast.LENGTH_SHORT).show()
        }

        binding.saveButton.setOnClickListener { saveProfileData() }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
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
                binding.birthdayEditText.setText(birthday)
                binding.genderEditText.setText(gender)
                binding.addressEditText.setText(address)
                binding.cityEditText.setText(city)
                binding.stateEditText.setText(state)
                binding.zipCodeEditText.setText(zipCode)
                binding.phone1EditText.setText(phone1)
                binding.phone2EditText.setText(phone2)

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
        val updatedBirthday = binding.birthdayEditText.text.toString()
        val updatedGender = binding.genderEditText.text.toString()
        val updatedAddress = binding.addressEditText.text.toString()
        val updatedCity = binding.cityEditText.text.toString()
        val updatedState = binding.stateEditText.text.toString()
        val updatedZipCode = binding.zipCodeEditText.text.toString()
        val updatedPhone1 = binding.phone1EditText.text.toString()
        val updatedPhone2 = binding.phone2EditText.text.toString()

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

        // Check if the document exists
        firestoreHelper.getUserDocument(userId).addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                // Document exists, update it
                firestoreHelper.updateUserData(userId, updates,
                    onSuccess = {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { exception ->
                        Toast.makeText(this, "Failed to update profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Document doesn't exist, create it
                firestoreHelper.saveUserData(userId, updatedEmail, updatedUsername, null,
                    onSuccess = {
                        Toast.makeText(this, "Profile created successfully", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { exception ->
                        Toast.makeText(this, "Failed to create profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to check document: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }
}