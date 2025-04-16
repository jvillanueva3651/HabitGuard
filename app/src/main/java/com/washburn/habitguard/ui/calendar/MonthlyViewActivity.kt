package com.washburn.habitguard.ui.calendar

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.Toast
import java.time.LocalDate
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.FragmentCalendarBinding
import com.washburn.habitguard.firebase.AuthUtils.showToast
import com.washburn.habitguard.ui.calendar.CalendarUtils.daysInMonthArray
import com.washburn.habitguard.ui.calendar.CalendarUtils.monthYearFromDate
import com.washburn.habitguard.ui.calendar.CalendarAdapter.OnItemListener
import com.washburn.habitguard.ui.calendar.CalendarUtils.selectedDate

@SuppressLint("NotifyDataSetChanged")
@RequiresApi(Build.VERSION_CODES.O)
class MonthlyViewActivity : Fragment(), OnItemListener {

    private lateinit var binding: FragmentCalendarBinding

    private lateinit var eventAdapter: EventAdapter // Updated Event view

    private lateinit var firestoreHelper: FirestoreHelper // Access to Firestore

    private var allEvents: List<Pair<String, Map<String, Any>>> = emptyList() // Store all events
    private var allTransaction: List<Pair<String, Map<String, Any>>> = emptyList() // Store all transaction

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestoreHelper = FirestoreHelper()

        selectedDate = LocalDate.now() // Initialize selectedDate

        setupViews()
        loadEvents()
        loadTransactionsForDate()
    }

    private fun setupViews() {
        binding.apply {
            monthYearTV.text = monthYearFromDate(selectedDate)
            monthYearTV.setOnClickListener { showYearPicker() }

            previousMonthButton.setOnClickListener { previousMonthAction() }
            nextMonthButton.setOnClickListener { nextMonthAction() }
            weeklyActionButton.setOnClickListener { weeklyAction() }
            addDotButton.setOnClickListener { newEventAction() }

            // Setup calendar grid
            calendarRecyclerView.layoutManager = GridLayoutManager(requireContext(), 7)
            // Setup events list
            itemsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadEvents() {
        firestoreHelper.getAllUserHabits(
            onSuccess = { habits ->
                allEvents = habits.sortedWith(compareBy(
                    { it.second["date"] as? String ?: "" },
                    { it.second["startTime"] as? String ?: "00:00" }
                ))
                setMonthView()
                setEventAdapter()
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadTransactionsForDate() {
        firestoreHelper.getAllUserTransactions(
            onSuccess = { transactions ->
                allTransaction = transactions.sortedWith(compareBy(
                    { it.second["date"] as? String ?: "" },
                    { it.second["time"] as? String ?: "00:00" }
                ))

                // Filter transactions for the specific date
                val dailyTransactions = allTransaction
                    .filter { (_, transaction) ->
                        transaction["date"] == selectedDate.toString()
                    }

                var totalIncome = 0.0
                var totalExpense = 0.0
                var totalCredit = 0.0

                dailyTransactions.forEach { (_, data) ->
                    val type = data["transactionType"] as? String ?: ""
                    val amount = data["amount"] as? Double ?: 0.0
                    when (type) {
                        "INCOME" -> totalIncome += amount
                        "EXPENSE" -> totalExpense += amount
                        "CREDIT" -> totalCredit += amount
                    }
                }

                // Update UI
                activity?.runOnUiThread {
                    binding.incomeValue.text = getString(R.string.income_label, totalIncome)
                    binding.expenseValue.text = getString(R.string.expense_label, totalExpense)
                    binding.creditValue.text = getString(R.string.credit_label, totalCredit)
                }
            },
            onFailure = { e ->
                showToast(requireContext(), "Error loading transactions: $e")
            }
        )
    }

    private fun showYearPicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                setMonthView()
                setEventAdapter()
            },
            selectedDate.year,
            selectedDate.monthValue - 1, // Month is 0-based in DatePicker
            selectedDate.dayOfMonth
        )

        datePickerDialog.datePicker.apply {
            updateDate(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)

            try {
                val yearField = javaClass.getDeclaredField("mYearPicker")
                yearField.isAccessible = true
                val yearPicker = yearField.get(this) as View
                yearPicker.performClick()
            } catch (_: Exception) {
                // Fallback to normal date picker if we can't access the year field
            }
        }

        datePickerDialog.show()
    }

    private fun setMonthView() {
        binding.monthYearTV.text = monthYearFromDate(selectedDate)

        binding.calendarRecyclerView.background = ContextCompat.getDrawable(
            requireContext(),
            when (selectedDate.monthValue) {
                12 -> R.drawable.bg_card_dec
                1 -> R.drawable.bg_card_jan
                2 -> R.drawable.bg_card_feb
                3 -> R.drawable.bg_card_mar
                4 -> R.drawable.bg_card_apr
                5 -> R.drawable.bg_card_may
                6 -> R.drawable.bg_card_jun
                7 -> R.drawable.bg_card_jul
                8 -> R.drawable.bg_card_aug
                9 -> R.drawable.bg_card_sep
                10 -> R.drawable.bg_card_oct
                11 -> R.drawable.bg_card_nov
                else -> R.drawable.bg_card_def
            }
        )

        binding.calendarRecyclerView.adapter = CalendarAdapter(
            daysInMonthArray(),
            this,
            allEvents.map { (_, eventData) ->
                mapOf(
                    "name" to (eventData["name"] as? String ?: ""),
                    "date" to (eventData["date"] as? String ?: ""),
                    "time" to (eventData["startTime"] as? String ?: "00:00")
                )
            },
            allTransaction.map { (_, transactionData) ->
                mapOf(
                    "name" to (transactionData["name"] as? String ?: ""),
                    "date" to (transactionData["date"] as? String ?: ""),
                    "time" to (transactionData["time"] as? String ?: "00:00")
                )
            }
        )
    }

    private fun setEventAdapter() {
        val dailyEvents = allEvents
            .filter { (_, eventData) ->
                eventData["date"] == selectedDate.toString()
            }
            .sortedBy { (_, eventData) ->
                eventData["startTime"] as? String ?: "00:00"
            }

        eventAdapter = EventAdapter(
            context = requireContext(),
            events = dailyEvents,
            firestoreHelper = firestoreHelper,
            onEditClick = { documentId ->
                startActivity(Intent(requireContext(), EventEditActivity::class.java).apply {
                    putExtra(EventEditActivity.EXTRA_HABIT_ID, documentId)
                })
            },
            onDeleteSuccess = { loadEvents() } // Auto-refresh after deletion
        )

        binding.itemsRecyclerView.adapter = eventAdapter
    }

    private fun previousMonthAction() {
        selectedDate = selectedDate.minusMonths(1)
        setMonthView()
    }

    private fun nextMonthAction() {
        selectedDate = selectedDate.plusMonths(1)
        setMonthView()
    }

    private fun newEventAction() {
        val intent = Intent(requireContext(), EventEditActivity::class.java).apply {

            putExtra("date", selectedDate.toString())
            putExtra("isTransaction", false)
        }
        startActivity(intent)
    }
    private fun weeklyAction() { startActivity(Intent(requireContext(), WeeklyViewActivity::class.java)) }

    override fun onItemClick(position: Int, date: LocalDate) {
        selectedDate = date
        setMonthView()
        setEventAdapter() // Update events list when date changes
    }

    override fun onResume() {
        super.onResume()
        loadEvents() // Refresh data when returning from other screens
        loadTransactionsForDate()
    }
}