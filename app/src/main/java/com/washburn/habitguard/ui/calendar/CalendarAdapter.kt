package com.washburn.habitguard.ui.calendar

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.R
import java.time.LocalDate
import java.util.ArrayList

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("NotifyDataSetChanged")
class CalendarAdapter(
    private val days: ArrayList<LocalDate>,
    private val onItemListener: OnItemListener,
    private val events: List<Map<String, String>>

) : RecyclerView.Adapter<CalendarViewHolder>() {

    interface OnItemListener {
        fun onItemClick(position: Int, date: LocalDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.calendar_cell, parent, false)

        val displayMetrics = parent.context.resources.displayMetrics
        val cellWidth = displayMetrics.widthPixels / 7
        val cellHeight = (cellWidth * 1.2).toInt()

        // Set fixed height for each cell (1/6th of parent height)
        view.layoutParams = RecyclerView.LayoutParams(
            cellWidth,
            cellHeight
        )

        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val date = days[position]
        val hasEvents = events.any { it["date"] == date.toString() }

        with(holder) {
            val dayOfTheMonthText = date.dayOfMonth.toString()
            dayOfMonth.text = dayOfTheMonthText

            // Styling
            dayOfMonth.setTextColor(
                if (date.month == CalendarUtils.selectedDate.month) Color.BLACK else Color.LTGRAY
            )

            parentView.setBackgroundColor(
                if (date == CalendarUtils.selectedDate) Color.LTGRAY else Color.TRANSPARENT
            )

            eventIndicator.visibility = if (hasEvents) View.VISIBLE else View.INVISIBLE

            itemView.setOnClickListener {
                onItemListener.onItemClick(position, date)
                notifyDataSetChanged() // Refresh highlights
            }
        }
    }

    override fun getItemCount(): Int = days.size
}