package com.washburn.habitguard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var habitRecyclerView: RecyclerView
    private lateinit var toggleGroup: MaterialButtonToggleGroup

    // Data for calendar view
    private val calendarDates = mutableListOf<String>()
    private var habitsMap: MutableMap<String, List<Habit>> = mutableMapOf()

    // Data for daily view
    private var dailyHabits: List<Habit> = listOf()
    private var selectedDateForDaily: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        habitRecyclerView = findViewById(R.id.habitRecyclerView)
        toggleGroup = findViewById(R.id.viewToggleGroup)

        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        habitRecyclerView.layoutManager = LinearLayoutManager(this)

        // Set initial view to monthly
        showMonthlyView()

        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.monthlyViewButton -> showMonthlyView()
                    R.id.dailyViewButton -> showDailyView(selectedDateForDaily)
                    // For weekly view, similar implementation can be added.
                    R.id.weeklyViewButton -> showDailyView(selectedDateForDaily) // Placeholder
                }
            }
        }

        // Floating action button to add a habit
        findViewById<FloatingActionButton>(R.id.fabAddHabit).setOnClickListener {
            val intent = Intent(this, CreateHabitActivity::class.java)
            // If in daily view, pass the selected date
            intent.putExtra("date", selectedDateForDaily)
            startActivity(intent)
        }

        // Sign out button
        findViewById<Button>(R.id.signOutButton).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        } else {
            // Reload data
            if (toggleGroup.checkedButtonId == R.id.monthlyViewButton) {
                loadMonthlyHabits()
            } else {
                loadDailyHabits(selectedDateForDaily)
            }
        }
    }

    private fun showMonthlyView() {
        calendarRecyclerView.visibility = View.VISIBLE
        habitRecyclerView.visibility = View.GONE
        loadCalendarDates()
        loadMonthlyHabits()
    }

    private fun showDailyView(date: String) {
        selectedDateForDaily = date
        calendarRecyclerView.visibility = View.GONE
        habitRecyclerView.visibility = View.VISIBLE
        loadDailyHabits(date)
    }

    private fun loadCalendarDates() {
        calendarDates.clear()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 1..maxDay) {
            calendar.set(Calendar.DAY_OF_MONTH, i)
            calendarDates.add(dateFormat.format(calendar.time))
        }
    }

    private fun loadMonthlyHabits() {
        habitsMap.clear()
        // Get first and last date of the month
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val firstDate = dateFormat.format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val lastDate = dateFormat.format(calendar.time)

        db.collection("Habits")
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), firstDate)
            .whereLessThanOrEqualTo(FieldPath.documentId(), lastDate)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val date = doc.id
                    val habits = doc.get("habits") as? List<Map<String, Any>>
                    val habitList = habits?.map { habitMap ->
                        Habit(
                            id = habitMap["id"] as? String ?: "",
                            name = habitMap["name"] as? String ?: "",
                            description = habitMap["description"] as? String ?: "",
                            startTime = habitMap["startTime"] as? String ?: "",
                            endTime = habitMap["endTime"] as? String ?: ""
                        )
                    } ?: listOf()
                    habitsMap[date] = habitList
                }
                val adapter = CalendarAdapter(calendarDates, habitsMap) { date ->
                    showDailyView(date)
                }
                calendarRecyclerView.adapter = adapter
            }
    }

    private fun loadDailyHabits(date: String) {
        db.collection("Habits").document(date)
            .get()
            .addOnSuccessListener { document ->
                val habits = document.get("habits") as? List<Map<String, Any>>
                dailyHabits = habits?.map { habitMap ->
                    Habit(
                        id = habitMap["id"] as? String ?: "",
                        name = habitMap["name"] as? String ?: "",
                        description = habitMap["description"] as? String ?: "",
                        startTime = habitMap["startTime"] as? String ?: "",
                        endTime = habitMap["endTime"] as? String ?: ""
                    )
                } ?: listOf()
                val adapter = HabitAdapter(dailyHabits, date,
                    onEdit = { habit ->
                        val intent = Intent(this, EditHabitActivity::class.java)
                        intent.putExtra("date", date)
                        intent.putExtra("habitId", habit.id)
                        intent.putExtra("name", habit.name)
                        intent.putExtra("description", habit.description)
                        intent.putExtra("startTime", habit.startTime)
                        intent.putExtra("endTime", habit.endTime)
                        startActivity(intent)
                    },
                    onDelete = { habit ->
                        val habitMap = mapOf(
                            "id" to habit.id,
                            "name" to habit.name,
                            "description" to habit.description,
                            "startTime" to habit.startTime,
                            "endTime" to habit.endTime
                        )
                        db.collection("Habits").document(date)
                            .update("habits", FieldValue.arrayRemove(habitMap))
                            .addOnSuccessListener {
                                loadDailyHabits(date)
                            }
                    }
                )
                habitRecyclerView.adapter = adapter
            }
    }
}
