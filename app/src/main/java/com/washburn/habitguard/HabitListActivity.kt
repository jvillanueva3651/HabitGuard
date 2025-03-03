package com.washburn.habitguard

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class HabitListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val habitList = mutableListOf<Habit>()
    private lateinit var habitAdapter: HabitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_list)

        // Retrieve the selected date from the intent extras.
        val selectedDate = intent.getStringExtra("selectedDate")
        if (selectedDate == null) {
            Toast.makeText(this, "No date selected.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewHabits)
        recyclerView.layoutManager = LinearLayoutManager(this)
        habitAdapter = HabitAdapter(habitList)
        recyclerView.adapter = habitAdapter

        // Query Firestore for habits on the selected date.
        db.collection("Habits").document(selectedDate).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Expecting an array field named "habits"
                    val habitsData = document.get("habits") as? List<Map<String, Any>>
                    habitsData?.forEach { habitMap ->
                        val description = habitMap["description"] as? String ?: ""
                        val name = habitMap["name"] as? String ?: ""
                        habitList.add(Habit(name, description))
                    }
                    habitAdapter.notifyDataSetChanged()
                } else {
                    // No habits exist yet for this date.
                    Toast.makeText(this, "No habits found for $selectedDate", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading habits: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

        val fabAddHabit = findViewById<FloatingActionButton>(R.id.fabAddHabit)
        fabAddHabit.setOnClickListener {
            // Launch HabitEditActivity to add a new habit.
            val intent = Intent(this, HabitEditActivity::class.java)
            intent.putExtra("selectedDate", selectedDate)
            startActivity(intent)
        }
    }
}
