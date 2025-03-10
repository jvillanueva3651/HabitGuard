package com.washburn.habitguard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(
    private val habits: MutableList<Habit>,
    private val listener: OnHabitActionListener
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    interface OnHabitActionListener {
        fun onEdit(habit: Habit, position: Int)
        fun onDelete(habit: Habit, position: Int)
    }

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val habitName: TextView = itemView.findViewById(R.id.habitName)
        val habitDescription: TextView = itemView.findViewById(R.id.habitDescription)
        val btnEditHabit: Button = itemView.findViewById(R.id.btnEditHabit)
        val btnDeleteHabit: Button = itemView.findViewById(R.id.btnDeleteHabit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.habitName.text = habit.name
        holder.habitDescription.text = habit.description

        holder.btnEditHabit.setOnClickListener {
            listener.onEdit(habit, position)
        }

        holder.btnDeleteHabit.setOnClickListener {
            listener.onDelete(habit, position)
        }
    }

    override fun getItemCount(): Int = habits.size
}

