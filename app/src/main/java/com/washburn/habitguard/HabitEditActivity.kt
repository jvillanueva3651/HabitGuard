package com.washburn.habitguard

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HabitEditActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_edit)

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val selectedDate = intent.getStringExtra("selectedDate") ?: run {
            Toast.makeText(this, "No date provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val isEditMode = intent.getBooleanExtra("isEditMode", false)
        val etHabitName = findViewById<EditText>(R.id.etHabitName)
        val etHabitDescription = findViewById<EditText>(R.id.etHabitDescription)
        val btnSaveHabit = findViewById<Button>(R.id.btnSaveHabit)

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

            val habitRef = db.collection("HabitGuard")
                .document(userId)
                .collection("Habits")
                .document(selectedDate)

            val newHabitData = hashMapOf(
                "name" to newName,
                "description" to newDescription,
                "date" to selectedDate
            )

            if (isEditMode) {
                val oldHabitData = hashMapOf(
                    "name" to oldName,
                    "description" to oldDescription,
                    "date" to selectedDate
                )

                habitRef.update("habits", FieldValue.arrayRemove(oldHabitData))
                    .addOnSuccessListener {
                        habitRef.update("habits", FieldValue.arrayUnion(newHabitData))
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
                habitRef.update("habits", FieldValue.arrayUnion(newHabitData))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Habit added!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        habitRef.set(hashMapOf("habits" to listOf(newHabitData)))
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