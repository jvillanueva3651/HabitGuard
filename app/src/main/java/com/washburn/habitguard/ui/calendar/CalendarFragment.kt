package com.washburn.habitguard.ui.calendar

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import java.time.LocalDate
import java.util.ArrayList
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.washburn.habitguard.databinding.FragmentCalendarBinding
import com.washburn.habitguard.ui.calendar.CalendarUtils.daysInMonthArray
import com.washburn.habitguard.ui.calendar.CalendarUtils.monthYearFromDate
import com.washburn.habitguard.ui.calendar.CalendarAdapter.OnItemListener
import com.washburn.habitguard.ui.calendar.CalendarUtils.selectedDate

@RequiresApi(Build.VERSION_CODES.O)
class CalendarFragment : Fragment(), OnItemListener {

    private lateinit var binding: FragmentCalendarBinding
    private lateinit var calendarAdapter: CalendarAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedDate = LocalDate.now()
        setMonthView()

        binding.previousMonthButton.setOnClickListener { previousMonthAction() }
        binding.nextMonthButton.setOnClickListener { nextMonthAction() }
        binding.weeklyActionButton.setOnClickListener { weeklyAction() }
    }

    private fun setMonthView() {
        binding.monthYearTV.text = monthYearFromDate(selectedDate)
        val daysInMonth: ArrayList<LocalDate> = daysInMonthArray()

        calendarAdapter = CalendarAdapter(daysInMonth, this)
        val layoutManager = GridLayoutManager(requireContext(), 7)
        binding.calendarRecyclerView.layoutManager = layoutManager
        binding.calendarRecyclerView.adapter = calendarAdapter
    }

    private fun previousMonthAction() {
        selectedDate = selectedDate.minusMonths(1)
        setMonthView()
    }

    private fun nextMonthAction() {
        selectedDate = selectedDate.plusMonths(1)
        setMonthView()
    }

    override fun onItemClick(position: Int, date: LocalDate) {
        selectedDate = date
        setMonthView()
    }

    private fun weeklyAction() {
        startActivity(Intent(requireContext(), WeekViewActivity::class.java))
    }
}