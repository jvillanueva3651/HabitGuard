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

        val isEditMode = intent.getBooleanExtra("isEditMode", false)
        val etHabitName = findViewById<EditText>(R.id.etHabitName)
        val etHabitDescription = findViewById<EditText>(R.id.etHabitDescription)
        val btnSaveHabit = findViewById<Button>(R.id.btnSaveHabit)

        // If editing, prefill the fields with the existing habit data.
        var oldName = ""
        var oldDescription = ""
        if (isEditMode) {
            oldName = intent.getStringExtra("oldName") ?: ""
            oldDescription = intent.getStringExtra("oldDescription") ?: ""
            etHabitName.setText(oldName)
            etHabitDescription.setText(oldDescription)
        }

        btnSaveHabit.setOnClickListener {
            val newName = etHabitName.text.toString().trim()
            val newDescription = etHabitDescription.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Please enter a habit name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newHabitData = hashMapOf(
                "name" to newName,
                "description" to newDescription
            )

            if (isEditMode) {
                // In edit mode, remove the old habit then add the updated habit.
                val oldHabitData = hashMapOf(
                    "name" to oldName,
                    "description" to oldDescription
                )
                db.collection("Habits").document(selectedDate)
                    .update("habits", FieldValue.arrayRemove(oldHabitData))
                    .addOnSuccessListener {
                        // Now add the updated habit.
                        db.collection("Habits").document(selectedDate)
                            .update("habits", FieldValue.arrayUnion(newHabitData))
                            .addOnSuccessListener {
                                Toast.makeText(this, "Habit updated!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error updating habit: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error removing old habit: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // In add mode, try to update the document (or create it if it doesn't exist).
                db.collection("Habits").document(selectedDate)
                    .update("habits", FieldValue.arrayUnion(newHabitData))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Habit added!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        // If document doesn't exist, create it.
                        db.collection("Habits").document(selectedDate)
                            .set(hashMapOf("habits" to listOf(newHabitData)))
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
}
