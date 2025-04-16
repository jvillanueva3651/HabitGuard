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
    private val habits: List<Map<String, String>>,
    private val transactions: List<Map<String, String>>

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

        // Set fixed height for each cell
        view.layoutParams = RecyclerView.LayoutParams(
            cellWidth,
            cellHeight
        )

        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val date = days[position]
        val habitCount = habits.count { it["date"] == date.toString() }
        val transactionCount = transactions.count { it["date"] == date.toString() }

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

            if (habitCount > 0) {
                habitIndicator.visibility = View.VISIBLE
                habitIndicator.setBackgroundResource(getHabitIndicatorResource(habitCount))
            } else {
                habitIndicator.visibility = View.INVISIBLE
            }

            if (transactionCount > 0) {
                transactionIndicator.visibility = View.VISIBLE
                transactionIndicator.setBackgroundResource(getTransactionIndicatorResource(transactionCount))
            } else {
                transactionIndicator.visibility = View.INVISIBLE
            }

            itemView.setOnClickListener {
                onItemListener.onItemClick(position, date)
                notifyDataSetChanged() // Refresh highlights
            }
        }
    }

    private fun getHabitIndicatorResource(count: Int): Int {
        return when (count) {
            1 -> R.drawable.ic_habit_1
            2 -> R.drawable.ic_habit_2
            3 -> R.drawable.ic_habit_3
            4 -> R.drawable.ic_habit_4
            5 -> R.drawable.ic_habit_5
            6 -> R.drawable.ic_habit_6
            7 -> R.drawable.ic_habit_7
            8 -> R.drawable.ic_habit_8
            9 -> R.drawable.ic_habit_9
            else -> R.drawable.ic_indicator
        }
    }

    private fun getTransactionIndicatorResource(count: Int): Int {
        return when (count) {
            1 -> R.drawable.ic_transaction_1
            2 -> R.drawable.ic_transaction_2
            3 -> R.drawable.ic_transaction_3
            4 -> R.drawable.ic_transaction_4
            5 -> R.drawable.ic_transaction_5
            6 -> R.drawable.ic_transaction_6
            else -> R.drawable.ic_indicator
        }
    }

    override fun getItemCount(): Int = days.size
}