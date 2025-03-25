package com.washburn.habitguard.ui.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
class Event(
    var eventName: String,
    var eventDesc: String,
    var date: LocalDate,
    var time: LocalTime,
    var amount: Double = 0.0,
    var transactionType: TransactionType = TransactionType.EXPENSE
) {
    companion object {
        private val _events = mutableListOf<Event>()
        val eventsList: List<Event> get() = _events.toList()

        fun addEvent(event: Event) = _events.add(event)

        fun eventsForDate(date: LocalDate) = _events.filter { it.date == date }

        fun eventsForDateAndTime(date: LocalDate, time: LocalTime) =
            _events.filter { it.date == date && it.time.hour == time.hour }
    }
}