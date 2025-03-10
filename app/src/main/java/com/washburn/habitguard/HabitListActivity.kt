package com.washburn.habitguard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HabitListActivity : AppCompatActivity(), HabitAdapter.OnHabitActionListener {

    private val db = FirebaseFirestore.getInstance()
    private val habitList = mutableListOf<Habit>()
    private lateinit var habitAdapter: HabitAdapter
    private lateinit var selectedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_list)

        selectedDate = intent.getStringExtra("selectedDate") ?: run {
            Toast.makeText(this, "No date selected.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewHabits)
        recyclerView.layoutManager = LinearLayoutManager(this)
        habitAdapter = HabitAdapter(habitList, this)
        recyclerView.adapter = habitAdapter

        // Load habits from Firestore
        loadHabits()

        val fabAddHabit = findViewById<FloatingActionButton>(R.id.fabAddHabit)
        fabAddHabit.setOnClickListener {
            // Launch HabitEditActivity in add mode (edit mode = false)
            val intent = Intent(this, HabitEditActivity::class.java)
            intent.putExtra("selectedDate", selectedDate)
            intent.putExtra("isEditMode", false)
            startActivity(intent)
        }
    }

    private fun loadHabits() {
        db.collection("Habits").document(selectedDate).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    habitList.clear()  // clear previous data
                    val habitsData = document.get("habits") as? List<Map<String, Any>>
                    habitsData?.forEach { habitMap ->
                        val name = habitMap["name"] as? String ?: ""
                        val description = habitMap["description"] as? String ?: ""
                        habitList.add(Habit(name, description))
                    }
                    habitAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "No habits found for $selectedDate", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading habits: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Delete
    override fun onDelete(habit: Habit, position: Int) {
        val habitMap = hashMapOf(
            "name" to habit.name,
            "description" to habit.description
        )
        db.collection("Habits").document(selectedDate)
            .update("habits", FieldValue.arrayRemove(habitMap))
            .addOnSuccessListener {
                Toast.makeText(this, "Habit deleted", Toast.LENGTH_SHORT).show()
                habitList.removeAt(position)
                habitAdapter.notifyItemRemoved(position)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error deleting habit: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Edit
    override fun onEdit(habit: Habit, position: Int) {
        // Launch HabitEditActivity in edit mode.
        val intent = Intent(this, HabitEditActivity::class.java)
        intent.putExtra("selectedDate", selectedDate)
        intent.putExtra("isEditMode", true)
        intent.putExtra("oldName", habit.name)
        intent.putExtra("oldDescription", habit.description)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadHabits()
    }
}
