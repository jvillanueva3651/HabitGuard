/**===========================================================================================
 * EventAdapter for displaying and managing habit/event cards
 * REF    : USE_BY -> .MonthlyViewActivity and .WeekViewActivity
 *          USING  -> ~/FirestoreHelper (database) & .EventEditActivity (editing)
 *          LAYOUT -> layout/event_cell.xml
 * Purpose: Bridges between Firestore data and calendar event displays
 * Features:
 *   1. Displays both events and transactions in unified view
 *   2. Handles edit/delete operations
 *   3. Formats time and currency values
 *   4. Manages click actions through callbacks
 *   5. Supports transaction type visualization
============================================================================================*/
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

//TODO: Fix EventAdapter to work around event versus transaction logic
// Currently, only events are displayed on the calendar views, NOT transactions
// What kind of logic do we want with that anyways? Show each transaction one by one or
// display a unified summation of transactions for that day??

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