package com.washburn.habitguard

class CalendarAdapter(
    private val dates: List<String>,
    private val onDateClick: (String) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = dates[position]
        holder.dateText.text = date.split("-").last() // Show just day number
        holder.itemView.setOnClickListener { onDateClick(date) }
    }

    override fun getItemCount() = dates.size
}