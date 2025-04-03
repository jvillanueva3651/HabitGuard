package com.washburn.habitguard.ui.home

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.washburn.habitguard.R
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.databinding.FragmentHomeBinding

@RequiresApi(Build.VERSION_CODES.O)
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestoreHelper = FirestoreHelper()

        updateProfile()

        updateHabitAnalysis()

        updateFinanceAnalysis()

        updateStreak()
    }

    private fun updateProfile() {
        val userId = firestoreHelper.getCurrentUserId() ?: run {
            return
        }

        firestoreHelper.getUserDocument(userId)
            .collection("UserInfo")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    updateUI(
                        document.getString("username"),
                        document.getString("email"),
                        document.getString("photoUri")
                    )
                } else {
                    fallbackToMainDocument(userId)
                }
            }
            .addOnFailureListener { e ->
                fallbackToMainDocument(userId)
            }
    }
    private fun fallbackToMainDocument(userId: String) {
        firestoreHelper.getUserDocument(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    updateUI(
                        document.getString("username"),
                        document.getString("email"),
                        document.getString("photoUri")
                    )
                }
            }
            .addOnFailureListener { e ->
                val error = "Error loading profile"
                binding.userNameTextView.text = error
            }
    }
    private fun updateUI(username: String?, email: String?, photoUri: String?) {
        binding.userNameTextView.text = username ?: "No username"
        binding.userEmailTextView.text = email ?: "No email"

        // Load profile image
        photoUri?.let { uri ->
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_launcher_foreground)
                .circleCrop()
                .into(binding.userProfileImageView)
        } ?: run {
            binding.userProfileImageView.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    private fun updateHabitAnalysis() {
        // TODO
    }

    private fun updateFinanceAnalysis() {
        // TODO
    }

    private fun updateStreak() {
        // TODO
    }
}