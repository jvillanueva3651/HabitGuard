@file:Suppress("KotlinConstantConditions")

package com.washburn.habitguard.ui.calendar

import android.annotation.SuppressLint
import java.time.LocalTime
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
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("SetTextI18n")
class EventEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventEditBinding
    private lateinit var firestoreHelper: FirestoreHelper
    // Time Handling - default to current time + 1 hour
    private var startTime: LocalTime = LocalTime.now()
    private var endTime: LocalTime = LocalTime.now().plusHours(1)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    // Transaction Handling
    private var currentType = TransactionType.INCOME
    private var isTransactionMode = false
    private var currentHabitId: String? = null

    // Formatting tools
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        isGroupingUsed = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEventEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreHelper = FirestoreHelper()
        currentHabitId = intent.getStringExtra("HABIT_ID")

        setupUI()
        setupClickListeners()

        currentHabitId?.let { loadHabitData(it) }
    }

    private fun setupUI() {
        binding.apply {
            eventDateTV.text = "Date: ${CalendarUtils.formattedDate(CalendarUtils.selectedDate)}"

            setupTimePickers()

            // Setup money input formatting
            editTextDollars.addTextChangedListener(createDollarTextWatcher())
            editTextDollars.setOnFocusChangeListener { _, hasFocus -> handleDollarFocusChange(hasFocus) }
            editTextCents.setOnFocusChangeListener { _, hasFocus -> handleCentsFocusChange(hasFocus) }

            updateTransactionUI()
            updateModeUI()
        }
    }

    private fun createDollarTextWatcher(): TextWatcher {
        return object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.editTextDollars.removeTextChangedListener(this)
                    val cleanString = s.toString().replace(",".toRegex(), "")
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = cleanString.toDouble()
                            val formatted = numberFormat.format(parsed)
                            current = formatted
                            binding.editTextDollars.setText(formatted)
                            binding.editTextDollars.setSelection(formatted.length)
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }
                    binding.editTextDollars.addTextChangedListener(this)
                }
            }
        }
    }

    private fun updateTransactionUI() {
        binding.apply {
            btnToggleIncome.setImageResource(currentType.iconRes)
            val color = ContextCompat.getColor(this@EventEditActivity, currentType.colorRes)

            btnToggleIncome.setColorFilter(color)
            currency.setTextColor(color)
            editTextDollars.setTextColor(color)
            editTextDecimalSeparator.setTextColor(color)
            editTextCents.setTextColor(color)

            val hint = when (currentType) {
                TransactionType.INCOME -> "Amount received"
                TransactionType.EXPENSE -> "Amount spent"
                TransactionType.CREDIT -> "Credit amount"
            }
            editTextDollars.hint = hint
        }
    }

    private fun updateModeUI() {
        binding.apply {
            // Show/hide appropriate sections based on transaction mode
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
        }
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
                picker.maxValue = 59 // 0-59 in 1 minute steps
                picker.displayedValues = (0..59).map { it .toString().padStart(2, '0') }.toTypedArray()
            }

            // Set initial values
            startHourPicker.value = startTime.hour
            startMinutePicker.value = startTime.minute
            endHourPicker.value = endTime.hour
            endMinutePicker.value = endTime.minute
        }
    }

    private fun loadHabitData(habitId: String) {
        firestoreHelper.getUserHabit(
            habitId = habitId,
            onSuccess = { habitData ->
                binding.apply {
                    // Load basic fields
                    eventNameET.setText(habitData["name"].toString())
                    messageEditText.setText(habitData["description"].toString())
                    eventLocationET.setText(habitData["location"].toString())

                    // Load times
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

                    // Load transaction data if exists
                    val amount = habitData["amount"] as? Double ?: 0.0
                    if (amount != 0.0) {
                        isTransactionMode = true
                        setCurrencyValue(abs(amount))
                        val type = habitData["transactionType"] as? String ?: "EXPENSE"
                        currentType = TransactionType.valueOf(type)
                    }

                    updateModeUI()
                    updateTransactionUI()
                }
            },
            onFailure = { e ->
                Toast.makeText(this, "Error loading habit: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun saveOrUpdateHabit() {
        val habitData = prepareHabitData()

        currentHabitId?.let { habitId ->
            // Update existing habit
            firestoreHelper.updateUserHabit(
                habitId = habitId,
                updatedData = habitData,
                onSuccess = { finish() },
                onFailure = { e ->
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } ?: run {
            // Create new habit
            firestoreHelper.addUserHabit(
                habitData = habitData,
                onSuccess = { finish() },
                onFailure = { e ->
                    Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun prepareHabitData(): Map<String, Any> {
        val hasAmount = binding.editTextDollars.text?.isNotEmpty() == true ||
                binding.editTextCents.text?.isNotEmpty() == true

        return hashMapOf(
            "name" to binding.eventNameET.text.toString().trim(),
            "description" to binding.messageEditText.text.toString(),
            "location" to binding.eventLocationET.text.toString(),
            "date" to CalendarUtils.selectedDate.toString(),
            "startTime" to startTime.format(timeFormatter),
            "endTime" to endTime.format(timeFormatter),
            "amount" to if (hasAmount) getCurrencyValue() else 0.0,
            "transactionType" to if (hasAmount) currentType.name else "EVENT",
            "createdAt" to FieldValue.serverTimestamp(),
            "isRecurring" to binding.recurringCheckBox.isChecked,
            "isTransaction" to hasAmount
        ).apply {
            if (binding.recurringCheckBox.isChecked) {
                putAll(prepareRecurrenceData())
            }
        }
    }

    private fun getCurrencyValue(): Double {
        binding.apply {
            val dollarsStr = editTextDollars.text?.toString()?.replace(",", "") ?: "0"
            val dollars = dollarsStr.toDoubleOrNull() ?: 0.0

            val centsStr = editTextCents.text?.toString() ?: "0"
            val cents = centsStr.toDoubleOrNull()?.div(100) ?: 0.0

            val positiveAmount = dollars + cents
            return when (currentType) {
                TransactionType.INCOME -> positiveAmount
                TransactionType.EXPENSE -> -positiveAmount
                TransactionType.CREDIT -> positiveAmount
            }
        }
    }

    private fun setCurrencyValue(amount: Double) {
        binding.apply {
            val absoluteAmount = abs(amount)
            val dollars = absoluteAmount.toInt()
            val cents = ((absoluteAmount - dollars) * 100).roundToInt()

            editTextDollars.setText(dollars.toString())
            editTextCents.setText("%02d".format(cents))
        }
    }


    private fun handleDollarFocusChange(hasFocus: Boolean) {
        if (!hasFocus && binding.editTextDollars.text?.isNotEmpty() == true) {
            val cleanValue = binding.editTextDollars.text.toString().replace(",", "")
            if (cleanValue.length >= 10) {
                binding.editTextCents.requestFocus()
            }
        }
    }

    private fun handleCentsFocusChange(hasFocus: Boolean) {
        if (!hasFocus) {
            val cents = binding.editTextCents.text.toString()
            when {
                cents.isEmpty() -> binding.editTextCents.setText("00")
                cents.length == 1 -> binding.editTextCents.setText("0$cents")
            }
        }
    }

    private fun toggleTransactionMode() {
        isTransactionMode = !isTransactionMode
        updateModeUI()
        if (isTransactionMode) updateTransactionUI()
    }

    private fun cycleTransactionType() {
        currentType = when (currentType) {
            TransactionType.INCOME -> TransactionType.EXPENSE
            TransactionType.EXPENSE -> TransactionType.CREDIT
            TransactionType.CREDIT -> TransactionType.INCOME
        }
        updateTransactionUI()
    }


    @Suppress("UNCHECKED_CAST")
    private fun prepareRecurrenceData(): Map<String, Any> {
        return hashMapOf(
            "recurrence" to hashMapOf(
                ("interval" to binding.recurrenceInterval.text?.toString()?.toIntOrNull()) as Pair<Any, Any>,
                ("frequency" to (binding.recurrenceFrequency.selectedItem as? String)) as Pair<Any, Any>,
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

    private fun setupClickListeners() {
        binding.apply {
            btnToggleTransaction.setOnClickListener { toggleTransactionMode() }
            btnToggleIncome.setOnClickListener {
                if (isTransactionMode) cycleTransactionType()
            }
            eventButton.setOnClickListener {
                if (isTransactionMode) toggleTransactionMode()
            }
            transactionButton.setOnClickListener {
                if (!isTransactionMode) toggleTransactionMode()
            }
            saveEventAction.setOnClickListener { saveOrUpdateHabit() }
        }
    }
}