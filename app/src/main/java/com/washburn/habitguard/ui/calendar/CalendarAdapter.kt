package com.washburn.habitguard.ui.calendar

import java.time.LocalDate
import java.util.ArrayList
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.graphics.Color
import android.annotation.SuppressLint
import android.view.View
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.R
import com.washburn.habitguard.ui.calendar.CalendarUtils.selectedDate

@RequiresApi(Build.VERSION_CODES.O)
class CalendarAdapter(
    private val days: ArrayList<LocalDate>,
    private val onItemListener: OnItemListener,

    private val events: List<Event>
) : RecyclerView.Adapter<CalendarViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.calendar_cell, parent, false)
        val layoutParams = view.layoutParams

        layoutParams.height = if (days.size > 15) {
            (parent.height * 0.166666666).toInt()
        } else {
            parent.height
        }

        return CalendarViewHolder(view, onItemListener, days)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val date = days[position]

        holder.dayOfMonth.text = date.dayOfMonth.toString()

        if (date == selectedDate) {
            holder.parentView.setBackgroundColor(Color.LTGRAY)
        } else {
            holder.parentView.setBackgroundColor(Color.TRANSPARENT)
        }

        if (date.month == selectedDate.month) {
            holder.dayOfMonth.setTextColor(Color.BLACK)
        } else {
            holder.dayOfMonth.setTextColor(Color.LTGRAY)
        }

        if (hasEvent(date)) {
            holder.eventIndicator.visibility = View.VISIBLE
        } else {
            holder.eventIndicator.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return days.size
    }

    private fun hasEvent(date: LocalDate): Boolean {
        return events.any { it.date == date }
    }

    interface OnItemListener {
        fun onItemClick(position: Int, date: LocalDate)
    }
}