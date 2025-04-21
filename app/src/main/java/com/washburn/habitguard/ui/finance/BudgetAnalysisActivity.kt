package com.washburn.habitguard.ui.finance

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.data.*
import com.washburn.habitguard.databinding.ActivityBudgetAnalysisBinding
import com.washburn.habitguard.FirestoreHelper
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
        binding.periodSpinner.setOnItemClickListener { _, _, position, _ ->
            currentPeriod = when (position) {
                0 -> "DAILY"
                1 -> "WEEKLY"
                else -> "MONTHLY"
            }
            updateCharts()
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
            it.description.isEnabled = false
            it.legend.isEnabled = true
            it.setTouchEnabled(true)
            it.setDrawMarkers(false)
        }

        // Line chart specific
        with(binding.lineChart) {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
            axisLeft.setDrawGridLines(false)
            xAxis.setDrawGridLines(false)
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
        val groupedData = transactions.groupBy {
            when (currentPeriod) {
                "DAILY" -> it.date
                "WEEKLY" -> "Week ${LocalDate.parse(it.date).getWeekOfMonth()}"
                else -> LocalDate.parse(it.date).month.toString()
            }
        }.mapValues { (_, values) -> values.sumOf { abs(it.amount) } }

        val entries = groupedData.entries.mapIndexed { index, entry ->
            Entry(index.toFloat(), entry.value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Spending Trend").apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
            lineWidth = 2f
        }

        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.invalidate()
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
                Color.rgb(255, 99, 132),  // Red
                Color.rgb(54, 162, 235),  // Blue
                Color.rgb(255, 206, 86)   // Yellow
            )
            valueTextColor = Color.BLACK
        }

        // Configure X-axis labels
        binding.barChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return labels.getOrNull(value.toInt()) ?: ""
                }
            }
            granularity = 1f
            setDrawGridLines(false)
        }

        binding.barChart.data = BarData(dataSet)
        binding.barChart.invalidate()
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
}