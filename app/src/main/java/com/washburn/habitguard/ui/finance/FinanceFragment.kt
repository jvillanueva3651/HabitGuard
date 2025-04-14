package com.washburn.habitguard.ui.finance

import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.washburn.habitguard.ui.calendar.MonthlyViewActivity
import com.washburn.habitguard.ui.finance.Transaction
import com.washburn.habitguard.ui.calendar.TransactionType
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    }

    private fun setupObservers() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.updateTransactions(transactions)
        }

        viewModel.balance.observe(viewLifecycleOwner) { balance ->
            binding.balanceTextView.text = getString(R.string.balance_format, balance)
        }

        // TODO: Add observers for period summary
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadInitialData() {
        lifecycleScope.launch {
            // Load transactions from Firestore
            FirestoreHelper().getAllUserTransactions(
                onSuccess = { transactions ->
                    val converted = transactions.map { (id, data) ->
                        Transaction(
                            id = id,
                            amount = data["amount"] as Double,
                            type = TransactionType.valueOf(data["transactionType"] as String),
                            date = data["date"] as String,
                            time = data["transactionTime"] as String,
                            tags = data["tags"] as? List<String> ?: emptyList(),
                            location = data["location"] as? String ?: "",
                            notes = data["description"] as? String ?: ""
                        )
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

        loadBudgetData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadBudgetData() {
        FirestoreHelper().getUserBudget(
            onSuccess = { budget ->
                budget?.let { (amount, period) ->
                    binding.budgetStatusText.text = "Budget: $${"%.2f".format(amount)} ($period)"
                    // Update progress bar and other UI elements
                } ?: run {
                    binding.budgetStatusText.text = getString(R.string.no_budget_message)
                }
            },
            onFailure = { e ->
                Toast.makeText(context, "Error loading budget", Toast.LENGTH_SHORT).show()
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadBudgetData()
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