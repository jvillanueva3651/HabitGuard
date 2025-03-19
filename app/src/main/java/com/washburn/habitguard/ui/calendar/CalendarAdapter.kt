package com.washburn.habitguard.ui.calendar

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.R
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class CalendarAdapter(
    private val days: ArrayList<LocalDate>,
    private val onItemListener: OnItemListener
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    interface OnItemListener {
        fun onItemClick(position: Int, date: LocalDate)
    }

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val dayText: TextView = itemView.findViewById(R.id.cellDayText)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            onItemListener.onItemClick(adapterPosition, days[adapterPosition])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.calendar_cell, parent, false)
        return CalendarViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val date = days[position]
        holder.dayText.text = date.dayOfMonth.toString()
    }

    override fun getItemCount(): Int {
        return days.size
    }
}