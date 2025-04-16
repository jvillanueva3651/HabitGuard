//=======================================================================================
// "WeekViewActivity" is the backend for displaying the weekly view calendar and the habit cards
// Refer to     "~/FirestoreHelper" for database operations,
//              "./ui/calendar/EventEditActivity" for creating habits and editing habits
//
// Get layout of activity from "layout/activity_week.view.xml"
//
// Fun: 1. Display weekly view calendar
//      2. Navigate between monthly view and daily view
//      3. Create habits
//      3. Display habit card(s)
//      5. Functions with EventAdapter
//=======================================================================================
package com.washburn.habitguard.ui.calendar

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.databinding.ActivityWeekViewBinding
import com.washburn.habitguard.ui.calendar.CalendarUtils.selectedDate
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class WeeklyViewActivity : AppCompatActivity(), CalendarAdapter.OnItemListener {

    private lateinit var binding: ActivityWeekViewBinding
    private lateinit var firestoreHelper: FirestoreHelper
    private var allEvents: List<Pair<String, Map<String, Any>>> = emptyList()
    private var allTransaction: List<Pair<String, Map<String, Any>>> = emptyList() // Store all transaction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeekViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreHelper = FirestoreHelper()
        initViews()
        loadEvents()
    }

    private fun initViews() {
        binding.apply {
            dailyActionButton.setOnClickListener { dailyActionAction() }
            previousWeekButton.setOnClickListener { previousWeekAction() }
            nextWeekButton.setOnClickListener { nextWeekAction() }
            newEventButton.setOnClickListener { newEventAction() }

            calendarRecyclerView.layoutManager = GridLayoutManager(this@WeeklyViewActivity, 7).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int = 1
                }
            }
            calendarRecyclerView.setHasFixedSize(true)
            calendarRecyclerView.isNestedScrollingEnabled = false

            // Setup events list
            eventListView.layoutManager = LinearLayoutManager(this@WeeklyViewActivity)
        }
    }

    private fun loadEvents() {
        firestoreHelper.getAllUserHabits(
            onSuccess = { habits ->
                allEvents = habits.sortedWith(compareBy(
                    { it.second["date"] as? String ?: "" },
                    { it.second["startTime"] as? String ?: "00:00" }
                ))
                setWeekView()
            },
            onFailure = { e ->
                Toast.makeText(this, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setWeekView() {
        binding.monthYearTV.text = CalendarUtils.monthYearFromDate(selectedDate)
        val days = CalendarUtils.daysInWeekArray(selectedDate)

        val calendarEvents = allEvents.map { (_, eventData) ->
            mapOf(
                "name" to (eventData["name"] as? String ?: ""),
                "date" to (eventData["date"] as? String ?: ""),
                "time" to (eventData["startTime"] as? String ?: "00:00")
            )
        }

        binding.calendarRecyclerView.adapter = CalendarAdapter(
            ArrayList(days),
            this,
            calendarEvents,
            allTransaction.map { (_, transactionData) ->
                mapOf(
                    "name" to (transactionData["name"] as? String ?: ""),
                    "date" to (transactionData["date"] as? String ?: ""),
                    "time" to (transactionData["time"] as? String ?: "00:00")
                )
            }
        )

        setEventAdapter()
    }

    private fun setEventAdapter() {
        val dailyEvents = allEvents
            .filter { (_, eventData) ->
                eventData["date"] == selectedDate.toString()
            }
            .sortedBy { (_, eventData) ->
                eventData["startTime"] as? String ?: "00:00"
            }

        binding.eventListView.adapter = EventAdapter(
            context = this,
            events = dailyEvents,
            firestoreHelper = firestoreHelper,
            onEditClick = { documentId ->
                startActivity(Intent(this, EventEditActivity::class.java).apply {
                    putExtra(EventEditActivity.EXTRA_HABIT_ID, documentId)
                })
            },
            onDeleteSuccess = { loadEvents() } // Auto-refresh after deletion
        )
    }

    override fun onItemClick(position: Int, date: LocalDate) {
        selectedDate = date
        setWeekView() // This will refresh both calendar and events list
    }

    override fun onResume() {
        super.onResume()
        loadEvents() // Refresh data when returning from other screens
    }

    private fun previousWeekAction() {
        selectedDate = selectedDate.minusWeeks(1)
        setWeekView()
    }

    private fun nextWeekAction() {
        selectedDate = selectedDate.plusWeeks(1)
        setWeekView()
    }

    private fun newEventAction() {
        val intent = Intent(this@WeeklyViewActivity, EventEditActivity::class.java).apply {

            putExtra("date", selectedDate.toString())
            putExtra("isTransaction", false)
        }
        startActivity(intent)
    }

    private fun dailyActionAction() {
        startActivity(Intent(this, DailyViewActivity::class.java))
    }
}