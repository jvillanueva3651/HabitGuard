package com.washburn.habitguard.ui.finance

import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.databinding.TransactionItemBinding
import com.washburn.habitguard.ui.finance.Transaction
import com.washburn.habitguard.ui.calendar.TransactionType


class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(val binding: TransactionItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TransactionItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.binding.apply {
            amountText.text = when(transaction.type) {
                TransactionType.INCOME -> "+$${"%.2f".format(transaction.amount)}"
                TransactionType.EXPENSE -> "-$${"%.2f".format(transaction.amount)}"
                TransactionType.CREDIT -> "$${"%.2f".format(transaction.amount)} (Credit)"
            }
            dateText.text = "${transaction.date} ${transaction.time}"
            categoryText.text = transaction.tags.joinToString(", ")
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}