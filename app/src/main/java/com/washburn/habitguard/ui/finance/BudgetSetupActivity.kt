package com.washburn.habitguard.ui.finance

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.ActivityBudgetSetupBinding

class BudgetSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetSetupBinding
    private lateinit var firestoreHelper: FirestoreHelper

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreHelper = FirestoreHelper()

        binding.saveButton.setOnClickListener {
            saveBudget()
        }
    }

    private fun saveBudget() {
        val amountInput = binding.amountInput.text.toString()
        val period = binding.periodSpinner.selectedItemPosition

        if (amountInput.isBlank()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountInput.toDoubleOrNull() ?: run {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val periodType = when(period) {
            0 -> "DAILY"
            1 -> "WEEKLY"
            2 -> "MONTHLY"
            else -> "MONTHLY"
        }

        firestoreHelper.addUserBudget(
            amount = amount,
            period = periodType,
            onSuccess = {
                Toast.makeText(this, "Budget saved", Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { e ->
                Toast.makeText(this, "Error saving budget: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}