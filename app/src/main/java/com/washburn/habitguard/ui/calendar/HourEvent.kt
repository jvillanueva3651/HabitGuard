package com.washburn.habitguard.ui.calendar

import java.time.LocalTime

class HourEvent(
    var time: LocalTime,
    var events: List<Event>
) {
    // No need for explicit getters and setters in Kotlin
    // Kotlin's data classes or regular classes with properties automatically provide them
}