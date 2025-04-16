package com.washburn.habitguard.ui.home

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.washburn.habitguard.R
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.databinding.FragmentHomeBinding
import com.washburn.habitguard.firebase.AuthUtils.showToast
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestoreHelper = FirestoreHelper()

        val userId = firestoreHelper.getCurrentUserId() ?: run {
            return
        }

        updateProfile(userId) // Fetch user profile data

        checkUpcomingCredits()

        updateHabitAnalysis() // Update habit analysis data

        updateFinanceAnalysis() // Update finance analysis data
    }

    // Update profile
    private fun updateProfile(userId: String) {
        firestoreHelper.getUserDocument(userId)
            .collection("UserInfo")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    binding.userNameTextView.text = document.getString("username") ?: "No username"
                    binding.userEmailTextView.text = document.getString("email") ?: "No email"

                    document.getString("photoUri")?.let { uri ->
                        Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .circleCrop()
                            .into(binding.userProfileImageView)
                    } ?: run {
                        binding.userProfileImageView.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                } else {
                    showToast(requireContext(), "No profile data found")
                }
            }
            .addOnFailureListener { e ->
                showToast(requireContext(), "Error loading profile: $e")
            }
    }

    // Credit Notification
    private fun checkUpcomingCredits() {
        binding.dismissCreditButton.setOnClickListener {
            binding.upcomingCreditsCard.visibility = View.GONE
        }

        firestoreHelper.getAllUserTransactions(
            onSuccess = { transactions ->
                val today = LocalDate.now()
                val upcomingCredits = transactions.filter { (_, data) ->
                    val dateStr = data["date"] as? String ?: return@filter false
                    val amount = data["amount"] as? Double ?: 0.0
                    val type = data["transactionType"] as? String ?: ""

                    type.equals("CREDIT", ignoreCase = true) &&
                            amount > 0 &&
                            !LocalDate.parse(dateStr).isBefore(today)
                }

                // Sort by date (nearest first)
                val sortedCredits = upcomingCredits.sortedBy {
                    LocalDate.parse(it.second["date"] as String).toEpochDay() - today.toEpochDay()
                }

                activity?.runOnUiThread {
                    if (sortedCredits.isNotEmpty()) {
                        // 1. Get nearest credit (first item in sorted list)
                        val nearestCredit = sortedCredits.first()
                        val (nearestDate, nearestAmount, nearestDesc) = getCreditDetails(nearestCredit)

                        // 2. Calculate cumulative total of REMAINING credits (excluding nearest)
                        val totalUpcoming = sortedCredits
                            .drop(1) // Skip the first/nearest payment
                            .sumOf { it.second["amount"] as? Double ?: 0.0 }

                        // 3. Format the display text
                        val upcomingCreditsText = buildString {
                            append("⚠️ Next Payment: $${"%.2f".format(nearestAmount)}\n")
                            append("Due: ${formatDueDate(nearestDate, today)}\n")
                            append("For: $nearestDesc")

                            if (sortedCredits.size > 1) {
                                append("\n──────────────\n")
                                append("\uD83E\uDDFE Upcoming Total: $${"%.2f".format(totalUpcoming)}\n")
                                append("(${sortedCredits.size - 1} more payments)")
                            }
                        }

                        binding.upcomingCreditText.text = upcomingCreditsText
                        binding.upcomingCreditsCard.visibility = View.VISIBLE
                    } else {
                        binding.upcomingCreditsCard.visibility = View.GONE
                    }
                }
            },
            onFailure = { e ->
                showToast(requireContext(), "Error checking credits: ${e.message}")
            }
        )
    }
    // Helper function to extract credit details
    private fun getCreditDetails(credit: Pair<String, Map<String, Any>>): Triple<LocalDate, Double, String> {
        val data = credit.second
        val date = LocalDate.parse(data["date"] as String)
        val amount = data["amount"] as Double
        val description = data["description"] as? String ?: "Upcoming credit"
        return Triple(date, amount, description)
    }
    // Helper function to format due date
    private fun formatDueDate(date: LocalDate, today: LocalDate): String {
        val daysUntil = date.toEpochDay() - today.toEpochDay()
        return when {
            daysUntil == 0L -> "TODAY"
            daysUntil == 1L -> "TOMORROW"
            daysUntil <= 7 -> "in $daysUntil days"
            else -> "on ${date.month.toString().lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}"
        }
    }

    // Habit Analysis
    private fun updateHabitAnalysis() {
        firestoreHelper.getAllUserHabits(
            onSuccess = { habits ->
                if (habits.isNotEmpty()) {
                    val currentDateTime = LocalDateTime.now()
                    val currentDate = currentDateTime.toLocalDate()
                    val currentMonth = currentDate.month
                    val currentYear = currentDate.year

                    val allMonthlyHabitDates = mutableSetOf<LocalDate>()
                    val completedDates = mutableSetOf<LocalDate>()
                    val weeklyCompletion = MutableList(7) { false }
                    val importantHabits = mutableListOf<String>()
                    val recurringHabits = mutableListOf<String>()

                    // Process each habit
                    habits.forEach { (_, data) ->
                        val name = data["name"] as? String ?: ""
                        val dateStr = data["date"] as? String
                        val timeStr = data["time"] as? String ?: "23:59"
                        val isRecurring = data["isRecurring"] as? Boolean == true
                        val tags = when (val tagsField = data["tags"]) {
                            is List<*> -> tagsField.filterIsInstance<String>()
                            else -> emptyList()
                        }

                        // Track important/recurring habits
                        // Method 1: Check tags array for "important"
                        if (tags.any { it.equals("important", ignoreCase = true) }) {
                            importantHabits.add(name)
                        }
                        // Method 2: Check name for important keywords
                        else if (importantTitles.any { name.contains(it, ignoreCase = true) }) {
                            importantHabits.add(name)
                        }
                        if (isRecurring) {
                            recurringHabits.add(name)
                        }

                        dateStr?.let { dateString ->
                            try {
                                val habitDate = LocalDate.parse(dateString)
                                val habitTime = LocalTime.parse(timeStr)
                                val habitDateTime = LocalDateTime.of(habitDate, habitTime)

                                // Count as scheduled if in current month
                                if (habitDate.month == currentMonth && habitDate.year == currentYear) {
                                    allMonthlyHabitDates.add(habitDate)

                                    // Check if completed (past current time)
                                    if (habitDateTime.isBefore(currentDateTime)) {
                                        completedDates.add(habitDate)

                                        // Weekly tracking (last 7 days)
                                        if (habitDate.isAfter(currentDate.minusDays(7))) {
                                            val dayOfWeek = habitDate.dayOfWeek.value % 7
                                            weeklyCompletion[dayOfWeek] = true
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                showToast(requireContext(),"Error parsing date/time: $dateString $timeStr")
                            }
                        }
                    }

                    // Calculate streaks properly
                    val (currentStreak, longestStreak) = calculateStreaks(
                        completedDates = completedDates,
                        currentDate = currentDate
                    )

                    // Update UI
                    updateHabitProgressUI(
                        monthlyCompleted = completedDates.size,
                        monthlyScheduled = allMonthlyHabitDates.size,
                        weeklyCompletion = weeklyCompletion,
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        importantHabits = importantHabits,
                        recurringHabits = recurringHabits,
                        completedDates = completedDates
                    )
                } else {
                    // No habits found
                    val defProgress = "No habits tracked yet"
                    val defStreak = "Start building your streaks!"
                    val defCurStreak = "0 days"
                    val defLonStreak = "0 days"
                    binding.habitProgressTextView.text = defProgress
                    binding.habitStreakTextView.text = defStreak
                    binding.currentStreakTextView.text = defCurStreak
                    binding.longestStreakTextView.text = defLonStreak

                    binding.incomeTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green_soft))
                    binding.expenseTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red_soft))
                    binding.creditTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.credit_yellow_soft))

                    val indicators = listOf(
                        binding.indicatorSun, binding.indicatorMon,
                        binding.indicatorTue, binding.indicatorWed,
                        binding.indicatorThu, binding.indicatorFri,
                        binding.indicatorSat
                    )

                    val currentDayOfWeek = LocalDate.now().dayOfWeek.value % 7
                    val tealColor = ContextCompat.getColor(requireContext(), R.color.teal_200)

                    indicators.forEachIndexed { index, imageView ->
                        imageView.clearColorFilter()

                        when {
                            index == currentDayOfWeek -> {
                                imageView.setImageResource(R.drawable.ic_current_date)
                                imageView.setColorFilter(tealColor)
                            }
                            index < currentDayOfWeek -> {
                                imageView.setImageResource(R.drawable.ic_past_inactive_day)
                            }
                            else -> {
                                imageView.setImageResource(R.drawable.ic_inactive_day)
                            }
                        }
                    }

                }
            },
            onFailure = { e ->
                showToast(requireContext(), "Error loading habits: ${e.message}")
            }
        )
    }

    private fun calculateStreaks(
        completedDates: Set<LocalDate>,
        currentDate: LocalDate
    ): Pair<Int, Int> {
        if (completedDates.isEmpty()) return Pair(0, 0)

        val sortedDates = completedDates.sorted()
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        var previousDate: LocalDate? = null

        for (date in sortedDates) {
            when {
                previousDate == null -> {
                    tempStreak = 1
                    if (date == currentDate || date == currentDate.minusDays(1)) {
                        currentStreak = tempStreak
                    }
                }
                previousDate.plusDays(1) == date -> {
                    tempStreak++
                    if (date >= currentDate.minusDays(tempStreak.toLong())) {
                        currentStreak = tempStreak
                    }
                }
                else -> tempStreak = 1
            }
            longestStreak = maxOf(longestStreak, tempStreak)
            previousDate = date
        }

        return Pair(currentStreak, longestStreak)
    }

    private fun updateHabitProgressUI(
        monthlyCompleted: Int,
        monthlyScheduled: Int,
        weeklyCompletion: List<Boolean>,
        currentStreak: Int,
        longestStreak: Int,
        importantHabits: List<String>,
        recurringHabits: List<String>,
        completedDates: Set<LocalDate>
    ) {
        // 1. Calculate Progress Percentages
        val monthlyProgress = if (monthlyScheduled > 0) {
            (monthlyCompleted * 100 / monthlyScheduled)
        } else {
            0
        }

        val currentDate = LocalDate.now()
        val last30DaysCompleted = completedDates.count {
            it.isAfter(currentDate.minusDays(30))
        }
        val thirtyDayRate = (last30DaysCompleted / 30f * 100).toInt()

        // 2. Update Progress TextView
        val habitProgressTextViewText = """
        📅 Monthly Progress: $monthlyProgress%
        ✅ Completed: $monthlyCompleted/$monthlyScheduled habits
        📊 30-Day Rate: $thirtyDayRate%
        """.trimIndent()
        binding.habitProgressTextView.text = habitProgressTextViewText

        // 3. Update Streak Information
        val habitStreakTextViewText = """
        🔥 Current Streak: $currentStreak ${if (currentStreak == 1) "day" else "days"}
        🏆 Longest Streak: $longestStreak ${if (longestStreak == 1) "day" else "days"}
        """.trimIndent()
        binding.habitStreakTextView.text = habitStreakTextViewText

        // 4. Update Tags Display
        binding.habitTagsTextView.text = buildString {
            if (importantHabits.isNotEmpty()) {
                append("⭐ Important Habits:\n")
                importantHabits.take(3).forEach { append("• $it\n") }
                if (importantHabits.size > 3) append("+ ${importantHabits.size - 3} more\n")
            }

            if (recurringHabits.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append("🔄 Recurring Habits:\n")
                recurringHabits.take(3).forEach { append("• $it\n") }
                if (recurringHabits.size > 3) append("+ ${recurringHabits.size - 3} more")
            }

            if (isEmpty()) {
                append("No tagged habits this month")
            }
        }.trim()

        // 5. Update Weekly Indicators
        updateWeeklyIndicators(weeklyCompletion)
    }

    private fun updateWeeklyIndicators(weeklyCompletion: List<Boolean>) {
        val indicators = listOf(
            binding.indicatorSun, binding.indicatorMon,
            binding.indicatorTue, binding.indicatorWed,
            binding.indicatorThu, binding.indicatorFri,
            binding.indicatorSat
        )

        val currentDayOfWeek = LocalDate.now().dayOfWeek.value % 7
        val tealColor = ContextCompat.getColor(requireContext(), R.color.teal_200)
        val tealDarkColor = ContextCompat.getColor(requireContext(), R.color.teal_700)

        indicators.forEachIndexed { index, imageView ->
            imageView.clearColorFilter()

            when {
                index == currentDayOfWeek -> {
                    imageView.setImageResource(R.drawable.ic_current_date)
                    imageView.setColorFilter(tealColor)
                }
                weeklyCompletion[index] -> {
                    imageView.setImageResource(R.drawable.ic_active_day)
                    imageView.setColorFilter(tealDarkColor)
                }
                index < currentDayOfWeek -> {
                    imageView.setImageResource(R.drawable.ic_past_inactive_day)
                }
                else -> {
                    imageView.setImageResource(R.drawable.ic_inactive_day)
                }
            }
        }
    }

    private fun updateFinanceAnalysis() {
        firestoreHelper.getAllUserTransactions(
            onSuccess = { transactions ->
                // Filter transactions for the current week
                val currentDate = LocalDate.now()
                val startOfWeek = currentDate.minusDays(currentDate.dayOfWeek.value.toLong() % 7)
                val endOfWeek = startOfWeek.plusDays(6)

                val weeklyTransactions = transactions.filter { (_, data) ->
                    val dateString = data["date"] as? String ?: return@filter false
                    val transactionDate = LocalDate.parse(dateString)
                    !transactionDate.isBefore(startOfWeek) && !transactionDate.isAfter(endOfWeek)
                }

                var totalIncome = 0.0
                var totalExpense = 0.0
                var totalCredit = 0.0

                weeklyTransactions.forEach { (_, data) ->
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
                    binding.incomeTextView.text =
                        getString(R.string.income_label, totalIncome)
                    binding.expenseTextView.text =
                        getString(R.string.expense_label, -totalExpense)
                    binding.creditTextView.text =
                        getString(R.string.credit_label, totalCredit)
                }
            },
            onFailure = { e ->
                showToast(requireContext(), "Error loading transactions: $e")
            }
        )
    }

    override fun onResume() {
        super.onResume()

        checkUpcomingCredits()

        updateHabitAnalysis()

        updateFinanceAnalysis()
    }

    companion object {
        val importantTitles = setOf(
            "Birthday",
            "Exam",
            "Mortgage",
            "Doctor Appointment",
            "Tax Due",
            "Payment",
            "Bill",
            "Due"
        )
    }
}