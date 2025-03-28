//=======================================================================================
// "EventAdapter" is the backend for creating, editing, and deleting habits
// Refer to     "~/FirestoreHelper" for database operations
//              "./ui/calendar/EventEditActivity" to take input from and create habits card for ./ui/calendar/* Week TODO: , Calendar and daily.
//
// Get layout of card from "layout/event_cell.xml"
//
// Fun: 1. Load habits from database
//      2. Handle from EventEditActivity with the others ViewActivity
//=======================================================================================
package com.washburn.habitguard.ui.calendar

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.Toast
import android.content.Context
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.R
import com.washburn.habitguard.FirestoreHelper
import com.washburn.habitguard.databinding.EventCellBinding
import com.washburn.habitguard.ui.calendar.CalendarUtils.formattedTimeFromString
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.O)
class EventAdapter(
    private val context: Context,
    private val events: List<Pair<String, Map<String, Any>>>,
    private val firestoreHelper: FirestoreHelper,
    private val onEditClick: (String) -> Unit,
    private val onDeleteSuccess: () -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(private val binding: EventCellBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Pair<String, Map<String, Any>>) {
            val (documentId, eventData) = event

            binding.eventTitleTV.text = eventData["name"] as? String ?: ""
            binding.eventDateTV.text = eventData["date"] as? String ?: ""
            binding.eventStartTimeTV.text = formattedTimeFromString(eventData["startTime"] as? String ?: "00:00")
            binding.eventEndTimeTV.text = formattedTimeFromString(eventData["endTime"] as? String ?: "00:00")
            binding.eventDescriptionTV.text = eventData["description"] as? String ?: ""
            binding.eventLocationTV.text = eventData["location"] as? String ?: ""

            val isTransaction = eventData["isTransaction"] as? Boolean ?: false
            val amount = eventData["amount"] as? Double ?: 0.0
            val transactionType = eventData["transactionType"] as? String ?: ""

            if (isTransaction && amount != 0.0) {
                binding.transactionInfoLayout.visibility = View.VISIBLE
                binding.transactionAmountTV.text =
                    if (amount < 0) "-$${"%.2f".format(abs(amount))}"
                    else "$${"%.2f".format(amount)}"

                val iconRes = when (transactionType) {
                    "INCOME" -> R.drawable.ic_income
                    "EXPENSE" -> R.drawable.ic_expense
                    "CREDIT" -> R.drawable.ic_credit
                    else -> R.drawable.ic_event
                }
                binding.transactionTypeIcon.setImageResource(iconRes)
            } else {
                binding.transactionInfoLayout.visibility = View.GONE
            }

            binding.editButton.setOnClickListener { onEditClick(documentId) }
            binding.deleteButton.setOnClickListener { deleteHabit(documentId) }
        }

        private fun deleteHabit(documentId: String) {
            firestoreHelper.deleteUserHabit(
                documentId,
                onSuccess = {
                    Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show()
                    onDeleteSuccess()
                },
                onFailure = { e ->
                    Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = EventCellBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size
}