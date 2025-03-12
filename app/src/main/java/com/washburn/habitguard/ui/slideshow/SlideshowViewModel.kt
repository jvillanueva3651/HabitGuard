package com.washburn.habitguard.ui.slideshow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SlideshowViewModel : ViewModel() {

    private val _balance = MutableLiveData(0.00)
    val balance: LiveData<Double> get() = _balance

    // LiveData for transaction history
    private val _transactions = MutableLiveData<MutableList<String>>(mutableListOf())
    val transactions: LiveData<MutableList<String>> get() = _transactions

    // Add a transaction (income or expense)
    fun addTransaction(isIncome: Boolean, amount: Double) {
        if (isIncome) {
            _balance.value = (_balance.value ?: 0.00) + amount
            _transactions.value?.add(0, "Income: +$${"%.2f".format(amount)}")
        } else {
            if (_balance.value ?: 0.00 >= amount) {
                _balance.value = (_balance.value ?: 0.00) - amount
                _transactions.value?.add(0, "Expense: -$${"%.2f".format(amount)}")
            } else {
                _transactions.value?.add(0, "Failed Expense: -$${"%.2f".format(amount)} (Insufficient funds)")
            }
        }
        _transactions.value = _transactions.value // Notify observers
    }
}