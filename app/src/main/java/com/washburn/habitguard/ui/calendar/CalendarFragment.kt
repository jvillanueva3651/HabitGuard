package com.washburn.habitguard.ui.calendar

import android.annotation.SuppressLint
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
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.databinding.FragmentCalendarBinding
import com.washburn.habitguard.ui.calendar.CalendarUtils.daysInMonthArray
import com.washburn.habitguard.ui.calendar.CalendarUtils.monthYearFromDate
import com.washburn.habitguard.ui.calendar.CalendarAdapter.OnItemListener
import com.washburn.habitguard.ui.calendar.CalendarUtils.selectedDate

@SuppressLint("NotifyDataSetChanged")
@RequiresApi(Build.VERSION_CODES.O)
class CalendarFragment : Fragment(), OnItemListener {

    private lateinit var binding: FragmentCalendarBinding
    private lateinit var calendarAdapter: CalendarAdapter

    private lateinit var firestoreHelper: FirestoreHelper
    private var allEvents: List<Pair<String, Map<String, Any>>> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestoreHelper = FirestoreHelper()
        selectedDate = LocalDate.now() // Initialize selectedDate

        setupViews()
        loadEvents()
    }

    private fun setupViews() {
        binding.apply {
            monthYearTV.text = monthYearFromDate(selectedDate)

            previousMonthButton.setOnClickListener { previousMonthAction() }
            nextMonthButton.setOnClickListener { nextMonthAction() }
            weeklyActionButton.setOnClickListener { weeklyAction() }
            addDotButton.setOnClickListener { newEventAction() }

            calendarRecyclerView.layoutManager = GridLayoutManager(requireContext(), 7)
            calendarRecyclerView.setHasFixedSize(true)
            calendarRecyclerView.isNestedScrollingEnabled = false
        }
    }

    private fun loadEvents() {
        firestoreHelper.getAllUserHabits(
            onSuccess = { habits ->
                allEvents = habits
                setMonthView()
            },
            onFailure = { e ->
                // Handle error (e.g., show toast)
            }
        )
    }

    private fun setMonthView() {
        val daysInMonth = ArrayList(daysInMonthArray())

        // Convert events to CalendarAdapter format
        val calendarEvents = allEvents.map { (_, eventData) ->
            mapOf(
                "name" to (eventData["name"] as? String ?: ""),
                "date" to (eventData["date"] as? String ?: ""),
                "time" to (eventData["startTime"] as? String ?: "00:00")
            )
        }

        calendarAdapter = CalendarAdapter(
            daysInMonth,
            this,
            calendarEvents
        )

        binding.apply {
            monthYearTV.text = monthYearFromDate(selectedDate)
            calendarRecyclerView.adapter = calendarAdapter
        }
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

    private fun newEventAction() {
        startActivity(Intent(requireContext(), EventEditActivity::class.java))
    }

    private fun weeklyAction() {
        startActivity(Intent(requireContext(), WeekViewActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        loadEvents() // Refresh data when returning from other screens
    }
}