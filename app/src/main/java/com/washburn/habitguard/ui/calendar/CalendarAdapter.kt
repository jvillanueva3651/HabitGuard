package com.washburn.habitguard.ui.calendar

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.R
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class CalendarAdapter(
    private val daysInMonth: List<LocalDate>,
    private val onItemListener: OnItemListener
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    interface OnItemListener {
        fun onItemClick(position: Int, date: LocalDate)
    }

    private var selectedPosition = -1
    private val dotIndicators = mutableSetOf<LocalDate>()

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val dayText: TextView = itemView.findViewById(R.id.cellDayText)
        val dotIndicator: TextView = itemView.findViewById(R.id.cellDotIndicator)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val previousPosition = selectedPosition
            selectedPosition = adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onItemListener.onItemClick(adapterPosition, daysInMonth[adapterPosition])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.calendar_cell, parent, false)
        return CalendarViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val date = daysInMonth[position]
        holder.dayText.text = date.dayOfMonth.toString()

        // Highlight the selected date
        if (position == selectedPosition) {
            holder.dayText.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.teal_200))
            holder.dayText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
        } else if (date == LocalDate.now()) {
            holder.dayText.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
            holder.dayText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.teal_200))
        } else {
            holder.dayText.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.transparent))
            holder.dayText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
        }

        // Show/hide dot indicator
        if (dotIndicators.contains(date)) {
            holder.dotIndicator.visibility = View.VISIBLE
        } else {
            holder.dotIndicator.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return daysInMonth.size
    }

    fun addDotIndicator(date: LocalDate) {
        dotIndicators.add(date)
        notifyDataSetChanged()
    }
}