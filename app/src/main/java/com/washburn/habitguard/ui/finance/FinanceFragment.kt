package com.washburn.habitguard.ui.finance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.washburn.habitguard.R

class FinanceFragment : Fragment() {

    private val viewModel: FinanceViewModel by viewModels()

    private lateinit var balanceTextView: TextView
    private lateinit var transactionListView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_finance, container, false)

        // Initialize views
        balanceTextView = view.findViewById(R.id.balanceTextView)
        transactionListView = view.findViewById(R.id.transactionListView)

        // Set up the transaction list adapter
        val transactionAdapter = TransactionAdapter()
        transactionListView.adapter = transactionAdapter

        // Observe balance changes
        viewModel.balance.observe(viewLifecycleOwner, Observer { balance ->
            balanceTextView.text = "Balance: $${"%.2f".format(balance)}"
            when {
                balance < 0 -> balanceTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                balance > 0 -> balanceTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                else -> balanceTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow))
            }
        })

        // Observe transaction history changes
        viewModel.transactions.observe(viewLifecycleOwner, Observer { transactions ->
            transactionAdapter.updateTransactions(transactions)
        })

        return view
    }

    // Custom Adapter for the ListView
    inner class TransactionAdapter : BaseAdapter() {

        private val transactions = mutableListOf<String>()

        fun updateTransactions(newTransactions: List<String>) {
            transactions.clear()
            transactions.addAll(newTransactions)
            notifyDataSetChanged()
        }

        override fun getCount(): Int = transactions.size + 1 // +1 for the input row

        override fun getItem(position: Int): Any = if (position == 0) "input" else transactions[position - 1]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return if (position == 0) {
                // Input row
                val inputView = layoutInflater.inflate(R.layout.list_item_transaction, parent, false)
                val typeSwitch = inputView.findViewById<Switch>(R.id.typeSwitch)
                val amountInput = inputView.findViewById<EditText>(R.id.amountInput)
                val confirmButton = inputView.findViewById<Button>(R.id.confirmButton)

                confirmButton.setOnClickListener {
                    val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.00
                    if (amount > 0) {
                        viewModel.addTransaction(typeSwitch.isChecked, amount)
                        amountInput.text.clear()
                    } else {
                        Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
                    }
                }

                inputView
            } else {
                // Transaction row
                val transactionView = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                val textView = transactionView.findViewById<TextView>(android.R.id.text1)
                val transaction = transactions[position - 1]
                textView.text = transaction

                // Set text color based on transaction type
                when {
                    transaction.startsWith("Income") -> textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                    transaction.startsWith("Expense") -> textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    transaction.startsWith("Failed Expense") -> textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow))
                }

                transactionView
            }
        }
    }
}