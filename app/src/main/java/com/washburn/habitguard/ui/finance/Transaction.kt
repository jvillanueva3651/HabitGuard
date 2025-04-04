package com.washburn.habitguard.ui.finance

import com.washburn.habitguard.ui.calendar.TransactionType

/**
 * Data class for Transactions
 */

data class Transaction(
    val id: String = "",    //The ID of the document in FireStore
    val amount: Double,     //The amount of the transaction
    val type: TransactionType,  //The type, copied from the TransactionType enum class
    val date: String,       //The date in the format: "yyyy-MM-dd"
    val time: String,       //The time in the format: "HH:mm"
    val tags: List<String> = emptyList(),   //Tags for category tracking i.e. Groceries, Restaurant, etc.
    val notes: String = ""  //User notes for specified transaction
)
