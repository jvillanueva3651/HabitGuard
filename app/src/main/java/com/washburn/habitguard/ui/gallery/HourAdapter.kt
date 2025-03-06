package com.washburn.habitguard.ui.gallery

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.washburn.habitguard.R
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
class HourAdapter(context: Context, hourEvents: List<HourEvent>) :
    ArrayAdapter<HourEvent>(context, 0, hourEvents) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val event = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.hour_cell, parent, false)

        setHour(view, event!!.time)
        setEvents(view, event.events)

        return view
    }

    private fun setHour(convertView: View, time: LocalTime) {
        val timeTV: TextView = convertView.findViewById(R.id.timeTV)
        timeTV.text = CalendarUtils.formattedShortTime(time)
    }

    private fun setEvents(convertView: View, events: List<Event>) {
        val event1: TextView = convertView.findViewById(R.id.event1)
        val event2: TextView = convertView.findViewById(R.id.event2)
        val event3: TextView = convertView.findViewById(R.id.event3)

        when (events.size) {
            0 -> {
                hideEvent(event1)
                hideEvent(event2)
                hideEvent(event3)
            }
            1 -> {
                setEvent(event1, events[0])
                hideEvent(event2)
                hideEvent(event3)
            }
            2 -> {
                setEvent(event1, events[0])
                setEvent(event2, events[1])
                hideEvent(event3)
            }
            3 -> {
                setEvent(event1, events[0])
                setEvent(event2, events[1])
                setEvent(event3, events[2])
            }
            else -> {
                setEvent(event1, events[0])
                setEvent(event2, events[1])
                event3.visibility = View.VISIBLE
                event3.text = "${events.size - 2} More Events"
            }
        }
    }

    private fun setEvent(textView: TextView, event: Event) {
        textView.text = event.name
        textView.visibility = View.VISIBLE
    }

    private fun hideEvent(tv: TextView) {
        tv.visibility = View.INVISIBLE
    }
}