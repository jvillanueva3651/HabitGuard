package com.washburn.habitguard.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.washburn.habitguard.User

class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> get() = _userData

    fun fetchUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "Unknown"
                        val email = document.getString("email") ?: "No email"
                        val user = User(name, email)
                        _userData.value = user
                    } else {
                        _userData.value = null
                    }
                }
                .addOnFailureListener {
                    _userData.value = null
                }
        } else {
            _userData.value = null
        }
    }
}