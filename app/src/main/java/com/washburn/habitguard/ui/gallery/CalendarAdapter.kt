package com.washburn.habitguard.ui.gallery

import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.R
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class CalendarAdapter(
    private val days: List<LocalDate?>,
    private val onItemListener: OnItemListener
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

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val date = days[position]
        holder.dayOfMonth.text = date!!.dayOfMonth.toString()

        if (date == CalendarUtils.selectedDate) {
            holder.parentView.setBackgroundColor(Color.LTGRAY)
        }

        holder.dayOfMonth.setTextColor(
            if (date!!.month == CalendarUtils.selectedDate.month) Color.BLACK else Color.LTGRAY
        )
    }

    override fun getItemCount(): Int {
        return days.size
    }

    interface OnItemListener {
        fun onItemClick(position: Int, date: LocalDate)
    }
}