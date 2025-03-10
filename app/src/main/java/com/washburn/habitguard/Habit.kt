package com.washburn.habitguard

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Habit(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val startTime: String = "",
    val endTime: String = ""
)
