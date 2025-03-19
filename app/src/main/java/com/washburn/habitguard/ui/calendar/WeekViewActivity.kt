package com.washburn.habitguard.ui.calendar

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.washburn.habitguard.databinding.ActivityWeekViewBinding
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class WeekViewActivity : AppCompatActivity(), CalendarAdapter.OnItemListener {

    private lateinit var binding: ActivityWeekViewBinding // View Binding instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWeekViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initWidgets()
        setWeekView()
    }

    private fun initWidgets() {
        binding.calendarRecyclerView.layoutManager = GridLayoutManager(applicationContext, 7)
    }

    private fun setWeekView() {
        binding.monthYearTV.text = CalendarUtils.monthYearFromDate(CalendarUtils.selectedDate)

        val days = CalendarUtils.daysInWeekArray(CalendarUtils.selectedDate)
        val calendarAdapter = CalendarAdapter(days, this)
        binding.calendarRecyclerView.adapter = calendarAdapter

        // Update event list
        setEventAdapter()
    }

    fun previousWeekAction(view: View) {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate.minusWeeks(1)
        setWeekView()
    }

    fun nextWeekAction(view: View) {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate.plusWeeks(1)
        setWeekView()
    }

    override fun onItemClick(position: Int, date: LocalDate) {
        CalendarUtils.selectedDate = date
        setWeekView()
    }

    override fun onResume() {
        super.onResume()
        setEventAdapter()
    }

    private fun setEventAdapter() {
        val dailyEvents = Event.eventsForDate(CalendarUtils.selectedDate)
        val eventAdapter = EventAdapter(applicationContext, dailyEvents)
        binding.eventListView.adapter = eventAdapter
    }

    fun newEventAction(view: View) {
        startActivity(Intent(this, EventEditActivity::class.java))
    }

    fun dailyAction(view: View) {
        startActivity(Intent(this, DailyCalendarActivity::class.java))
    }
}