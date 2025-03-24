package com.washburn.habitguard

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirestoreHelper {

    private val db: FirebaseFirestore = Firebase.firestore

    fun saveUserData(userId: String, email: String, username: String, photoUri: String?, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userData = hashMapOf(
            "email" to email,
            "username" to username,
            "photoUri" to photoUri,
            "createdAt" to FieldValue.serverTimestamp(),
            "habits" to hashMapOf(
                "daily" to emptyList<String>(),
                "weekly" to emptyList<String>(),
                "monthly" to emptyList<String>()
            )
        )

        db.collection("HabitGuard")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun updateUserData(
        userId: String,
        updatedData: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("HabitGuard")
            .document(userId)
            .update(updatedData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getUserDocument(userId: String) = db.collection("HabitGuard").document(userId).get()
}