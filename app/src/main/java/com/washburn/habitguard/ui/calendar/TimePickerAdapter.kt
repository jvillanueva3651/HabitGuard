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
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("DefaultLocale")
class TimePickerAdapter(
    private val onTimeSelected: (LocalTime) -> Unit
) : RecyclerView.Adapter<TimePickerAdapter.TimeViewHolder>() {

    private var hours = 0
    private var minutes = 0

    inner class TimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val hourTextView: TextView = itemView.findViewById(R.id.hourTextView)
        private val minuteTextView: TextView = itemView.findViewById(R.id.minuteTextView)

        fun bind(hours: Int, minutes: Int) {
            hourTextView.text = String.format("%02d", hours)
            minuteTextView.text = String.format("%02d", minutes)

            // Handle hour selection
            hourTextView.setOnClickListener {
                this@TimePickerAdapter.hours = (this@TimePickerAdapter.hours + 1) % 24
                notifyItemChanged(adapterPosition)
                onTimeSelected(LocalTime.of(this@TimePickerAdapter.hours, minutes))
            }

            // Handle minute selection
            minuteTextView.setOnClickListener {
                this@TimePickerAdapter.minutes = (this@TimePickerAdapter.minutes + 1) % 60
                notifyItemChanged(adapterPosition)
                onTimeSelected(LocalTime.of(hours, this@TimePickerAdapter.minutes))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time, parent, false)
        return TimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        holder.bind(hours, minutes)
    }

    override fun getItemCount(): Int = 1 // Only one item for simplicity
}