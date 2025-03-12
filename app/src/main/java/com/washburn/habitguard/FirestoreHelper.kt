package com.washburn.habitguard

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Update user information in Firestore
    fun updateUserInfo(
        firstName: String,
        lastName: String,
        birthday: String,
        gender: String,
        streetAddress: String,
        city: String,
        state: String,
        zipCode: String,
        phoneNumber: String,
        alternatePhoneNumber: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            val userInfoData = hashMapOf(
                "First Name" to firstName,
                "Last Name" to lastName,
                "Birthday" to birthday,
                "Gender" to gender,
                "Street Address" to streetAddress,
                "City" to city,
                "State" to state,
                "Zip Code" to zipCode,
                "Phone Number" to phoneNumber,
                "Alternate Phone Number" to alternatePhoneNumber
            )

            db.collection("UserInfo").document(uid)
                .set(userInfoData, SetOptions.merge())
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        } else {
            onFailure(Exception("User not signed in."))
        }
    }

    // Update calendar event in Firestore
    fun updateCalendarEvent(
        eventName: String,
        eventDescription: String,
        startTime: String,
        endTime: String,
        isRecurring: Boolean,
        date: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            val userCalendarData = hashMapOf(
                "Event" to eventName,
                "Event Description" to eventDescription,
                "Start Time" to startTime,
                "End Time" to endTime,
                "Recurring Event" to isRecurring,
                "Date" to date
            )

            db.collection("UserCalendar").document(uid)
                .set(userCalendarData, SetOptions.merge())
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        } else {
            onFailure(Exception("User not signed in."))
        }
    }

    fun saveCalendarEvent(
        eventName: String,
        eventDescription: String,
        startTime: String,
        endTime: String,
        isRecurring: Boolean,
        date: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            val eventData = hashMapOf(
                "Event" to eventName,
                "Event Description" to eventDescription,
                "Start Time" to startTime,
                "End Time" to endTime,
                "Recurring Event" to isRecurring,
                "Date" to date
            )

            // Add a new document with a generated ID
            db.collection("UserCalendar").document(uid).collection("Events")
                .add(eventData)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        } else {
            onFailure(Exception("User not signed in."))
        }
    }
}