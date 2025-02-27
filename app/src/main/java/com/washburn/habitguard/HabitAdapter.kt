package com.washburn.habitguard

class HabitAdapter(
    private val habits: List<Habit>,
    private val onHabitClick: (Habit) -> Unit
) : RecyclerView.Adapter<HabitAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val habitName: TextView = view.findViewById(R.id.habitName)
        val habitCheckbox: CheckBox = view.findViewById(R.id.habitCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val habit = habits[position]
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        holder.habitName.text = habit.name
        holder.habitCheckbox.isChecked = habit.completedDates.contains(today)

        holder.habitCheckbox.setOnCheckedChangeListener { _, isChecked ->
            onHabitClick(habit)
        }
    }

    override fun getItemCount() = habits.size
}