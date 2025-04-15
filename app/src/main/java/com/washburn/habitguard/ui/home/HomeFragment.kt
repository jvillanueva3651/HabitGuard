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

    private fun updateProfile(userId: String) {
        firestoreHelper.getUserDocument(userId)
            .collection("UserInfo")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    binding.userNameTextView.text = document.getString("username") ?: "No username"
                    binding.userEmailTextView.text = document.getString("email") ?: "No email"

                    // Load profile image
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
                            append("For: $nearestDesc\n")
                            append("Due: ${formatDueDate(nearestDate, today)}\n──────────────\n")

                            if (sortedCredits.size > 1) {
                                append("\uD83E\uDDFE Upcoming Total: $${"%.2f".format(totalUpcoming)}\n")
                                append("(${sortedCredits.size - 1} more payments)")
                            } else {
                                append("(No other upcoming payments)")
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

    private fun updateHabitAnalysis() {
        firestoreHelper.getAllUserHabits(
            onSuccess = { habits ->
                if (habits.isNotEmpty()) {
                    val currentDateTime = LocalDateTime.now()
                    val currentDate = currentDateTime.toLocalDate()

                    // Initialize tracking variables
                    val weeklyCompletion = MutableList(7) { false }
                    val monthlyHabits = mutableMapOf<LocalDate, Boolean>()
                    val importantHabits = mutableListOf<String>()
                    val recurringHabits = mutableListOf<String>()
                    var currentStreak = 0
                    var longestStreak = 0
                    var tempStreak = 0

                    // Process each habit
                    habits.forEach { (_, data) ->
                        val name = data["name"] as? String ?: ""
                        val dateStr = data["date"] as? String
                        val timeStr = data["time"] as? String ?: "23:59"
                        val isRecurring = data["isRecurring"] as? Boolean == true

                        if (importantTitles.any { importantTitles ->
                                name.contains(importantTitles, ignoreCase = true)
                            }) {
                            importantHabits.add(name)
                        }
                        if (isRecurring) recurringHabits.add(name)

                        dateStr?.let { dateString ->
                            try {
                                val habitDate = LocalDate.parse(dateString)
                                val habitTime = LocalTime.parse(timeStr)
                                val habitDateTime = LocalDateTime.of(habitDate, habitTime)
                                val isCompleted = habitDateTime.isBefore(currentDateTime)

                                // Monthly tracking
                                if (habitDate.month == currentDate.month && habitDate.year == currentDate.year) {
                                    monthlyHabits[habitDate] = isCompleted
                                }

                                // Weekly tracking
                                if (habitDate.isAfter(currentDate.minusDays(7))) {
                                    val dayOfWeek = habitDate.dayOfWeek.value % 7
                                    weeklyCompletion[dayOfWeek] = weeklyCompletion[dayOfWeek] || isCompleted
                                }

                                // Streak calculation
                                if (isCompleted) {
                                    tempStreak++
                                    longestStreak = maxOf(longestStreak, tempStreak)
                                } else {
                                    tempStreak = 0
                                }
                            } catch (_: Exception) {
                                showToast(requireContext(), "Error parsing date/time: $dateStr $timeStr")
                            }
                        }
                    }

                    // Calculate current streak
                    var streak = 0
                    for (i in currentDate.dayOfWeek.value % 7 downTo 0) {
                        if (weeklyCompletion[i]) streak++ else break
                    }
                    currentStreak = streak

                    // Update UI
                    updateHabitProgressUI(
                        monthlyHabits = monthlyHabits,
                        weeklyCompletion = weeklyCompletion,
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        importantHabits = importantHabits,
                        recurringHabits = recurringHabits
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
                }
            },
            onFailure = { e ->
                showToast(requireContext(), "Error loading habits: ${e.message}")
            }
        )
    }

    private fun updateHabitProgressUI(
        monthlyHabits: Map<LocalDate, Boolean>,
        weeklyCompletion: List<Boolean>,
        currentStreak: Int,
        longestStreak: Int,
        importantHabits: List<String>,
        recurringHabits: List<String>
    ) {
        // Monthly progress
        val monthlyCompleted = monthlyHabits.count { it.value }
        val monthlyTotal = monthlyHabits.size
        val monthlyProgress = if (monthlyTotal > 0) (monthlyCompleted * 100 / monthlyTotal) else 0
        val habitProgressTextViewText = """
            📅 Monthly Progress: $monthlyProgress%
            ✅ Completed: $monthlyCompleted/$monthlyTotal
        """.trimIndent()
        binding.habitProgressTextView.text = habitProgressTextViewText

        // Streak info
        val habitStreakTextViewText = """
            🔥 Current Streak: $currentStreak days
            🏆 Longest Streak: $longestStreak days
        """.trimIndent()
        binding.habitStreakTextView.text = habitStreakTextViewText

        // Tags info
        binding.habitTagsTextView.text = when {
            importantHabits.isNotEmpty() || recurringHabits.isNotEmpty() -> """
                ⭐ Important: ${importantHabits.take(3).joinToString()}${if (importantHabits.size > 3) "..." else ""}
                🔄 Recurring: ${recurringHabits.take(3).joinToString()}${if (recurringHabits.size > 3) "..." else ""}
            """.trimIndent()
            else -> "No tagged habits this month"
        }

        // Update weekly indicators
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