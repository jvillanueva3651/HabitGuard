package com.washburn.habitguard.ui.calendar

import android.annotation.SuppressLint
import java.time.LocalTime
import android.os.Bundle
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.ActivityEventEditBinding

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("SetTextI18n")
class EventEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventEditBinding

    private lateinit var time: LocalTime
    private var isTransactionMode = false
    private var isCreditMode = true
    private var isIncomeMode = true
    private var isExpenseMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEventEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.eventDateTV.text = "Date: ${CalendarUtils.formattedDate(CalendarUtils.selectedDate)} ${CalendarUtils.formattedTime(LocalTime.now())}"

        binding.saveEventAction.isEnabled = false

        updateUIState()

        binding.btnToggleTransaction.setOnClickListener {
            toggleMode() // Toggle the mode
        }

        binding.eventButton.setOnClickListener {
            if (isTransactionMode) {
                toggleMode() // Toggle the mode
            }
        }

        binding.transactionButton.setOnClickListener {
            if (!isTransactionMode) {
                toggleMode() // Toggle the mode
            }
        }

        binding.timeRecyclerView.layoutManager = LinearLayoutManager(this)
        val timePickerAdapter = TimePickerAdapter { selectedTime ->
            time = selectedTime

            binding.saveEventAction.isEnabled = true
        }
        binding.timeRecyclerView.adapter = timePickerAdapter

        binding.saveEventAction.setOnClickListener { saveEventAction() }
    }

    private fun saveEventAction() {
        val eventName = binding.eventNameET.text.toString()
        val newEvent = Event(eventName, CalendarUtils.selectedDate, time)
        Event.eventsList.add(newEvent)
        setResult(RESULT_OK)
        finish()
    }

    private fun toggleMode() {
        isTransactionMode = !isTransactionMode // Toggle the mode
        updateUIState() // Update the UI based on the new mode
    }

    private fun updateUIState() {
        if (isTransactionMode) {
            // Set icon to ic_money and enable Transaction button
            binding.btnToggleTransaction.setImageResource(R.drawable.ic_money)
            binding.transactionButton.isEnabled = false
            binding.transactionButton.alpha = 0.5f // Grayed out
            binding.eventButton.isEnabled = true
            binding.eventButton.alpha = 1f // Fully visible
            binding.transactionType.visibility = View.VISIBLE
            binding.timeRecyclerView.visibility = View.INVISIBLE
            //if (isIncomeMode) {}
        } else {
            // Set icon to ic_event and enable Event button
            binding.btnToggleTransaction.setImageResource(R.drawable.ic_event)
            binding.eventButton.isEnabled = false
            binding.eventButton.alpha = 0.5f // Grayed out
            binding.transactionButton.isEnabled = true
            binding.transactionButton.alpha = 1f // Fully visible
            binding.transactionType.visibility = View.INVISIBLE
            binding.timeRecyclerView.visibility = View.VISIBLE
        }
    }
}