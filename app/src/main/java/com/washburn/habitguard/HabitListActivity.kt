package com.washburn.habitguard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HabitListActivity : AppCompatActivity(), HabitAdapter.OnHabitActionListener {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val habitList = mutableListOf<Habit>()
    private lateinit var habitAdapter: HabitAdapter
    private lateinit var selectedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_list)

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        selectedDate = intent.getStringExtra("selectedDate") ?: run {
            Toast.makeText(this, "No date selected.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewHabits)
        recyclerView.layoutManager = LinearLayoutManager(this)
        habitAdapter = HabitAdapter(habitList, this)
        recyclerView.adapter = habitAdapter

        loadHabits(userId)

        val fabAddHabit = findViewById<FloatingActionButton>(R.id.fabAddHabit)
        fabAddHabit.setOnClickListener {
            val intent = Intent(this, HabitEditActivity::class.java)
            intent.putExtra("selectedDate", selectedDate)
            intent.putExtra("isEditMode", false)
            startActivity(intent)
        }
    }

    private fun loadHabits(userId: String) {
        db.collection("HabitGuard")
            .document(userId)
            .collection("Habits")
            .document(selectedDate)
            .get()
            .addOnSuccessListener { document ->
                habitList.clear()
                if (document.exists()) {
                    val habitsData = document.get("habits") as? List<Map<String, Any>>
                    habitsData?.forEach { habitMap ->
                        habitList.add(Habit(
                            name = habitMap["name"] as? String ?: "",
                            description = habitMap["description"] as? String ?: "",
                            date = habitMap["date"] as? String ?: ""
                        ))
                    }
                }
                habitAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading habits: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDelete(habit: Habit, position: Int) {
        val userId = auth.currentUser?.uid ?: return

        val habitMap = hashMapOf(
            "name" to habit.name,
            "description" to habit.description,
            "date" to habit.date
        )

        db.collection("HabitGuard")
            .document(userId)
            .collection("Habits")
            .document(habit.date)
            .update("habits", FieldValue.arrayRemove(habitMap))
            .addOnSuccessListener {
                habitList.removeAt(position)
                habitAdapter.notifyItemRemoved(position)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error deleting habit: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onEdit(habit: Habit, position: Int) {
        val intent = Intent(this, HabitEditActivity::class.java)
        intent.putExtra("selectedDate", selectedDate)
        intent.putExtra("isEditMode", true)
        intent.putExtra("oldName", habit.name)
        intent.putExtra("oldDescription", habit.description)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        auth.currentUser?.uid?.let { loadHabits(it) }
    }
}