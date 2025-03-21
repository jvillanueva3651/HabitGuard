package com.washburn.habitguard.ui.calendar

import java.time.LocalDate;
import java.util.ArrayList;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import com.washburn.habitguard.databinding.ActivityWeekViewBinding
import com.washburn.habitguard.ui.calendar.CalendarAdapter.OnItemListener
import com.washburn.habitguard.ui.calendar.CalendarUtils.daysInMonthArray
import com.washburn.habitguard.ui.calendar.CalendarUtils.daysInWeekArray
import com.washburn.habitguard.ui.calendar.CalendarUtils.monthYearFromDate
import com.washburn.habitguard.ui.calendar.CalendarUtils.selectedDate


@RequiresApi(Build.VERSION_CODES.O)
class WeekViewActivity : AppCompatActivity(), OnItemListener {

    private lateinit var binding: ActivityWeekViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeekViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setWeekView()
        binding.previousWeekButton.setOnClickListener { previousWeekAction() }
        binding.nextWeekButton.setOnClickListener { nextWeekAction() }
        binding.dailyActionButton.setOnClickListener { dailyAction() }
        binding.newEventButton.setOnClickListener { newEventAction() }
    }

    private fun setWeekView() {
        binding.monthYearTV.text = monthYearFromDate(selectedDate)
        val days = daysInWeekArray(selectedDate)

        val calendarAdapter = CalendarAdapter(days, this)
        binding.calendarRecyclerView.layoutManager = GridLayoutManager(applicationContext, 7)
        binding.calendarRecyclerView.adapter = calendarAdapter

        setEventAdapter()
    }

    private fun previousWeekAction() {
        selectedDate = selectedDate.minusWeeks(1)
        setWeekView()
    }

    private fun nextWeekAction() {
        selectedDate = selectedDate.plusWeeks(1)
        setWeekView()
    }

    override fun onItemClick(position: Int, date: LocalDate) {
        selectedDate = date
        setWeekView()
    }

    override fun onResume() {
        super.onResume()
        setEventAdapter()
    }

    private fun setEventAdapter() {
        val dailyEvents = Event.eventsForDate(selectedDate)
        val eventAdapter = EventAdapter(applicationContext, dailyEvents)
        binding.eventListView.adapter = eventAdapter
    }

    private fun newEventAction() {
        startActivity(Intent(this, EventEditActivity::class.java))
    }

    private fun dailyAction() {
        startActivity(Intent(this, DailyCalendarActivity::class.java))
    }
}