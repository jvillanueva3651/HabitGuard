package com.washburn.habitguard.ui.calendar

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.R

class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val dayOfMonth: TextView = itemView.findViewById(R.id.cellDayText)
    val habitIndicator: View = itemView.findViewById(R.id.habitIndicator)
    val transactionIndicator: View = itemView.findViewById(R.id.transactionIndicator)
    val parentView: View = itemView.findViewById(R.id.parentView)
}