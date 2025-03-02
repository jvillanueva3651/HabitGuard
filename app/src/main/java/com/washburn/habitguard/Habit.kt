package com.washburn.habitguard

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Habit(
    @DocumentId val id: String = "",
    val name: String = "",
    val type: String = "daily",
    val targetDays: Int = 1,
    val completedDates: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)