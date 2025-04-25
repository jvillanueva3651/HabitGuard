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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

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
                            append("Due: ${formatDueDate(nearestDate, today)}")
                            if (!nearestDesc.isEmpty()) {
                                append("\nFor: $nearestDesc")
                            }
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
                    val currentTime = currentDateTime.toLocalTime()
                    val currentMonth = currentDate.month
                    val currentYear = currentDate.year

                    val allMonthlyHabits = mutableListOf<Pair<LocalDate, String>>()
                    val completedHabits = mutableListOf<Pair<LocalDate, String>>()
                    val habitCompletionCount = mutableMapOf<LocalDate, Int>()
                    val weeklyCompletion = MutableList(7) { false }
                    val importantHabits = mutableListOf<String>()
                    val recurringHabits = mutableListOf<String>()

                    // Process each habit
                    habits.forEach { (_, data) ->
                        val name = data["name"] as? String ?: ""
                        val dateStr = data["date"] as? String
                        val endTimeStr = data["endTime"] as? String ?: "23:59"
                        val isRecurring = data["isRecurring"] as? Boolean == true
                        val tags = when (val tagsField = data["tags"]) {
                            is List<*> -> tagsField.filterIsInstance<String>()
                            else -> emptyList()
                        }

                        // Track important/recurring habits
                        if (tags.any { it.equals("important", ignoreCase = true) }) {
                            importantHabits.add(name)
                        }
                        else if (importantTitles.any { name.contains(it, ignoreCase = true) }) {
                            importantHabits.add(name)
                        }
                        if (isRecurring) {
                            recurringHabits.add(name)
                        }

                        dateStr?.let { dateString ->
                            try {
                                val habitDate = LocalDate.parse(dateString)
                                val endTime = LocalTime.parse(endTimeStr)
                                val currentTime = LocalTime.now()
                                val (weekStart, weekEnd) = getCurrentWeekBounds()

                                // Count as scheduled if in current month
                                if (habitDate.month == currentMonth && habitDate.year == currentYear) {
                                    allMonthlyHabits.add(habitDate to name)

                                    val isCompleted = when {
                                        habitDate.isBefore(currentDate) -> true
                                        habitDate == currentDate -> currentTime.isAfter(endTime)
                                        else -> false
                                    }

                                    if (isCompleted) {
                                        completedHabits.add(habitDate to name)
                                        habitCompletionCount[habitDate] = (habitCompletionCount[habitDate] ?: 0) + 1
                                    }

                                    if (habitDate in weekStart..weekEnd) {
                                        weeklyCompletion[habitDate.dayOfWeek.value % 7] = true
                                    }
                                }
                            } catch (_: Exception) {
                                showToast(requireContext(),"Error parsing date/time: $dateString")
                            }
                        }
                    }

                    // Calculate streaks with all completed habits
                    val (currentStreak, longestStreak) = calculateStreaks(
                        completedHabits = completedHabits,
                        currentDate = currentDate,
                        currentTime = currentTime,
                        habits = habits
                    )

                    // Calculate total possible completions (considering time windows)
                    val totalPossible = allMonthlyHabits.count { (date, _) ->
                        date.isBefore(currentDate) ||
                                (date == currentDate && currentTime.isAfter(LocalTime.parse("23:59")))
                    }

                    val adjustedMonthlyProgress = if (totalPossible > 0) {
                        ((completedHabits.size.toFloat() / totalPossible.toFloat()) * 100).toInt().coerceAtMost(100)
                    } else {
                        0
                    }

                    // Update UI
                    updateHabitProgressUI(
                        monthlyCompleted = completedHabits.size,
                        monthlyScheduled = allMonthlyHabits.size,
                        adjustedMonthlyProgress = adjustedMonthlyProgress,
                        weeklyCompletion = weeklyCompletion,
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        importantHabits = importantHabits,
                        recurringHabits = recurringHabits,
                        habitCompletionCount = habitCompletionCount,
                        habits = habits
                    )
                } else {
                    // No habits found
                    val defProgress = "🎯 No habits tracked yet"
                    val defStreak = "⏳ Start building your streaks!"
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
                    val color = ContextCompat.getColor(requireContext(), R.color.teal_700)

                    indicators.forEachIndexed { index, imageView ->
                        imageView.clearColorFilter()

                        when {
                            index == currentDayOfWeek -> {
                                imageView.setImageResource(R.drawable.ic_current_date)
                                imageView.setColorFilter(color)
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
    private fun getCurrentWeekBounds(): Pair<LocalDate, LocalDate> {
        val currentDate = LocalDate.now()
        val startOfWeek = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val endOfWeek = startOfWeek.plusDays(6) // Saturday
        return Pair(startOfWeek, endOfWeek)
    }
    private fun calculateStreaks(
        completedHabits: List<Pair<LocalDate, String>>,
        currentDate: LocalDate,
        currentTime: LocalTime,
        habits: List<Pair<String, Map<String, Any>>>
    ): Pair<Int, Int> {
        if (completedHabits.isEmpty()) return Pair(0, 0)

        val validCompletedHabits = completedHabits.filter { (date, _) ->
            if (date == currentDate) {
                habits.any { (_, data) ->
                    data["date"]?.toString()?.let { dateStr ->
                        try {
                            val habitDate = LocalDate.parse(dateStr)
                            if (habitDate == currentDate) {
                                val endTimeStr = data["endTime"] as? String ?: "23:59"
                                currentTime.isAfter(LocalTime.parse(endTimeStr))
                            } else false
                        } catch (_: Exception) { false }
                    } == true
                }
            } else true
        }

        val uniqueDates = validCompletedHabits.map { it.first }.distinct().sorted()

        // 2. Calculate LONGEST streak (all historical data)
        var longestStreak = 0
        var tempLongest = 0
        var prevDate: LocalDate? = null

        uniqueDates.forEach { date ->
            when {
                prevDate == null -> tempLongest = 1
                prevDate.plusDays(1) == date -> tempLongest++
                else -> {
                    longestStreak = maxOf(longestStreak, tempLongest)
                    tempLongest = 1
                }
            }
            prevDate = date
        }
        longestStreak = maxOf(longestStreak, tempLongest) // Final update

        // 3. Calculate CURRENT streak (starting from today backward)
        var currentStreak = 0
        var checkingDate = currentDate

        while (uniqueDates.contains(checkingDate)) {
            currentStreak++
            checkingDate = checkingDate.minusDays(1)
        }

        return Pair(currentStreak, longestStreak)
    }

    private fun updateHabitProgressUI(
        monthlyCompleted: Int,
        monthlyScheduled: Int,
        adjustedMonthlyProgress: Int,
        weeklyCompletion: List<Boolean>,
        currentStreak: Int,
        longestStreak: Int,
        importantHabits: List<String>,
        recurringHabits: List<String>,
        habitCompletionCount: Map<LocalDate, Int>,
        habits: List<Pair<String, Map<String, Any>>>
    ) {
        val currentDate = LocalDate.now()
        val daysPassed = currentDate.dayOfMonth

        // Calculate additional metrics
        val last30DaysCompleted = habitCompletionCount.keys.count {
            it.isAfter(currentDate.minusDays(30))
        }
        val thirtyDayRate = (last30DaysCompleted / 30f * 100).toInt()

        // Update Progress TextView with time window awareness
        val progressEmoji = when {
            adjustedMonthlyProgress >= 100 -> "🚀"
            adjustedMonthlyProgress >= 75 -> "🔥"
            adjustedMonthlyProgress >= 50 -> "📈"
            adjustedMonthlyProgress > 0 -> "🌱"
            else -> "🛌"
        }

        val habitProgressTextViewText = """
    $progressEmoji Monthly Progress: 
    ▸ 📅 : $adjustedMonthlyProgress%
    ▸ ✅ : $monthlyCompleted/$monthlyScheduled habits
    ▸ 🗓️ : ${habitCompletionCount.size}/$daysPassed
    ▸ 🕡 : $thirtyDayRate%
    """.trimIndent()
        binding.habitProgressTextView.text = habitProgressTextViewText

        val streakCelebration = when {
            currentStreak == longestStreak && currentStreak > 7 -> "🎉 You're at your all-time best!"
            currentStreak == longestStreak && currentStreak > 0 -> "✨ Matching your record!"
            longestStreak - currentStreak < 3 && currentStreak > 5 -> "👀 Almost at your record!"
            else -> ""
        }

        // 3. Update Streak TextView
        val habitStreakTextViewText = """
    ${getStreakEmoji(currentStreak)} Current Streak: $currentStreak ${if (currentStreak == 1) "day" else "days"}
    ${getLongStreakEmoji(longestStreak)} Longest Streak: $longestStreak days
    $streakCelebration
    ${if (currentStreak > 0 && longestStreak > currentStreak)
            "🔜 ${longestStreak - currentStreak} more days to beat your record!"
        else ""}
    """.trimIndent()
        binding.habitStreakTextView.text = habitStreakTextViewText

        // 4. Update Tags Display
        binding.habitTagsTextView.text = buildString {
            if (importantHabits.isNotEmpty()) {
                append("${getRandomImportantEmoji()} Important Habits:\n")
                importantHabits.take(3).forEach { habit ->
                    append("${getHabitEmoji(habit)} $habit\n")
                }
                if (importantHabits.size > 3) append("✨ +${importantHabits.size - 3} more\n")
            }

            if (recurringHabits.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append("🌀 Recurring Habits:\n")
                recurringHabits.take(3).forEach { habit ->
                    append("${getHabitEmoji(habit)} $habit\n")
                }
                if (recurringHabits.size > 3) append("🌀 +${recurringHabits.size - 3} more")
            }

            if (isEmpty()) {
                append("🎯 No tagged habits this month")
            }
        }.trim()

        // 5. Update Weekly Indicators
        updateWeeklyIndicators(weeklyCompletion, habits)

        // 6. Update Streak Display
        binding.currentStreakTextView.text = currentStreak.toString()
        binding.longestStreakTextView.text = longestStreak.toString()
    }

    private fun getStreakEmoji(streak: Int): String = when {
        streak == 0 -> "⏳"
        streak < 3 -> "🌱"
        streak < 7 -> "🔥"
        streak < 14 -> "🚀"
        streak < 30 -> "🏆"
        else -> "🐉"
    }

    private fun getLongStreakEmoji(streak: Int): String = when {
        streak == 0 -> "🕳️"
        streak < 7 -> "🌿"
        streak < 14 -> "🌟"
        streak < 30 -> "💎"
        streak < 60 -> "🏅"
        else -> "🦸"
    }

    private fun getRandomImportantEmoji(): String {
        val importantEmojis = listOf("⭐", "🌟", "✨", "💎", "👑", "🏆", "🎯", "🚀")
        return importantEmojis.random()
    }

    private fun getHabitEmoji(habitName: String): String {
        return when {
            habitName.contains("work", true) -> "💼"
            habitName.contains("exercise", true) -> "🏋️"
            habitName.contains("read", true) -> "📚"
            habitName.contains("water", true) -> "🚰"
            habitName.contains("sleep", true) -> "😴"
            habitName.contains("meditation", true) -> "🧘"
            habitName.contains("study", true) -> "📖"
            habitName.contains("walk", true) -> "🚶"
            habitName.contains("run", true) -> "🏃"
            habitName.contains("yoga", true) -> "🧘‍♀️"
            else -> listOf("🌱", "🌿", "🌸", "🌻", "🌈", "⚡").random()
        }
    }

    private fun updateWeeklyIndicators(
        weeklyCompletion: List<Boolean>,
        habits: List<Pair<String, Map<String, Any>>>
    ) {
        val indicators = listOf(
            binding.indicatorSun, binding.indicatorMon,
            binding.indicatorTue, binding.indicatorWed,
            binding.indicatorThu, binding.indicatorFri,
            binding.indicatorSat
        )

        val currentDate = LocalDate.now()
        val currentTime = LocalTime.now()

        val currentDayOfWeek = LocalDate.now().dayOfWeek.value % 7
        val tealColor = ContextCompat.getColor(requireContext(), R.color.teal_200)
        val tealDarkColor = ContextCompat.getColor(requireContext(), R.color.teal_700)

        val currentDayCompleted = habits.any { (_, data) ->
            data["date"]?.toString()?.let { dateStr ->
                try {
                    val habitDate = LocalDate.parse(dateStr)
                    if (habitDate == currentDate) {
                        val endTimeStr = data["endTime"] as? String ?: "23:59"
                        currentTime.isAfter(LocalTime.parse(endTimeStr))
                    } else false
                } catch (_: Exception) { false }
            } == true
        }

        indicators.forEachIndexed { index, imageView ->
            imageView.clearColorFilter()

            when {
                index == currentDayOfWeek -> {
                    imageView.setImageResource(R.drawable.ic_current_date)
                    imageView.setColorFilter(
                        if (currentDayCompleted) tealColor else tealDarkColor
                    )
                }
                index < currentDayOfWeek && weeklyCompletion[index] -> {
                    imageView.setImageResource(R.drawable.ic_active_day)
                    imageView.setColorFilter(tealColor)
                }
                index > currentDayOfWeek && weeklyCompletion[index] -> {
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

                    binding.incomeTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green_soft))
                    binding.expenseTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red_soft))
                    binding.creditTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.credit_yellow_soft))
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