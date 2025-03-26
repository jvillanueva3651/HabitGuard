// FirestoreHelper.kt is where firebase firestore is being called to save and update user data

package com.washburn.habitguard

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirestoreHelper {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = Firebase.firestore

    // User authentication
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // User Data
    fun saveUserInfo(userId: String, email: String, username: String, photoUri: String?, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
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

    fun updateUserData(userId: String, updatedData: Map<String, Any>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
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

    fun addUserHabit(habitData: Map<String, Any>, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        getCurrentUserId()?.let { userId ->
            db.collection("HabitGuard")
                .document(userId)
                .collection("UserHabit")
                .add(habitData) // Firestore initializes the DocumentReference here
                .addOnSuccessListener { documentReference -> // This is the initialized reference
                    onSuccess(documentReference.id) // We just use the auto-generated ID
                }
                .addOnFailureListener { e -> onFailure(e) }
        } ?: onFailure(Exception("User not authenticated"))
    }

    fun updateUserHabit(habitId: String, updatedData: Map<String, Any>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        getCurrentUserId()?.let { userId ->
            db.collection("HabitGuard")
                .document(userId)
                .collection("UserHabit")
                .document(habitId)
                .update(updatedData)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(e) }
        } ?: onFailure(Exception("User not authenticated"))
    }

    fun deleteUserHabit(habitId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        getCurrentUserId()?.let { userId ->
            db.collection("HabitGuard")
                .document(userId)
                .collection("UserHabit")
                .document(habitId)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(e) }
        } ?: onFailure(Exception("User not authenticated"))
    }

    fun getUserHabit(habitId: String, onSuccess: (Map<String, Any>) -> Unit, onFailure: (Exception) -> Unit) {
        getCurrentUserId()?.let { userId ->
            db.collection("HabitGuard")
                .document(userId)
                .collection("UserHabit")
                .document(habitId)
                .get()
                .addOnSuccessListener { document ->
                    document.data?.let { data ->
                        onSuccess(data)
                    } ?: onFailure(Exception("Habit data is null"))
                }
                .addOnFailureListener { e -> onFailure(e) }
        } ?: onFailure(Exception("User not authenticated"))
    }

    fun getAllUserHabits(onSuccess: (List<Pair<String, Map<String, Any>>>) -> Unit, onFailure: (Exception) -> Unit) {
        getCurrentUserId()?.let { userId ->
            db.collection("HabitGuard")
                .document(userId)
                .collection("UserHabit")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val habits = querySnapshot.documents.mapNotNull { doc ->
                        doc.data?.let { Pair(doc.id, it) }
                    }
                    onSuccess(habits)
                }
                .addOnFailureListener { e -> onFailure(e) }
        } ?: onFailure(Exception("User not authenticated"))
    }
}