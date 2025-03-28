package com.washburn.habitguard.ui.calendar

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.HourCellBinding
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class HourAdapter(
    context: Context,
    hourlyEvents: List<FirestoreHelper.HourlyEventData>
) : ArrayAdapter<FirestoreHelper.HourlyEventData>(context, 0, hourlyEvents) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val hourEvent = getItem(position)!!
        val binding = if (convertView != null) {
            HourCellBinding.bind(convertView)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.hour_cell, parent, false)
            HourCellBinding.bind(view)
        }

        setHour(binding, hourEvent.timeSlot)
        setEvents(binding, hourEvent.events)

        return binding.root
    }

    private fun setHour(binding: HourCellBinding, time: LocalTime) {
        binding.timeTV.text = time.format(DateTimeFormatter.ofPattern("h a"))
    }

    private fun setEvents(binding: HourCellBinding, events: List<FirestoreHelper.EventData>) {
        val eventViews = listOf(binding.event1, binding.event2, binding.event3)

        events.take(3).forEachIndexed { index, event ->
            eventViews[index].apply {
                val formattedTime =
                    LocalTime.parse(event.time).format(DateTimeFormatter.ofPattern("h:mm a"))
                eventViews[index].apply {
                    text = context.getString(
                        R.string.event_with_time_format,
                        event.name,
                        formattedTime
                    )
                    visibility = View.VISIBLE
                }
            }

            if (events.size > 3) {
                binding.event3.text = context.resources.getQuantityString(
                    R.plurals.more_events_count,
                    events.size - 2,
                    events.size - 2
                )
                binding.event3.visibility = View.VISIBLE
            } else if (events.size < 3) {
                eventViews.slice(events.size until 3).forEach {
                    it.visibility = View.INVISIBLE
                }
            }
        }
    }
}