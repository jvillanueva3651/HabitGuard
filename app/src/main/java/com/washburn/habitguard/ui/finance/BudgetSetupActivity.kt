package com.washburn.habitguard.ui.finance

import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.ActivityBudgetSetupBinding

class BudgetSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetSetupBinding
    private lateinit var firestoreHelper: FirestoreHelper
    private var selectedPeriod = "MONTHLY" // Default value

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreHelper = FirestoreHelper()
        setupPeriodSpinner()

        binding.saveButton.setOnClickListener {
            saveBudget()
        }
    }

    private fun setupPeriodSpinner() {
        val periodOptions = resources.getStringArray(R.array.period_options)
        val adapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_item,
            periodOptions
        )

        (binding.periodSpinner as? MaterialAutoCompleteTextView)?.apply {
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                selectedPeriod = periodOptions[position].uppercase()
            }

            // Set initial selection
            setText(periodOptions.first(), false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveBudget() {
        val amountInput = binding.amountInput.text.toString()

        if (amountInput.isBlank()) {
            binding.amountInput.error = "Please enter an amount"
            return
        }

        val amount = amountInput.toDoubleOrNull() ?: run {
            binding.amountInput.error = "Invalid amount"
            return
        }

        firestoreHelper.addUserBudget(
            amount = amount,
            period = selectedPeriod,
            onSuccess = {
                Toast.makeText(this, "Budget saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}