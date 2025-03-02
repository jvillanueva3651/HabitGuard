package com.washburn.habitguard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(
    private val dates: List<String>,
    private val habits: List<Habit>,
    private val onDateClick: (String) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
        val indicatorsLayout: ViewGroup = view.findViewById(R.id.habitIndicators)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = dates[position]
        val dayNumber = date.split("-").last().toInt()

        // Set date text
        holder.dateText.text = dayNumber.toString()

        // Clear previous indicators
        holder.indicatorsLayout.removeAllViews()

        // Add habit indicators
        habits.filter { it.completedDates.contains(date) }.forEach { habit ->
            val indicator = View(holder.itemView.context).apply {
                layoutParams = ViewGroup.LayoutParams(12, 12)
                background = ContextCompat.getDrawable(context, R.drawable.indicator_circle)
                background.setTColor(ContextCompat.getColor(context, getColorForHabit(habit)))
            }
            holder.indicatorsLayout.addView(indicator)
        }

        holder.itemView.setOnClickListener { onDateClick(date) }
    }

    private fun getColorForHabit(habit: Habit): Int {
        return when (habit.type) {
            "daily" -> R.color.daily_habit
            "weekly" -> R.color.weekly_habit
            else -> R.color.monthly_habit
        }
    }

    override fun getItemCount() = dates.size
}