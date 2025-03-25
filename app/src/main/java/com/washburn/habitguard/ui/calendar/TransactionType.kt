package com.washburn.habitguard.ui.calendar

import com.washburn.habitguard.R

enum class TransactionType(val iconRes: Int, val colorRes: Int) {
    INCOME(R.drawable.ic_income, R.color.income_green_soft),
    EXPENSE(R.drawable.ic_expense, R.color.expense_red_soft),
    CREDIT(R.drawable.ic_credit, R.color.credit_yellow_soft)
}