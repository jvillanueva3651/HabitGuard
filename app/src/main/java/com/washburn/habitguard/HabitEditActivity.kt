package com.washburn.habitguard

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HabitEditActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_edit)

        // Retrieve the selected date from the intent extras.
        val selectedDate = intent.getStringExtra("selectedDate")
        if (selectedDate == null) {
            Toast.makeText(this, "No date provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etHabitName = findViewById<EditText>(R.id.etHabitName)
        val etHabitDescription = findViewById<EditText>(R.id.etHabitDescription)
        val btnSaveHabit = findViewById<Button>(R.id.btnSaveHabit)

        btnSaveHabit.setOnClickListener {
            val name = etHabitName.text.toString().trim()
            val description = etHabitDescription.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a habit name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Build a habit data map.
            val habitData = hashMapOf(
                "name" to name,
                "description" to description
            )

            // Attempt to update the document by adding the new habit to the "habits" array.
            db.collection("Habits").document(selectedDate)
                .update("habits", FieldValue.arrayUnion(habitData))
                .addOnSuccessListener {
                    Toast.makeText(this, "Habit added!", Toast.LENGTH_SHORT).show()
                    finish()  // Return to the habit list
                }
                .addOnFailureListener { exception ->
                    // If the document does not exist yet, create it.
                    db.collection("Habits").document(selectedDate)
                        .set(hashMapOf("habits" to listOf(habitData)))
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
