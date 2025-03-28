package com.washburn.habitguard.ui.calendar

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.databinding.ActivityDailyCalendarBinding
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class DailyCalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyCalendarBinding
    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreHelper = FirestoreHelper()
        setupViews()
        loadDailyEvents()
    }

    private fun setupViews() {
        updateDateHeader()

        binding.apply {
            previousDayButton.setOnClickListener {
                CalendarUtils.selectedDate = CalendarUtils.selectedDate.minusDays(1)
                loadDailyEvents()
            }

            nextDayButton.setOnClickListener {
                CalendarUtils.selectedDate = CalendarUtils.selectedDate.plusDays(1)
                loadDailyEvents()
            }

            newEventButton.setOnClickListener {
                startActivity(Intent(this@DailyCalendarActivity, EventEditActivity::class.java))
            }
        }
    }

    private fun loadDailyEvents() {
        firestoreHelper.getHourlyEvents(
            date = CalendarUtils.selectedDate,
            onSuccess = { hourlyEvents ->
                binding.hourListView.adapter = HourAdapter(this@DailyCalendarActivity, hourlyEvents)
                updateDateHeader()
            },
            onFailure = { e ->
                Toast.makeText(this@DailyCalendarActivity,
                    "Error loading events: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateDateHeader() {
        binding.apply {
            monthDayText.text = CalendarUtils.monthDayFromDate(CalendarUtils.selectedDate)
            dayOfWeekTV.text = CalendarUtils.selectedDate.dayOfWeek
                .getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
    }

    override fun onResume() {
        super.onResume()
        loadDailyEvents()
    }
}