package com.washburn.habitguard.ui.calendar

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R

class TransactionAdapter(
    private val context: Context,
    private var transactions: List<Pair<String, Map<String, Any>>>,
    private val firestoreHelper: FirestoreHelper,
    private val onEditClick: (String) -> Unit,
    private val onDeleteSuccess: () -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.transactionName)
        val amount: TextView = itemView.findViewById(R.id.transactionAmount)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditTransaction)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTransaction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (id, data) = transactions[position]
        holder.name.text = data["name"].toString()
        val amount = data["amount"] as? Double ?: 0.0
        val type = data["transactionType"] as? String ?: "INCOME"
        holder.amount.text = "$type: ${String.format("%.2f", amount)}"

        holder.btnEdit.setOnClickListener { onEditClick(id) }
        holder.btnDelete.setOnClickListener {
            firestoreHelper.deleteUserTransaction(
                transactionId = id,
                onSuccess = onDeleteSuccess,
                onFailure = { e -> Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Pair<String, Map<String, Any>>>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}