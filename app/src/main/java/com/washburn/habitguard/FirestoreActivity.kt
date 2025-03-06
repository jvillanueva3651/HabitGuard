package com.washburn.habitguard

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreActivity {
    private val db = FirebaseFirestore.getInstance()

    fun updateUserData() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser: FirebaseUser? = firebaseAuth.currentUser

        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            val db = FirebaseFirestore.getInstance()

            // --- Update UserInfo collection ---
            val userInfoRef = db.collection("UserInfo").document(uid)
            val userInfoData = hashMapOf(
                "First Name" to "FirstName",
                "Last Name" to "LastName",
                "Birthday" to "BirthDay",
                "Gender" to "Gender",
                "Street Address" to "Address",
                "City" to "City",
                "State" to "State",
                "Zip Code" to "ZipCode",
                "Phone Number" to "Phone1",
                "Alternate Phone Number" to "Phone2"
                // Add other user info fields here...
            )

            userInfoRef.set(userInfoData, SetOptions.merge()) //Merge option to update only changed fields.
                .addOnSuccessListener {
                    println("UserInfo updated successfully for UID: $uid")
                }
                .addOnFailureListener { exception ->
                    println("Error updating UserInfo: ${exception.message}")
                }


            // --- Update UserCalendar collection ---
            val userCalendarRef = db.collection("UserCalendar").document(uid)
            val userCalendarData = hashMapOf(
                "Event" to "EventName",
                "Event Description" to "EventDesc",
                "Start Time" to "StartTime",
                "End Time" to "EndTime",
                "Recurring Event" to "Recurring"
                // Add other calendar fields here...
            )

            userCalendarRef.set(userCalendarData, SetOptions.merge()) //Merge option to update only changed fields.
                .addOnSuccessListener {
                    println("UserCalendar updated successfully for UID: $uid")
                }
                .addOnFailureListener { exception ->
                    println("Error updating UserCalendar: ${exception.message}")
                }
        } else {
            println("User not signed in.")
        }
    }



}



