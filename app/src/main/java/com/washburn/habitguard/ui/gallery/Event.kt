package com.washburn.habitguard.ui.gallery

import android.os.Build
import androidx.annotation.RequiresApi
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
    }
}