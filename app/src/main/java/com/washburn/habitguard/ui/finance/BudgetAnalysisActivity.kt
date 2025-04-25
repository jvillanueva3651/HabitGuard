package com.washburn.habitguard.ui.finance

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.data.*
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.washburn.habitguard.databinding.ActivityBudgetAnalysisBinding
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import com.washburn.habitguard.ui.calendar.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class BudgetAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetAnalysisBinding
    private lateinit var firestoreHelper: FirestoreHelper
    private var transactions = listOf<Transaction>()
    private var currentPeriod = "MONTHLY"
    private val periodOptions by lazy { resources.getStringArray(R.array.period_options) }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreHelper = FirestoreHelper()
        setupChartConfigs()
        setupPeriodSpinner()
        loadData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupPeriodSpinner() {
        val adapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_item,
            periodOptions
        )

        (binding.periodSpinner as? MaterialAutoCompleteTextView)?.apply {
            setAdapter(adapter)
            setText(periodOptions.first(), false) // Set initial value
            setOnItemClickListener { _, _, position, _ ->
                currentPeriod = when (position) {
                    0 -> "DAILY"
                    1 -> "WEEKLY"
                    else -> "MONTHLY"
                }
                updateCharts()
            }

            // Add visual improvements
            var dropDownBackgroundDrawable =
                ContextCompat.getDrawable(
                    this@BudgetAnalysisActivity,
                    R.drawable.spinner_dropdown_bg
                )
            dropDownVerticalOffset = resources.getDimensionPixelSize(R.dimen.spinner_dropdown_offset)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadData() {
        firestoreHelper.getAllUserTransactions(
            onSuccess = { transactions ->
                this.transactions = transactions.mapNotNull { (id, data) ->
                    Transaction(
                        id = id,
                        amount = data["amount"] as? Double ?: 0.0,
                        type = TransactionType.valueOf(
                            data["transactionType"] as? String ?: TransactionType.EXPENSE.name
                        ),
                        date = data["date"] as? String ?: LocalDate.now().toString(),
                        time = data["transactionTime"] as? String ?: "00:00",
                        tags = data["tags"] as? List<String> ?: emptyList()
                    )
                }.filter { it.type == TransactionType.EXPENSE }

                updateCharts()
            },
            onFailure = { exception ->
                // TODO Handle error
            }
        )
    }

    private fun setupChartConfigs() {
        // Common style config
        listOf(binding.lineChart, binding.barChart, binding.pieChart).forEach {
            // Add this line to all charts
            it.data?.setValueFormatter(dollarFormatter)

            // For line chart specifically
            if (it is LineChart) {
                it.axisLeft.valueFormatter = dollarFormatter
            }
        }

        // Line chart specific
        with(binding.lineChart) {
            axisLeft.valueFormatter = dollarFormatter

            // Enable background grid
            xAxis.setDrawGridLines(true)
            axisLeft.setDrawGridLines(true)

            // Style grid lines
            xAxis.gridColor = Color.LTGRAY
            axisLeft.gridColor = Color.LTGRAY

            // Remove right axis
            axisRight.isEnabled = false

            // Add description
            description.text = "Daily Spending Trend"
            description.textSize = 12f

            setExtraOffsets(10f, 0f, 10f, 20f) // Left, Top, Right, Bottom padding
            xAxis.setLabelCount(7, false) // Force maximum of 7 labels
            axisLeft.spaceTop = 15f // Add space at top of Y-axis
        }

        // Bar chart specific
        with(binding.barChart) {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateCharts() {
        val filteredTransactions = filterTransactionsByPeriod()
        updateLineChart(filteredTransactions)
        updateBarChart(filteredTransactions)
        updatePieChart(filteredTransactions)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterTransactionsByPeriod(): List<Transaction> {
        val now = LocalDate.now()
        return transactions.filter {
            val date = LocalDate.parse(it.date)
            when (currentPeriod) {
                "DAILY" -> date.isEqual(now)
                "WEEKLY" -> date.isAfter(now.minusWeeks(1)) || date.isEqual(now)
                else -> date.month == now.month && date.year == now.year
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateLineChart(transactions: List<Transaction>) {
        // Create a map of dates to daily totals
        val dailyData = transactions.groupBy { it.date }
            .mapValues { (_, transactions) -> transactions.sumOf { abs(it.amount) } }

        // Create sorted list of dates in the period
        val dates = getDatesInPeriod().sorted()

        // Create entries for each day in the period (including days with 0 spending)
        val entries = dates.mapIndexed { index, date ->
            val amount = dailyData[date] ?: 0.0
            Entry(index.toFloat(), amount.toFloat())
        }

        val dataSet = LineDataSet(entries, "Daily Spending").apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER // Creates the smooth "wave" effect
            cubicIntensity = 0.2f
            setDrawCircles(true)
            circleRadius = 4f
            circleHoleRadius = 2f
            setCircleColor(Color.BLUE)
        }

        // Configure X-axis to show dates
        binding.lineChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                private val formatter = DateTimeFormatter.ofPattern("M/d") // Shorter format

                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return dates.getOrNull(value.toInt())?.let {
                        try {
                            LocalDate.parse(it).format(formatter)
                        } catch (e: Exception) {
                            ""
                        }
                    } ?: ""
                }
            }
            granularity = when (currentPeriod) {
                "DAILY" -> 1f
                "WEEKLY" -> 1f
                else -> 2f // Show every other day for monthly
            }
            setLabelCount(7, true) // Show max 7 labels
            labelRotationAngle = -45f // Rotate labels for better fit
            position = XAxis.XAxisPosition.BOTTOM
            setAvoidFirstLastClipping(true) // Prevent edge labels from being cut off
        }

        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.invalidate()
        binding.lineChart.animateX(1000)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDatesInPeriod(): List<String> {
        val now = LocalDate.now()
        return when (currentPeriod) {
            "DAILY" -> listOf(now.toString())
            "WEEKLY" -> (0..6).map { now.minusDays(6 - it.toLong()) }.map { it.toString() }
            else -> (1..now.lengthOfMonth()).map { day ->
                LocalDate.of(now.year, now.month, day).toString()
            }
        }
    }

    private fun updateBarChart(transactions: List<Transaction>) {
        val categoryMap = transactions.flatMap { it.tags }
            .groupingBy { it }
            .eachCount()

        if (categoryMap.isEmpty()) return

        // Create entries with index-based x-values
        val entries = categoryMap.values.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        // Create labels for categories
        val labels = categoryMap.keys.toList()

        val dataSet = BarDataSet(entries, "Spending by Category").apply {
            colors = listOf(
                Color.rgb(65, 105, 225),  // Royal Blue
                Color.rgb(255, 140, 0),    // Dark Orange
                Color.rgb(50, 205, 50),    // Lime Green
                Color.rgb(220, 20, 60),    // Crimson
                Color.rgb(138, 43, 226)     // Blue Violet
            )
            valueTextColor = Color.BLACK
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString() // Show whole numbers only
                }
            }
            setDrawValues(true) // Ensure values are displayed
        }

        // Configure X-axis labels
        binding.barChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return labels.getOrNull(value.toInt()) ?: ""
                }
            }
            granularity = 1f
            setDrawGridLines(false)
            labelRotationAngle = -45f // Rotate labels 45 degrees
            position = XAxis.XAxisPosition.BOTTOM
            setLabelCount(labels.size, true)
            textSize = 10f // Smaller font size
            setCenterAxisLabels(true)
        }

        // Configure Y-axis
        binding.barChart.axisLeft.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString() // Show only integers
                }
            }
            granularity = 1f // Only show whole numbers
            axisMinimum = 0f // Start from zero
            setDrawGridLines(true)
            gridColor = Color.LTGRAY
        }

        // Additional chart styling
        with(binding.barChart) {
            setExtraOffsets(10f, 0f, 10f, 40f) // Add bottom offset for rotated labels
            description.text = "Category Spending Frequency"
            description.textSize = 12f
            legend.isEnabled = true
            animateY(1000) // Add animation
            isDoubleTapToZoomEnabled = false
            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)

            // Set bar width
            data = BarData(dataSet).apply {
                barWidth = 0.4f // Adjust bar width for better spacing
            }

            // Remove right axis
            axisRight.isEnabled = false
            invalidate()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePieChart(transactions: List<Transaction>) {
        firestoreHelper.getUserBudget(
            onSuccess = { budget ->
                budget?.let { (amount, _) ->
                    val totalSpent = transactions.sumOf { abs(it.amount) }
                    val remaining = amount - totalSpent

                    val entries = listOf(
                        PieEntry(totalSpent.toFloat(), "Spent"),
                        PieEntry(remaining.toFloat(), "Remaining")
                    )

                    val dataSet = PieDataSet(entries, "Budget vs Actual").apply {
                        colors = listOf(Color.RED, Color.GREEN)
                        valueTextColor = Color.WHITE
                    }

                    binding.pieChart.data = PieData(dataSet)
                    binding.pieChart.invalidate()
                }
            },
            onFailure = { exception ->
                // TODO Handle error
            }
        )
    }

    // Extension function to get week of month
    @RequiresApi(Build.VERSION_CODES.O)
    private fun LocalDate.getWeekOfMonth(): Int {
        return (dayOfMonth - 1) / 7 + 1
    }

    //Dollar formatter
    private val dollarFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "$${String.format("%.2f", value)}"
        }
    }
}