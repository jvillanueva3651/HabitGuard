/**===========================================================================================
 * EventEditActivity for creating/editing habits and transactions
 * REF    : USE_BY -> .MonthlyViewActivity, .WeeklyViewActivity, and .DailyViewActivity
 *                      .EventAdapter
 *          USING  -> ~/FirestoreHelper (database)
 *          LAYOUT -> layout/activity_event_edit.xml
 * Purpose: Handles creation and modification of calendar events and financial transactions
 * Features:
 *   1. Dual-mode operation (Event vs Transaction)
 *   2. Time pickers with validation
 *   3. Currency input formatting
 *   4. TODO: Recurrence options (partial implementation)
 *   5. Firestore integration for data persistence
 *   6. Form validation
============================================================================================*/
package com.washburn.habitguard.ui.calendar

import android.os.Bundle
import android.os.Build
import android.view.View
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import android.widget.NumberPicker
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.washburn.habitguard.R
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.databinding.ActivityEventEditBinding
import com.washburn.habitguard.ui.calendar.CalendarUtils.formattedDate
import com.washburn.habitguard.ui.calendar.CalendarUtils.formattedShortTime
import com.washburn.habitguard.ui.calendar.CalendarUtils.selectedDate
import com.washburn.habitguard.ui.finance.Transaction
import java.util.*
import java.time.LocalTime
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O) // Required for bunch of small calls, might be because of TODO: old syntax?
class EventEditActivity : AppCompatActivity() {

    // For referring to the habit ID
    companion object {
        const val EXTRA_HABIT_ID = "HABIT_ID"
    }

    private lateinit var binding: ActivityEventEditBinding

    private lateinit var firestoreHelper: FirestoreHelper

    // Time handling with sensible defaults
    private var startTime: LocalTime = LocalTime.now().withMinute(0)
    private var endTime: LocalTime = startTime.plusHours(1)

    // Transaction handling
    private var currentType = TransactionType.INCOME
    private var isTransactionMode = false
    private var currentHabitId: String? = null

    //Time handling for transactions
    private lateinit var transactionHourPicker: NumberPicker
    private lateinit var transactionMinutePicker: NumberPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentHabitId = intent.getStringExtra(EXTRA_HABIT_ID)
        firestoreHelper = FirestoreHelper()

        // Compose UI
        initializeComponents()
        // Load existing habit if editing
        loadExistingHabitIfEditing()
    }

    private fun initializeComponents() {
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() } // Back button
        binding.eventDateTV.text = getString(R.string.date_format, formattedDate(selectedDate)) // Set date text

        transactionHourPicker = binding.transactionHourPicker
        transactionMinutePicker = binding.transactionMinutePicker

        setupTimePickers() // Time Initializing
        setupTransactionTimePickers() // Transaction Time Initializing
        setupMoneyInput() // Transaction Initializing
        setupEventHandlers() // Event Handlers making sure Time and Transaction are in sync and valid

        updateUI() // Update UI based on mode
    }

    // Time Initializing
    private fun setupTimePickers() {
        val locale = Locale.getDefault()
        with(binding) {
            // Configure hour pickers (0-23)
            listOf(startHourPicker, endHourPicker).forEach { picker ->
                picker.minValue = 0
                picker.maxValue = 23
                picker.setFormatter { String.format(locale, "%02d", it) }
            }

            // Configure minute pickers (0-59 in 1-minute increments)
            listOf(startMinutePicker, endMinutePicker).forEach { picker ->
                picker.minValue = 0
                picker.maxValue = 59 // 0-59 in 1 minute steps
                picker.setFormatter { String.format(locale, "%02d", it) }
            }

            // Set initial values
            startHourPicker.value = startTime.hour
            startMinutePicker.value = startTime.minute
            endHourPicker.value = endTime.hour
            endMinutePicker.value = endTime.minute
        }
    }

    // Function to initialize transaction time pickers
    private fun setupTransactionTimePickers() {
        val locale = Locale.getDefault()
        transactionHourPicker.minValue = 0
        transactionHourPicker.maxValue = 23
        transactionHourPicker.setFormatter { String.format(locale, "%02d", it) }
        transactionMinutePicker.minValue = 0
        transactionMinutePicker.maxValue = 59
        transactionMinutePicker.setFormatter { String.format(locale, "%02d", it) }
    }

    // Transaction Initializing
    private fun setupMoneyInput() {
        binding.editTextDollars.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.editTextDollars.removeTextChangedListener(this)
                    formatCurrencyInput(s) // Format US based currency format
                    binding.editTextDollars.addTextChangedListener(this)
                    checkTransactionMode() // Check which transaction mode in updating UI
                }
            }
        })

        binding.editTextCents.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { checkTransactionMode() }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }
    // Format US based currency format
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
    // Determine either a transaction or just an event (no money)
    private fun checkTransactionMode() {
        isTransactionMode = hasMoneyInput()
        updateUI()
    }
    // Check if there is any money input
    private fun hasMoneyInput(): Boolean {
        return binding.editTextDollars.text?.isNotEmpty() == true ||
                binding.editTextCents.text?.isNotEmpty() == true
    }

    // Update UI based on mode
    private fun updateUI() {
        binding.apply {
            // Update visibility based on mode
            transactionType.visibility = if (isTransactionMode) View.VISIBLE else View.INVISIBLE
            //timeRecyclerView.visibility = if (isTransactionMode) View.INVISIBLE else View.VISIBLE
            timeRecyclerView.visibility = if (isTransactionMode) View.GONE else View.VISIBLE
            transactionTimeLayout.visibility = if (isTransactionMode) View.VISIBLE else View.GONE

            // Show/Hide tags/location based on mode
            tagsLayout.visibility = if (isTransactionMode) View.VISIBLE else View.GONE
            eventLocationET.hint = if (isTransactionMode) "Location (Optional)" else "Location"

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

    // Event Handlers making sure Time and Transaction are in sync and valid
    private fun setupEventHandlers() {
        binding.apply {
            // Time picker validation
            startHourPicker.setOnValueChangedListener { _, _, newVal ->
                startTime = LocalTime.of(newVal, startTime.minute)
                validateTimes()
            }
            startMinutePicker.setOnValueChangedListener { _, _, newVal ->
                startTime = LocalTime.of(startTime.hour, newVal)
                validateTimes()
            }
            endHourPicker.setOnValueChangedListener { _, _, newVal ->
                endTime = LocalTime.of(newVal, endTime.minute)
                validateTimes()
            }
            endMinutePicker.setOnValueChangedListener { _, _, newVal ->
                endTime = LocalTime.of(endTime.hour, newVal)
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
    // Validate time input
    private fun validateTimes(): Boolean {
        return if (!isTransactionMode && startTime.isAfter(endTime)) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }
    // Button to toggle between transaction and event mode
    private fun toggleTransactionMode() {
        isTransactionMode = !isTransactionMode
        updateUI()
    }
    // Button to cycle through transaction types
    private fun cycleTransactionType() {
        currentType = when (currentType) {
            TransactionType.INCOME -> TransactionType.EXPENSE
            TransactionType.EXPENSE -> TransactionType.CREDIT
            TransactionType.CREDIT -> TransactionType.INCOME
        }
        updateUI()
    }
    // Validate form input
    private fun validateForm(): Boolean {
        var isValid = true

        if (binding.eventNameET.text.isNullOrBlank()) {
            binding.eventNameET.error = "Please enter a title"
            isValid = false
        }

        if (binding.eventNameET.text.isNullOrBlank()) {
            binding.eventNameET.error = "Please enter a title"
            isValid = false
        }

        if (!validateTimes()) {
            isValid = false
        }

        return isValid
    }

    // Save or update habit to firestore
    private fun saveOrUpdateHabit() {
        val habitData = prepareHabitData()

        if (currentHabitId != null) {
            updateExistingHabit(habitData)
        } else {
            createNewHabit(habitData)
        }
    }

    // Prepare data for firestore
    private fun prepareHabitData(): Map<String, Any> {
        if(isTransactionMode) {
            return hashMapOf(
                "name" to binding.eventNameET.text.toString().trim(),
                "description" to binding.messageEditText.text.toString(),
                "tags" to binding.tagsEditText.text.toString().split(",").map { it.trim() },
                "location" to binding.eventLocationET.text.toString(),
                "time" to formattedShortTime(LocalTime.of(transactionHourPicker.value, transactionMinutePicker.value)),
                "amount" to getCurrencyValue(),
                "date" to selectedDate.toString(),
                "transactionType" to currentType.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "isRecurring" to binding.recurringCheckBox.isChecked
            ).apply {
                if (binding.recurringCheckBox.isChecked) {
                    putAll(prepareRecurrenceData())
                }
            }
        }
        else {
            return hashMapOf(
                "name" to binding.eventNameET.text.toString().trim(),
                "description" to binding.messageEditText.text.toString(),
                "location" to binding.eventLocationET.text.toString(),
                "date" to selectedDate.toString(),
                "startTime" to formattedShortTime(startTime),
                "endTime" to formattedShortTime(endTime),
                "createdAt" to FieldValue.serverTimestamp(),
                "isRecurring" to binding.recurringCheckBox.isChecked
            ).apply {
                if (binding.recurringCheckBox.isChecked) {
                    putAll(prepareRecurrenceData())
                }
            }
        }
    }
    // Prepare recurrence data for firestore
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
    } // TODO still looking how to implement

    // Create new habit in firestore
    private fun createNewHabit(habitData: Map<String, Any>) {
        if(isTransactionMode) {
            firestoreHelper.addUserTransaction(
                transactionData = habitData,
                onSuccess = { id ->
                    currentHabitId = id
                    showSuccess("Habit created successfully")
                },
                onFailure = { e ->
                    showError("Creation failed: ${e.message}")
                }
            )
        }
        else {
            firestoreHelper.addUserHabit(
                habitData = habitData,
                onSuccess = { id ->
                    currentHabitId = id
                    showSuccess("Habit created successfully")
                },
                onFailure = { e ->
                    showError("Creation failed: ${e.message}")
                }
            )
        }
    }

    // Update existing habit in firestore
    private fun updateExistingHabit(habitData: Map<String, Any>) {
        if(isTransactionMode) {
            firestoreHelper.updateUserTransaction(
                transactionId = currentHabitId!!,
                updatedData = habitData,
                onSuccess = { showSuccess("Habit updated successfully") },
                onFailure = { e ->
                    showError("Update failed: ${e.message}")
                }
            )
        }
        else {
            firestoreHelper.updateUserHabit(
                habitId = currentHabitId!!,
                updatedData = habitData,
                onSuccess = { showSuccess("Habit updated successfully") },
                onFailure = { e ->
                    showError("Update failed: ${e.message}")
                }
            )
        }
    }

    // Load existing habit if editing
    private fun loadExistingHabitIfEditing() {
        if(isTransactionMode) {
            currentHabitId?.let { transactionId ->
                firestoreHelper.getUserTransaction(
                    transactionId = transactionId,
                    onSuccess = { transactionData ->
                        populateForm(transactionData)
                    },
                    onFailure = { e ->
                        showError("Error loading habit: ${e.message}")
                    }
                )
            }
        }
        else {
            currentHabitId?.let { habitId ->
                firestoreHelper.getUserHabit(
                    habitId = habitId,
                    onSuccess = { habitData ->
                        populateForm(habitData)
                    },
                    onFailure = { e ->
                        showError("Error loading habit: ${e.message}")
                    }
                )
            }
        }
    }

    // Populate form with habit data
    private fun populateForm(habitData: Map<String, Any>) {
        binding.apply {
            // Common fields
            eventNameET.setText(habitData["name"].toString())
            messageEditText.setText(habitData["description"].toString())
            eventLocationET.setText(habitData["location"].toString())

            if (isTransactionMode) {
                // Loading amount
                val amount = habitData["amount"] as? Double ?: 0.0
                setCurrencyValue(amount)

                // Loading type
                val typeString = habitData["transactionType"] as? String ?: "EXPENSE"
                currentType = TransactionType.valueOf(typeString)

                // Loading tags
                val tags = habitData["tags"] as? List<*> ?: emptyList<String>()
                tagsEditText.setText(tags.joinToString(", "))

                //Loading transaction time pickers
                habitData["time"]?.toString()?.let {
                    val time = LocalTime.parse(it)
                    transactionHourPicker.value = time.hour
                    transactionMinutePicker.value = time.minute
                }
            }
            else {
                habitData["startTime"]?.toString()?.let {
                    startTime = LocalTime.parse(it)
                    startHourPicker.value = startTime.hour
                    startMinutePicker.value = startTime.minute
                }
                habitData["endTime"]?.toString()?.let {
                    endTime = LocalTime.parse(it)
                    endHourPicker.value = endTime.hour
                    endMinutePicker.value = endTime.minute
                }
            }

            updateUI()
        }
    }

    // Get currency value from input
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

    // Toast message
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}