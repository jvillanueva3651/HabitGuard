// FirestoreHelper.kt is where firebase firestore is being called to save and update user data

package com.washburn.habitguard

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
class FirestoreHelper {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val HABIT_GUARD_COLLECTION = "HabitGuard"
        private const val USER_HABIT_SUBCOLLECTION = "UserHabit"
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getUserDocument(userId: String): DocumentReference {
        return db.collection(HABIT_GUARD_COLLECTION).document(userId)
    }

    fun saveUserInfo(
        email: String,
        username: String,
        photoUri: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userInfo = hashMapOf(
            "email" to email,
            "username" to username,
            "photoUri" to photoUri,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection(HABIT_GUARD_COLLECTION)
            .document(getCurrentUserId().toString())
            .collection("UserInfo")
            .add(userInfo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }

        val userLogin = hashMapOf(
            "email" to email,
            "lastLogin" to FieldValue.serverTimestamp()
        )

        db.collection(HABIT_GUARD_COLLECTION)
            .document(getCurrentUserId().toString())
            .set(userLogin)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun updateLastLogin(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(HABIT_GUARD_COLLECTION)
            .document(getCurrentUserId().toString())
            .update("lastLogin", FieldValue.serverTimestamp())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error -> onFailure(error) }
    }

    fun saveUserProfile(
        userId: String,
        profileData: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val dataWithTimestamp = profileData.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }

        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .set(dataWithTimestamp, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun addUserHabit(
        habitData: Map<String, Any>,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }

        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_HABIT_SUBCOLLECTION)
            .add(habitData)
            .addOnSuccessListener { documentReference ->
                // Update the document with its own ID
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener { onSuccess(documentReference.id) }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun updateUserHabit(
        habitId: String,
        updatedData: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }

        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_HABIT_SUBCOLLECTION)
            .document(habitId)
            .update(updatedData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun deleteUserHabit(
        habitId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }

        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_HABIT_SUBCOLLECTION)
            .document(habitId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getUserHabit(
        habitId: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }

        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_HABIT_SUBCOLLECTION)
            .document(habitId)
            .get()
            .addOnSuccessListener { document ->
                document.data?.let { data ->
                    onSuccess(data)
                } ?: onFailure(Exception("Habit data is null"))
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getAllUserHabits(
        onSuccess: (List<Pair<String, Map<String, Any>>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }

        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_HABIT_SUBCOLLECTION)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val habits = querySnapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Pair(doc.id, it) }
                }
                onSuccess(habits)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getHourlyEvents(
        date: LocalDate,
        onSuccess: (List<HourlyEventData>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getAllUserHabits(
            onSuccess = { habits ->
                val hourlyEvents = (0..23).map { hour ->
                    val timeSlot = LocalTime.of(hour, 0)
                    val events = habits.filter { (_, data) ->
                        val eventDate = data["date"] as? String ?: ""
                        val startTime = data["startTime"] as? String ?: "00:00"
                        eventDate == date.toString() && LocalTime.parse(startTime).hour == hour
                    }.map { (id, data) ->
                        EventData(
                            id = id,
                            name = data["name"] as? String ?: "No Title",
                            time = data["startTime"] as? String ?: "00:00",
                            description = data["description"] as? String ?: ""
                        )
                    }
                    HourlyEventData(timeSlot, events)
                }
                onSuccess(hourlyEvents)
            },
            onFailure = onFailure
        )
    }

    data class EventData(
        val id: String,
        val name: String,
        val time: String,
        val description: String
    )

    data class HourlyEventData(
        val timeSlot: LocalTime,
        val events: List<EventData>
    )
}