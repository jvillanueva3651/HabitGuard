package com.washburn.habitguard.ui.calendar

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
class EventEditActivity : AppCompatActivity() {

    private lateinit var eventNameET: EditText
    private lateinit var eventDateTV: TextView
    private lateinit var eventTimeTV: TextView

    private var time: LocalTime = LocalTime.now()
    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_edit)
        initWidgets()
        eventDateTV.text = "Date: ${CalendarUtils.formattedDate(CalendarUtils.selectedDate)}"
        eventTimeTV.text = "Time: ${CalendarUtils.formattedTime(time)}"

        firestoreHelper = FirestoreHelper()
    }

    private fun initWidgets() {
        eventNameET = findViewById(R.id.eventNameET)
        eventDateTV = findViewById(R.id.eventDateTV)
        eventTimeTV = findViewById(R.id.eventTimeTV)
    }

    fun saveEventAction(view: View) {
        val eventName = eventNameET.text.toString()
        val newEvent = Event(eventName, CalendarUtils.selectedDate, time)
        Event.eventsList.add(newEvent)
        finish()
    }
}