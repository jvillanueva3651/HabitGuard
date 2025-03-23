package com.washburn.habitguard.ui.calendar

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.os.Build
import androidx.annotation.RequiresApi
import com.washburn.habitguard.R

@RequiresApi(Build.VERSION_CODES.O)
class EventAdapter(context: Context, events: List<Event>) :
    ArrayAdapter<Event>(context, 0, events) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val event = getItem(position)

        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.event_cell, parent, false)

        val eventCellTV = view.findViewById<TextView>(R.id.eventCellTV)
        val eventCompleteDateTimeCellTV = view.findViewById<TextView>(R.id.eventCompleteDateTimeCellTV)
        val eventDescCellTV = view.findViewById<TextView>(R.id.eventDescCellTV)


        if (event != null) {
            val eventTitle = event.eventName
            val eventTime = "${CalendarUtils.formattedDate(event.date)} ${CalendarUtils.formattedTime(event.time)}"
            val eventDescription = event.eventDesc

            eventCellTV.text = eventTitle
            eventCompleteDateTimeCellTV.text = eventTime
            eventDescCellTV.text = eventDescription
        }

        return view
    }
}