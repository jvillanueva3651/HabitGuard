package com.washburn.habitguard.ui.gallery

import android.os.Build
import androidx.annotation.RequiresApi
import com.washburn.habitguard.FirestoreHelper
import java.time.LocalDate
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
class Event(
    var name: String,
    var date: LocalDate,
    var time: LocalTime
) {
    companion object {
        val eventsList = mutableListOf<Event>()

        fun eventsForDate(date: LocalDate): List<Event> {
            return eventsList.filter { it.date == date }
        }

        fun eventsForDateAndTime(date: LocalDate, time: LocalTime): List<Event> {
            return eventsList.filter { it.date == date && it.time.hour == time.hour }
        }

        fun saveToFirestore(
            event: Event,
            onSuccess: () -> Unit,
            onFailure: (Exception) -> Unit
        ) {
            val firestoreHelper = FirestoreHelper()
            firestoreHelper.updateCalendarEvent(
                eventName = event.name,
                eventDescription = "", // Add a description field if needed
                startTime = event.time.toString(),
                endTime = event.time.plusHours(1).toString(), // Adjust end time as needed
                isRecurring = false, // Set recurring flag as needed
                date = event.date.toString(),
                onSuccess = onSuccess,
                onFailure = onFailure
            )
            firestoreHelper.saveCalendarEvent(
                eventName = event.name,
                eventDescription = "",
                startTime = event.time.toString(),
                endTime = event.time.plusHours(1).toString(),
                isRecurring = false,
                date = event.date.toString(),
                onSuccess = onSuccess,
                onFailure = onFailure
            )
        }
    }
}