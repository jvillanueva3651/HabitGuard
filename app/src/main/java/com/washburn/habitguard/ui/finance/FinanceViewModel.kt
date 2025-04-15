package com.washburn.habitguard.ui.finance

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.washburn.habitguard.ui.calendar.TransactionType
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

class FinanceViewModel : ViewModel() {
    private val _balance = MutableLiveData(0.00)
    val balance: LiveData<Double> get() = _balance

    private val _transactions = MutableLiveData<List<Transaction>>(emptyList())
    val transactions: LiveData<List<Transaction>> get() = _transactions

    private val _currentPeriod = MutableLiveData(Period.WEEKLY)
    val currentPeriod: LiveData<Period> get() = _currentPeriod

    fun updateTransactions(newTransactions: List<Transaction>) {
        _transactions.value = newTransactions.sortedByDescending {
            "${it.date} ${it.time}".replace("-", "").replace(":", "")
        }
        calculateBalance()
    }

    private fun calculateBalance() {
        _balance.value = transactions.value?.sumOf {
            when(it.type) {
                TransactionType.INCOME -> it.amount
                TransactionType.EXPENSE -> -it.amount
                TransactionType.CREDIT -> 0.0 // Credits don't affect current balance
            }
        } ?: 0.0
    }

    fun setPeriod(period: Period) {
        _currentPeriod.value = period
        // TODO: Implement period filtering
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPeriodSummary(period: Period): Triple<Double, Double, Double> {
        val filtered = when(period) {
            Period.DAILY -> transactions.value?.filter { isToday(it.date) }
            Period.WEEKLY -> transactions.value?.filter { isThisWeek(it.date) }
            Period.MONTHLY -> transactions.value?.filter { isThisMonth(it.date) }
        } ?: emptyList()

        val income = filtered.sumOf { if(it.type == TransactionType.INCOME) it.amount else 0.0 }
        val expense = filtered.sumOf { if(it.type == TransactionType.EXPENSE) it.amount else 0.0 }
        val credit = filtered.sumOf { if(it.type == TransactionType.CREDIT) it.amount else 0.0 }

        return Triple(income, expense, credit)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isToday(date: String): Boolean {
        return LocalDate.parse(date) == LocalDate.now()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isThisWeek(date: String): Boolean {
        val inputDate = LocalDate.parse(date)
        val today = LocalDate.now()

        // Get the start and end of the current week (Sunday-based)
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val endOfWeek = startOfWeek.plusDays(6)

        return !inputDate.isBefore(startOfWeek) && !inputDate.isAfter(endOfWeek)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isThisMonth(date: String): Boolean {
        val inputDate = LocalDate.parse(date)
        val today = LocalDate.now()

        return inputDate.year == today.year &&
                inputDate.month == today.month
    }

    enum class Period { DAILY, WEEKLY, MONTHLY }
}