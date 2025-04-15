// FirestoreHelper.kt is where firebase firestore is being called to save and update user data

package com.washburn.habitguard

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
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
        private const val USER_INFO_SUBCOLLECTION = "UserInfo"
        private const val USER_HABIT_SUBCOLLECTION = "UserHabit"
        private const val USER_TRANSACTIONS_SUBCOLLECTION = "UserTransactions"
        private const val USER_BUDGET_SUBCOLLECTION = "UserBudget"
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getUserInfoDocumentField(userId: String): DocumentReference {
        return db.collection(HABIT_GUARD_COLLECTION).document(userId).collection(USER_INFO_SUBCOLLECTION).document(userId)
    }
    fun getUserDocument(userId: String): DocumentReference {
        return db.collection(HABIT_GUARD_COLLECTION).document(userId)
    }

    /** ===========================================================================================
     * UserInfo Collection Functions
    =============================================================================================*/
    fun saveUserInfo(
        email: String,
        username: String,
        photoUri: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId().toString()
        val userRef = db.collection(HABIT_GUARD_COLLECTION).document(userId)

        val userInfo = mapOf(
            "email" to email,
            "username" to username,
            "photoUri" to photoUri,
            "createdAt" to FieldValue.serverTimestamp()
        )

        userRef.collection(USER_INFO_SUBCOLLECTION)
            .document(userId)
            .set(userInfo)
            .addOnSuccessListener { _ ->
                val userLogin = mapOf(
                    "email" to email,
                    "lastLogin" to FieldValue.serverTimestamp()
                )

                userRef.set(userLogin)
                    .addOnSuccessListener{ _ -> onSuccess()}
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
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

    /** ===========================================================================================
     * UserHabit Collection Functions
    =============================================================================================*/
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

    /** ===========================================================================================
     * UserTransaction Collection Functions
    =============================================================================================*/
    fun addUserTransaction(
        transactionData: Map<String, Any>,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }
        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_TRANSACTIONS_SUBCOLLECTION)
            .add(transactionData)
            .addOnSuccessListener { docRef ->
                docRef.update("id", docRef.id)
                onSuccess(docRef.id)
            }
            .addOnFailureListener(onFailure)
    }

    fun updateUserTransaction(
        transactionId: String,
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
            .collection(USER_TRANSACTIONS_SUBCOLLECTION)
            .document(transactionId)
            .update(updatedData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun deleteUserTransaction(
        transactionId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }
        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_TRANSACTIONS_SUBCOLLECTION)
            .document(transactionId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getUserTransaction(
        transactionId: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }
        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_TRANSACTIONS_SUBCOLLECTION)
            .document(transactionId)
            .get()
            .addOnSuccessListener { document ->
                document.data?.let { data ->
                    onSuccess(data)
                } ?: onFailure(Exception("Transaction data is null"))
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getAllUserTransactions(
        onSuccess: (List<Pair<String, Map<String, Any>>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }
        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_TRANSACTIONS_SUBCOLLECTION)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val transactions = querySnapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Pair(doc.id, it) }
                }
                onSuccess(transactions)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }


    /** ===========================================================================================
     * UserBudget Collection Functions
    =============================================================================================*/
    fun addUserBudget(
        amount: Double,
        period: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }

        val budgetData = hashMapOf(
            "amount" to amount,
            "period" to period,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_BUDGET_SUBCOLLECTION)
            .document()
            .set(budgetData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getUserBudget(
        onSuccess: (Pair<Double, String>?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }

        db.collection(HABIT_GUARD_COLLECTION)
            .document(userId)
            .collection(USER_BUDGET_SUBCOLLECTION)
            .document()
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val amount = document.getDouble("amount") ?: 0.0
                    val period = document.getString("period") ?: "MONTHLY"
                    onSuccess(Pair(amount, period))
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }


    /** ===========================================================================================
     * HourlyEvent Functions
    =============================================================================================*/
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