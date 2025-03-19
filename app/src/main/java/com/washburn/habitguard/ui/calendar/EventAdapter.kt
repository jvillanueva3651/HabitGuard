package com.washburn.habitguard.ui.calendar

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.washburn.habitguard.R

@RequiresApi(Build.VERSION_CODES.O)
class EventAdapter(context: Context, events: List<Event>) :
    ArrayAdapter<Event>(context, 0, events) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val event = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.event_cell, parent, false)

        val eventCellTV = view.findViewById<TextView>(R.id.eventCellTV)
        val eventTitle = "${event?.name} ${CalendarUtils.formattedTime(event?.time)}"
        eventCellTV.text = eventTitle

        return view
    }
}