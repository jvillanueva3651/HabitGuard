package com.washburn.habitguard.ui.finance

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.washburn.habitguard.R
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.databinding.FragmentFinanceBinding
import com.washburn.habitguard.ui.calendar.EventEditActivity
import com.washburn.habitguard.ui.finance.BudgetSetupActivity
import com.washburn.habitguard.ui.calendar.MonthlyViewActivity
import com.washburn.habitguard.ui.finance.Transaction
import com.washburn.habitguard.ui.calendar.TransactionType
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

class FinanceFragment : Fragment() {
    private lateinit var binding: FragmentFinanceBinding
    private val viewModel: FinanceViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFinanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTransactionList()
        setupButtonHandlers()
        setupBudgetSection()
        setupObservers()
        loadInitialData()
    }

    private fun setupTransactionList() {
        transactionAdapter = TransactionAdapter(mutableListOf()) { transaction ->
            // TODO: Add click handler for transaction details
        }

        binding.transactionRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupButtonHandlers() {
        binding.addTransactionButton.setOnClickListener {
            // Create intent with current date
            val intent = Intent(requireContext(), EventEditActivity::class.java).apply {

                putExtra("date", LocalDate.now().toString())
                putExtra("isTransaction", true)
            }
            startActivity(intent)
        }

        binding.scanReceiptButton.setOnClickListener {
            // TODO: Implement receipt scanning logic
            Toast.makeText(context, "Receipt scanning placeholder", Toast.LENGTH_SHORT).show()
        }

        binding.budgetAnalysisButton.setOnClickListener {
            startActivity(Intent(requireContext(), BudgetAnalysisActivity::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.updateTransactions(transactions)
            loadBudgetData()

            // Calculate current month's balance
            val currentMonthTransactions = filterTransactionsByCurrentMonth(transactions)
            val balance = calculateNetBalance(currentMonthTransactions)
            binding.balanceTextView.text = getString(R.string.balance_format, balance)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadInitialData() {
        lifecycleScope.launch {
            FirestoreHelper().getAllUserTransactions(
                onSuccess = { transactions ->
                    val converted = transactions.mapNotNull { (id, data) ->
                        try {
                            Transaction(
                                id = id,
                                amount = (data["amount"] as? Double) ?: 0.0,
                                type = TransactionType.valueOf(
                                    data["transactionType"] as? String ?: TransactionType.EXPENSE.name
                                ),
                                date = data["date"] as? String ?: LocalDate.now().toString(),
                                time = data["transactionTime"] as? String ?: "00:00",
                                tags = data["tags"] as? List<String> ?: emptyList(),
                                location = data["location"] as? String ?: "",
                                notes = data["description"] as? String ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e("FinanceFragment", "Error converting transaction $id", e)
                            null // Skip invalid transactions
                        }
                    }
                    viewModel.updateTransactions(converted)
                },
                onFailure = { e ->
                    Toast.makeText(context, "Error loading transactions", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // TODO: Add period selection logic


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupBudgetSection() {
        binding.setBudgetButton.setOnClickListener {
            startActivity(Intent(requireContext(), BudgetSetupActivity::class.java))
        }

        binding.editBudgetButton.setOnClickListener {
            startActivity(Intent(requireContext(), BudgetSetupActivity::class.java))
        }

        loadBudgetData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadBudgetData() {
        FirestoreHelper().getUserBudget(
            onSuccess = { budget ->
                budget?.let { (amount, period) ->
                    binding.budgetSetupLayout.visibility = View.GONE
                    binding.budgetProgressLayout.visibility = View.VISIBLE

                    val transactions = viewModel.transactions.value ?: emptyList()
                    val filteredTransactions = filterTransactionsByPeriod(transactions, period)
                        .filter { it.type == TransactionType.EXPENSE } // Only expenses

                    val totalSpent = filteredTransactions.sumOf { abs(it.amount) } // Force positive

                    val progress = calculateProgress(totalSpent, amount)
                    updateProgressUI(totalSpent, amount, progress)

                } ?: run {
                    binding.budgetSetupLayout.visibility = View.VISIBLE
                    binding.budgetProgressLayout.visibility = View.GONE
                }
            },
            onFailure = { e ->
                Toast.makeText(context, "Error loading budget", Toast.LENGTH_SHORT).show()
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterTransactionsByPeriod(transactions: List<Transaction>, period: String): List<Transaction> {
        val now = LocalDate.now()
        return when (period.uppercase()) { // Handle case insensitivity
            "DAILY" -> transactions.filter {
                LocalDate.parse(it.date).isEqual(now)
            }
            "WEEKLY" -> transactions.filter {
                val date = LocalDate.parse(it.date)
                date.isAfter(now.minusWeeks(1)) || date.isEqual(now)
            }
            "MONTHLY" -> transactions.filter {
                val date = LocalDate.parse(it.date)
                date.month == now.month && date.year == now.year
            }
            else -> transactions
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterTransactionsByCurrentMonth(transactions: List<Transaction>): List<Transaction> {
        val now = LocalDate.now()
        return transactions.filter {
            val date = LocalDate.parse(it.date)
            date.month == now.month && date.year == now.year
        }
    }

    private fun calculateNetBalance(transactions: List<Transaction>): Double {
        return transactions.sumOf { transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> transaction.amount
                TransactionType.EXPENSE -> -transaction.amount
                else -> 0.0 // Handle other types if needed
            }
        }
    }

    private fun calculateProgress(spent: Double, budget: Double): Int {
        return if (budget > 0) (spent / budget * 100).toInt() else 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadBudgetData()
    }

    private fun updateProgressUI(spent: Double, budget: Double, progress: Int) {
        binding.budgetProgressBar.progress = progress
        binding.spentAmountText.text = getString(R.string.spent_format, spent)
        binding.remainingAmountText.text = getString(R.string.remaining_format, budget - spent)
        binding.budgetTotalText.text = getString(R.string.budget_total_format, budget, progress)
        binding.budgetProgressBar.animate().setDuration(800).start()
    }

    // TODO: Implement receipt scanning
    private fun setupReceiptScanning() {
        // Implementation outline:
        // 1. Request camera permissions
        // 2. Launch camera intent
        // 3. Process image with ML Kit Text Recognition?
        // 4. Parse amounts/dates from text
        // 5. Auto-fill transaction fields
    }

    // TODO: Implement detailed transaction view
    private fun showTransactionDetails(transaction: Transaction) {
        // startActivity(Intent(requireContext(), TransactionDetailActivity::class.java).apply {
        //     putExtra("TRANSACTION_ID", transaction.id)
        // })
    }
}