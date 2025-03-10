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
import java.util.UUID

class CreateHabitActivity : AppCompatActivity() {

    private lateinit var habitNameEditText: EditText
    private lateinit var habitDescriptionEditText: EditText
    private lateinit var habitStartTimeEditText: EditText
    private lateinit var habitEndTimeEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var saveButton: Button

    private val db = Firebase.firestore

    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_habit)

        habitNameEditText = findViewById(R.id.habitNameEditText)
        habitDescriptionEditText = findViewById(R.id.habitDescriptionEditText)
        habitStartTimeEditText = findViewById(R.id.habitStartTimeEditText)
        habitEndTimeEditText = findViewById(R.id.habitEndTimeEditText)
        dateButton = findViewById(R.id.dateButton)
        saveButton = findViewById(R.id.saveButton)

        dateButton.text = selectedDate

        dateButton.setOnClickListener {
            showDatePicker()
        }

        saveButton.setOnClickListener {
            saveHabit()
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

    private fun saveHabit() {
        val name = habitNameEditText.text.toString().trim()
        val description = habitDescriptionEditText.text.toString().trim()
        val startTime = habitStartTimeEditText.text.toString().trim()
        val endTime = habitEndTimeEditText.text.toString().trim()

        if (name.isEmpty() || description.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new Habit with a unique ID
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            startTime = startTime,
            endTime = endTime
        )

        // Convert habit to a map
        val habitMap = mapOf(
            "id" to habit.id,
            "name" to habit.name,
            "description" to habit.description,
            "startTime" to habit.startTime,
            "endTime" to habit.endTime
        )

        // Save habit to Firestore under "Habits" collection, document with id = selectedDate
        val habitDocRef = db.collection("Habits").document(selectedDate)
        habitDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                habitDocRef.update("habits", FieldValue.arrayUnion(habitMap))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Habit added!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error adding habit: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Create new document with habits array
                habitDocRef.set(mapOf("habits" to listOf(habitMap)))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Habit added!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error adding habit: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
