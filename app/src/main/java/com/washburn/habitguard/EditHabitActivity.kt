package com.washburn.habitguard

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class EditHabitActivity : AppCompatActivity() {

    private lateinit var habitNameEditText: EditText
    private lateinit var habitDescriptionEditText: EditText
    private lateinit var habitStartTimeEditText: EditText
    private lateinit var habitEndTimeEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var updateButton: Button
    private lateinit var deleteButton: Button

    private val db = Firebase.firestore

    private var selectedDate: String = ""
    private var originalHabitMap: Map<String, Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_habit)

        habitNameEditText = findViewById(R.id.habitNameEditText)
        habitDescriptionEditText = findViewById(R.id.habitDescriptionEditText)
        habitStartTimeEditText = findViewById(R.id.habitStartTimeEditText)
        habitEndTimeEditText = findViewById(R.id.habitEndTimeEditText)
        dateButton = findViewById(R.id.dateButton)
        updateButton = findViewById(R.id.updateButton)
        deleteButton = findViewById(R.id.deleteButton)

        // Get data from intent
        selectedDate = intent.getStringExtra("date") ?: ""
        val habitId = intent.getStringExtra("habitId") ?: ""
        val name = intent.getStringExtra("name") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val startTime = intent.getStringExtra("startTime") ?: ""
        val endTime = intent.getStringExtra("endTime") ?: ""

        dateButton.text = selectedDate
        habitNameEditText.setText(name)
        habitDescriptionEditText.setText(description)
        habitStartTimeEditText.setText(startTime)
        habitEndTimeEditText.setText(endTime)

        // Save the original habit map for removal later
        originalHabitMap = mapOf(
            "id" to habitId,
            "name" to name,
            "description" to description,
            "startTime" to startTime,
            "endTime" to endTime
        )

        dateButton.setOnClickListener {
            showDatePicker()
        }

        updateButton.setOnClickListener {
            updateHabit(habitId)
        }

        deleteButton.setOnClickListener {
            deleteHabit()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                dateButton.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateHabit(habitId: String) {
        val name = habitNameEditText.text.toString().trim()
        val description = habitDescriptionEditText.text.toString().trim()
        val startTime = habitStartTimeEditText.text.toString().trim()
        val endTime = habitEndTimeEditText.text.toString().trim()

        if (name.isEmpty() || description.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        // Create updated habit map
        val updatedHabitMap = mapOf(
            "id" to habitId,
            "name" to name,
            "description" to description,
            "startTime" to startTime,
            "endTime" to endTime
        )

        // Update Firestore: remove the old habit and add the updated one.
        val habitDocRef = db.collection("Habits").document(selectedDate)
        habitDocRef.update("habits", FieldValue.arrayRemove(originalHabitMap))
            .addOnSuccessListener {
                habitDocRef.update("habits", FieldValue.arrayUnion(updatedHabitMap))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Habit updated!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating habit: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating habit: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteHabit() {
        if (originalHabitMap == null) return

        val habitDocRef = db.collection("Habits").document(selectedDate)
        habitDocRef.update("habits", FieldValue.arrayRemove(originalHabitMap))
            .addOnSuccessListener {
                Toast.makeText(this, "Habit deleted!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting habit: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
