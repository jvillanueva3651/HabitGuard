package com.washburn.habitguard

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Map to store events (date -> event description)
    private val eventsMap = mutableMapOf<String, String>()
    private val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val calendar: CalendarView = findViewById(R.id.calendar)
        val eventListView: TextView = findViewById(R.id.event_list_view)
        val addEventButton: Button = findViewById(R.id.add_event_button)

        // Set up CalendarView listener
        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = formatDate(dayOfMonth, month, year)
            val eventDescription = eventsMap[selectedDate]

            if (eventDescription != null) {
                // Display the event description for the selected date
                eventListView.text = "Event: $eventDescription"
            } else {
                eventListView.text = "No event for this date"
            }
        }

        // Set up Add Event button
        addEventButton.setOnClickListener {
            val selectedDate = getSelectedDate(calendar)
            if (selectedDate.isNotEmpty()) {
                showEventInputDialog(selectedDate)
            } else {
                Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Show a dialog to input event description.
     */
    private fun showEventInputDialog(date: String) {
        val inputDialog = AlertDialog.Builder(this)
        inputDialog.setTitle("Add Event for $date")
        inputDialog.setMessage("Enter event description:")

        // Use EditText for user input
        val input = EditText(this)
        inputDialog.setView(input)

        inputDialog.setPositiveButton("Add") { _, _ ->
            val eventDescription = input.text.toString()
            if (eventDescription.isNotEmpty()) {
                // Save the event description for the selected date
                eventsMap[date] = eventDescription
                Toast.makeText(this, "Event added to $date", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Event description cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        inputDialog.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        inputDialog.show()
    }

    /**
     * Format the date as a string.
     */
    private fun formatDate(day: Int, month: Int, year: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        return dateFormatter.format(calendar.time)
    }

    /**
     * Get the selected date from the CalendarView.
     */
    private fun getSelectedDate(calendar: CalendarView): String {
        val dateMillis = calendar.date
        val calendarInstance = Calendar.getInstance()
        calendarInstance.timeInMillis = dateMillis
        return formatDate(
            calendarInstance.get(Calendar.DAY_OF_MONTH),
            calendarInstance.get(Calendar.MONTH),
            calendarInstance.get(Calendar.YEAR)
        )
    }
}