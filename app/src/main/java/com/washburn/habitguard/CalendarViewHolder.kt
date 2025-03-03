package com.washburn.habitguard

import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.util.ArrayList

@RequiresApi(Build.VERSION_CODES.O)
class CalendarViewHolder(
    itemView: View,
    private val onItemListener: CalendarAdapter.OnItemListener,
    private val days: ArrayList<LocalDate>
) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    val parentView: View = itemView.findViewById(R.id.parentView)
    val dayOfMonth: TextView = itemView.findViewById(R.id.cellDayText)

    init {
        itemView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        onItemListener.onItemClick(adapterPosition, days[adapterPosition])
    }
}