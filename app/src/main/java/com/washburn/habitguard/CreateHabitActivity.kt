package com.washburn.habitguard

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.washburn.habitguard.databinding.ActivityCreateHabitBinding

class CreateHabitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateHabitBinding
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateHabitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup frequency spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.habit_frequencies,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.frequencySpinner.adapter = adapter
        }

        binding.saveButton.setOnClickListener {
            val name = binding.habitNameEditText.text.toString().trim()
            val frequency = binding.frequencySpinner.selectedItem.toString()
            val targetDays = binding.targetDaysEditText.text.toString().toIntOrNull() ?: 1

            if (name.isEmpty()) {
                Toast.makeText(this, "Habit name required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveHabitToFirestore(
                Habit(
                    name = name,
                    type = frequency.lowercase(),
                    targetDays = targetDays
                )
            )
        }
    }

    private fun saveHabitToFirestore(habit: Habit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("habits")
            .add(habit)
            .addOnSuccessListener {
                Toast.makeText(this, "Habit created!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save habit", Toast.LENGTH_SHORT).show()
            }
    }
}