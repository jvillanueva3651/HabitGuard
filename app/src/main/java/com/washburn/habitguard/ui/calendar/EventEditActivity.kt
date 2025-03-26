package com.washburn.habitguard.ui.calendar

import android.os.Bundle
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FieldValue
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.ActivityEventEditBinding
import com.washburn.habitguard.ui.calendar.CalendarUtils.formattedShortTime
import java.text.NumberFormat
import java.time.LocalTime
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
class EventEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventEditBinding
    private lateinit var firestoreHelper: FirestoreHelper

    // Time handling with sensible defaults
    private var startTime: LocalTime = LocalTime.now().withMinute(0) // Current hour, on the hour
    private var endTime: LocalTime = startTime.plusHours(1) // 1 hour after start

    // Transaction handling
    private var currentType = TransactionType.INCOME
    private var isTransactionMode = false
    private var currentHabitId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupEventHandlers()
        loadExistingHabitIfEditing()
    }

    private fun initializeComponents() {
        firestoreHelper = FirestoreHelper()
        currentHabitId = intent.getStringExtra("HABIT_ID")

        val dateText  = "Date: ${CalendarUtils.formattedDate(CalendarUtils.selectedDate)}"
        binding.eventDateTV.text = dateText

        setupTimePickers()
        setupMoneyInput()
        updateUI()
    }

    private fun setupTimePickers() {
        val locale = Locale.getDefault()
        with(binding) {
            // Configure hour pickers (0-23)
            listOf(startHourPicker, endHourPicker).forEach { picker ->
                picker.minValue = 0
                picker.maxValue = 23
                picker.setFormatter { String.format(locale, "%02d", it) }
            }

            // Configure minute pickers (0-55 in 5-minute increments)
            listOf(startMinutePicker, endMinutePicker).forEach { picker ->
                picker.minValue = 0
                picker.maxValue = 11 // 0-55 in 5 minute steps
                picker.displayedValues = (0..11).map { (it * 5).toString().padStart(2, '0') }.toTypedArray()
            }

            // Set initial values
            startHourPicker.value = startTime.hour
            startMinutePicker.value = startTime.minute / 5
            endHourPicker.value = endTime.hour
            endMinutePicker.value = endTime.minute / 5
        }
    }

    private fun setupMoneyInput() {
        binding.editTextDollars.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.editTextDollars.removeTextChangedListener(this)
                    formatCurrencyInput(s)
                    binding.editTextDollars.addTextChangedListener(this)
                    checkTransactionMode()
                }
            }
        })

        binding.editTextCents.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { checkTransactionMode() }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun formatCurrencyInput(s: Editable?) {
        val cleanString = s.toString().replace(",".toRegex(), "")
        if (cleanString.isNotEmpty()) {
            try {
                val parsed = cleanString.toDouble()
                val formatted = NumberFormat.getNumberInstance(Locale.US).format(parsed)
                binding.editTextDollars.setText(formatted)
                binding.editTextDollars.setSelection(formatted.length)
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
    }

    private fun setupEventHandlers() {
        binding.apply {
            // Time picker listeners with validation
            startHourPicker.setOnValueChangedListener { _, _, newVal ->
                startTime = LocalTime.of(newVal, startTime.minute)
                validateTimes()
            }
            startMinutePicker.setOnValueChangedListener { _, _, newVal ->
                startTime = LocalTime.of(startTime.hour, newVal * 5)
                validateTimes()
            }
            endHourPicker.setOnValueChangedListener { _, _, newVal ->
                endTime = LocalTime.of(newVal, endTime.minute)
                validateTimes()
            }
            endMinutePicker.setOnValueChangedListener { _, _, newVal ->
                endTime = LocalTime.of(endTime.hour, newVal * 5)
                validateTimes()
            }

            // Mode toggle buttons
            btnToggleTransaction.setOnClickListener { toggleTransactionMode() }
            btnToggleIncome.setOnClickListener { if (isTransactionMode) cycleTransactionType() }
            eventButton.setOnClickListener { if (isTransactionMode) toggleTransactionMode() }
            transactionButton.setOnClickListener { if (!isTransactionMode) toggleTransactionMode() }

            recurringCheckBox.setOnCheckedChangeListener { _, isChecked ->
                recurrenceOptionsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            }



            // Save button with validation
            saveEventAction.setOnClickListener {
                if (validateForm()) saveOrUpdateHabit()
            }
        }
    }

    private fun validateTimes(): Boolean {
        return if (startTime.isAfter(endTime)) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (binding.eventNameET.text.isNullOrBlank()) {
            binding.eventNameET.error = "Please enter a title"
            isValid = false
        }

        if (!validateTimes()) {
            isValid = false
        }

        return isValid
    }

    private fun loadExistingHabitIfEditing() {
        currentHabitId?.let { habitId ->
            firestoreHelper.getUserHabit(
                habitId = habitId,
                onSuccess = { habitData -> populateForm(habitData) },
                onFailure = { e -> showError("Error loading habit: ${e.message}") }
            )
        }
    }

    private fun populateForm(habitData: Map<String, Any>) {
        binding.apply {
            // Basic fields
            eventNameET.setText(habitData["name"].toString())
            messageEditText.setText(habitData["description"].toString())
            eventLocationET.setText(habitData["location"].toString())

            // Time fields
            habitData["startTime"]?.toString()?.let {
                startTime = LocalTime.parse(it)
                startHourPicker.value = startTime.hour
                startMinutePicker.value = startTime.minute / 5
            }
            habitData["endTime"]?.toString()?.let {
                endTime = LocalTime.parse(it)
                endHourPicker.value = endTime.hour
                endMinutePicker.value = endTime.minute / 5
            }

            // Transaction data
            val amount = habitData["amount"] as? Double ?: 0.0
            if (amount != 0.0) {
                setCurrencyValue(abs(amount))
                val type = habitData["transactionType"] as? String ?: "EXPENSE"
                currentType = TransactionType.valueOf(type)
                isTransactionMode = true
            }

            updateUI()
        }
    }

    private fun checkTransactionMode() {
        isTransactionMode = hasMoneyInput()
        updateUI()
    }

    private fun hasMoneyInput(): Boolean {
        return binding.editTextDollars.text?.isNotEmpty() == true ||
                binding.editTextCents.text?.isNotEmpty() == true
    }

    private fun toggleTransactionMode() {
        isTransactionMode = !isTransactionMode
        updateUI()
    }

    private fun cycleTransactionType() {
        currentType = when (currentType) {
            TransactionType.INCOME -> TransactionType.EXPENSE
            TransactionType.EXPENSE -> TransactionType.CREDIT
            TransactionType.CREDIT -> TransactionType.INCOME
        }
        updateUI()
    }

    private fun updateUI() {
        binding.apply {
            // Update visibility based on mode
            transactionType.visibility = if (isTransactionMode) View.VISIBLE else View.INVISIBLE
            timeRecyclerView.visibility = if (isTransactionMode) View.INVISIBLE else View.VISIBLE

            // Update button states
            btnToggleTransaction.setImageResource(
                if (isTransactionMode) R.drawable.ic_money else R.drawable.ic_event
            )
            transactionButton.isEnabled = !isTransactionMode
            transactionButton.alpha = if (isTransactionMode) 0.5f else 1f
            eventButton.isEnabled = isTransactionMode
            eventButton.alpha = if (isTransactionMode) 1f else 0.5f

            // Update transaction type UI
            btnToggleIncome.setImageResource(currentType.iconRes)
            val color = ContextCompat.getColor(this@EventEditActivity, currentType.colorRes)
            btnToggleIncome.setColorFilter(color)
            currency.setTextColor(color)
            editTextDollars.setTextColor(color)
            editTextDecimalSeparator.setTextColor(color)
            editTextCents.setTextColor(color)
            editTextDollars.hint = when (currentType) {
                TransactionType.INCOME -> "Amount received"
                TransactionType.EXPENSE -> "Amount spent"
                TransactionType.CREDIT -> "Credit amount"
            }
        }
    }

    private fun saveOrUpdateHabit() {
        val habitData = prepareHabitData()

        if (currentHabitId != null) {
            updateExistingHabit(habitData)
        } else {
            createNewHabit(habitData)
        }
    }

    private fun prepareHabitData(): Map<String, Any> {
        return hashMapOf(
            "name" to binding.eventNameET.text.toString().trim(),
            "description" to binding.messageEditText.text.toString(),
            "location" to binding.eventLocationET.text.toString(),
            "date" to CalendarUtils.selectedDate.toString(),
            "startTime" to formattedShortTime(startTime),
            "endTime" to formattedShortTime(endTime),
            "amount" to getCurrencyValue(),
            "transactionType" to currentType.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "isRecurring" to binding.recurringCheckBox.isChecked,
            "isTransaction" to isTransactionMode
        ).apply {
            if (binding.recurringCheckBox.isChecked) {
                putAll(prepareRecurrenceData())
            }
        }
    }

    private fun getCurrencyValue(): Double {
        val dollars = binding.editTextDollars.text?.toString()
            ?.replace(",", "")
            ?.toDoubleOrNull() ?: 0.0

        val cents = binding.editTextCents.text?.toString()
            ?.toDoubleOrNull()
            ?.div(100) ?: 0.0

        val amount = dollars + cents
        return when (currentType) {
            TransactionType.INCOME -> amount
            TransactionType.EXPENSE -> -amount
            TransactionType.CREDIT -> amount
        }
    }

    private fun setCurrencyValue(amount: Double) {
        val absoluteAmount = abs(amount)
        val dollars = absoluteAmount.toInt()
        val cents = ((absoluteAmount - dollars) * 100).roundToInt()

        val textDollar = dollars.toString()
        binding.editTextDollars.setText(textDollar)
        val textCent = "%02d".format(cents)
        binding.editTextCents.setText(textCent)
    }

    private fun updateExistingHabit(habitData: Map<String, Any>) {
        firestoreHelper.updateUserHabit(
            habitId = currentHabitId!!,
            updatedData = habitData,
            onSuccess = { showSuccess("Habit updated successfully") },
            onFailure = { e -> showError("Update failed: ${e.message}") }
        )
    }

    private fun createNewHabit(habitData: Map<String, Any>) {
        firestoreHelper.addUserHabit(
            habitData = habitData,
            onSuccess = { showSuccess("Habit created successfully") },
            onFailure = { e -> showError("Creation failed: ${e.message}") }
        )
    }

    private fun prepareRecurrenceData(): Map<String, Any> {
        return hashMapOf(
            "recurrence" to hashMapOf(
                "interval" to (binding.recurrenceInterval.text?.toString()?.toIntOrNull() ?: 1),
                "frequency" to (binding.recurrenceFrequency.selectedItem as? String ?: "DAILY"),
                "endOption" to when {
                    binding.endNever.isChecked -> "NEVER"
                    binding.endAfterOccurrences.isChecked -> "AFTER_OCCURRENCES"
                    binding.endOnDate.isChecked -> "ON_DATE"
                    else -> "NEVER"
                },
                "occurrencesCount" to binding.occurrencesCount.text?.toString()?.toIntOrNull(),
                "endDate" to if (binding.endOnDate.isChecked) {
                    "${binding.recurrenceEndDate.year}-" +
                            "${(binding.recurrenceEndDate.month + 1).toString().padStart(2, '0')}-" +
                            binding.recurrenceEndDate.dayOfMonth.toString().padStart(2, '0')
                } else null
            )
        )
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}