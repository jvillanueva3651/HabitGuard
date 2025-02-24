package com.washburn.habitguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.washburn.habitguard.ui.theme.HabitGuardTheme
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var habitRecyclerView: RecyclerView

    private val calendarDates = mutableListOf<String>()
    private val habitsList = mutableListOf<Habit>()
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var habitAdapter: HabitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = Firebase.auth

        // Initialize RecyclerViews
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        habitRecyclerView = findViewById(R.id.habitRecyclerView)

        // Setup Calendar Grid (Monthly View)
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarAdapter = CalendarAdapter(calendarDates) { date ->
            handleDateClick(date)
        }
        calendarRecyclerView.adapter = calendarAdapter

        // Setup Habit List (Daily/Weekly View)
        habitRecyclerView.layoutManager = LinearLayoutManager(this)
        habitAdapter = HabitAdapter(habitsList) { habit ->
            handleHabitClick(habit)
        }
        habitRecyclerView.adapter = habitAdapter

        // Existing sign-out code
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }

        // Load data
        loadCalendarDates()  // Initialize calendar
        loadHabits()         // Load habits from Firestore
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Redirect to AuthActivity if not logged in
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }

    // Generate calendar dates for current month
    private fun loadCalendarDates() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        calendarDates.clear()
        repeat(maxDay) {
            calendarDates.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        calendarAdapter.notifyDataSetChanged()
    }

    // Fetch habits from Firestore
    private fun loadHabits() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("habits")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                habitsList.clear()
                snapshot?.documents?.forEach { doc ->
                    val habit = doc.toObject<Habit>()
                    habit?.let {
                        habitsList.add(it.copy(id = doc.id))
                    }
                }
                habitAdapter.notifyDataSetChanged()
                calendarAdapter.notifyDataSetChanged() // Update calendar completion marks
            }
    }

    // Handle date selection in calendar
    private fun handleDateClick(date: String) {
        // Get habits completed on this date
        val completedHabits = habitsList.filter { it.completedDates.contains(date) }

        // Show dialog or update UI
        showDateSummaryDialog(date, completedHabits)
    }

    // Handle habit checkbox toggle
    private fun handleHabitClick(habit: Habit) {
        val userId = auth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val newCompletedDates = if (habit.completedDates.contains(today)) {
            habit.completedDates - today
        } else {
            habit.completedDates + today
        }

        db.collection("users").document(userId).collection("habits")
            .document(habit.id)
            .update("completedDates", newCompletedDates)
    }
}

