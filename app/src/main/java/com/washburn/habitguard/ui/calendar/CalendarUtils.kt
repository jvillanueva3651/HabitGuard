package com.washburn.habitguard.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
object CalendarUtils {
    lateinit var selectedDate: LocalDate

    fun formattedTimeFromString(timeStr: String): String {
        return try {
            val time = LocalTime.parse(timeStr)
            DateTimeFormatter.ofPattern("h:mm a").format(time)
        } catch (_: Exception) {
            timeStr
        }
    }

    fun formattedDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
        return date.format(formatter)
    }

    fun formattedShortTime(time: LocalTime): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return time.format(formatter)
    }

    fun monthYearFromDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        return date.format(formatter)
    }

    fun monthDayFromDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM d")
        return date.format(formatter)
    }

    fun daysInMonthArray(): ArrayList<LocalDate> {
        val daysInMonthArray = ArrayList<LocalDate>()
        val yearMonth = YearMonth.from(selectedDate)
        val daysInMonth = yearMonth.lengthOfMonth()

        val firstOfMonth = selectedDate.withDayOfMonth(1)
        val lastOfMonth = selectedDate.withDayOfMonth(daysInMonth)

        // Add leading days from previous month (only if part of the same week)
        if (firstOfMonth.dayOfWeek != DayOfWeek.SUNDAY) {  // Adjust based on your week start (e.g., SUNDAY)
            // Start from the last day of the previous month and go backward
            var day = firstOfMonth.minusDays(1)
            while (day.dayOfWeek != DayOfWeek.SATURDAY) {  // Adjust to your week's start day
                daysInMonthArray.add(0, day)  // Insert at beginning
                day = day.minusDays(1)
            }
        }

        // Add all days of the current month
        for (day in 1..daysInMonth) {
            daysInMonthArray.add(LocalDate.of(selectedDate.year, selectedDate.month, day))
        }

        // Add trailing days from next month (only if part of the same week)
        if (lastOfMonth.dayOfWeek != DayOfWeek.SATURDAY) {  // Adjust to your week's end day
            var day = lastOfMonth.plusDays(1)
            while (day.dayOfWeek != DayOfWeek.SUNDAY) {  // Stop at the start of the next week
                daysInMonthArray.add(day)
                day = day.plusDays(1)
            }
        }

        return daysInMonthArray
    }

    fun daysInWeekArray(selectedDate: LocalDate): ArrayList<LocalDate> {
        val days = ArrayList<LocalDate>()
        var current = sundayForDate(selectedDate)
        val endDate = current.plusWeeks(1)

        while (current.isBefore(endDate)) {
            days.add(current)
            current = current.plusDays(1)
        }
        return days
    }

    private fun sundayForDate(current: LocalDate): LocalDate {
        var currentDate = current
        val oneWeekAgo = current.minusWeeks(1)

        while (currentDate.isAfter(oneWeekAgo)) {
            if (currentDate.dayOfWeek == DayOfWeek.SUNDAY) {
                return currentDate
            }
            currentDate = currentDate.minusDays(1)
        }

        throw IllegalStateException("No Sunday found for the given date.")
    }
}
