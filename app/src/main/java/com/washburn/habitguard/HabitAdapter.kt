package com.washburn.habitguard

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HabitAdapter(
    private val habits: List<Habit>,
    private val onHabitToggled: (Habit, Boolean) -> Unit
) : RecyclerView.Adapter<HabitAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val habitCheckbox: CheckBox = view.findViewById(R.id.habitCheckbox)
        val habitName: TextView = view.findViewById(R.id.habitName)
        val habitStreak: TextView = view.findViewById(R.id.habitStreak)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val habit = habits[position]
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        with(holder) {
            habitName.text = habit.name
            habitCheckbox.isChecked = habit.completedDates.contains(today)
            habitStreak.text = "Current streak: ${calculateStreak(habit.completedDates)} days"

            habitCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onHabitToggled(habit, isChecked)
            }
        }
    }

    private fun calculateStreak(dates: List<String>): Int {
        val sortedDates = dates.sortedDescending()
        var streak = 0
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        sortedDates.forEach { dateStr ->
            val date = dateFormat.parse(dateStr)!!
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            if (date.after(calendar.time)) streak++ else return streak
        }
        return streak
    }

    override fun getItemCount() = habits.size
}