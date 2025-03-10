package com.washburn.habitguard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(
    private val dates: List<String>,
    // Map from date string to list of habits for that date.
    private val habitsMap: Map<String, List<Habit>>,
    private val onDateClick: (String) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
        // View to show an indicator if habits exist
        val indicator: View = view.findViewById(R.id.habitIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = dates[position]
        // Extract day number from date string (assumes "yyyy-MM-dd")
        val dayNumber = date.split("-").last().toIntOrNull() ?: 0
        holder.dateText.text = dayNumber.toString()

        // Show indicator if there are habits for that date.
        val habitsForDate = habitsMap[date]
        holder.indicator.visibility = if (habitsForDate != null && habitsForDate.isNotEmpty()) View.VISIBLE else View.INVISIBLE

        holder.itemView.setOnClickListener { onDateClick(date) }
    }

    override fun getItemCount() = dates.size
}
