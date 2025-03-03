package com.washburn.habitguard.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.HabitListActivity
import com.washburn.habitguard.databinding.FragmentGalleryBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class GalleryFragment : Fragment(), CalendarAdapter.OnItemListener {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarViewModel: GalleryViewModel
    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var selectedDate: LocalDate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        calendarViewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initWidgets()
        selectedDate = LocalDate.now()
        setMonthView()

        binding.previousMonthAction.setOnClickListener {
            previousMonthAction(it)
        }
        binding.nextMonthAction.setOnClickListener {
            nextMonthAction(it)
        }


        return root
    }

    private fun initWidgets() {
        calendarRecyclerView = binding.calendarRecyclerView
        monthYearText = binding.monthYearTV
    }

    private fun setMonthView() {
        monthYearText.text = monthYearFromDate(selectedDate)
        val daysInMonth = daysInMonthArray(selectedDate)

        val calendarAdapter = CalendarAdapter(daysInMonth, this)
        val layoutManager = GridLayoutManager(requireContext(), 7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
    }

    private fun daysInMonthArray(date: LocalDate): ArrayList<String> {
        val daysInMonthArray = ArrayList<String>()
        val yearMonth = YearMonth.from(date)
        val daysInMonth = yearMonth.lengthOfMonth()

        val firstOfMonth = selectedDate.withDayOfMonth(1)
        val dayOfWeek = firstOfMonth.dayOfWeek.value

        for (i in 1..42) {
            if (i <= dayOfWeek || i > daysInMonth + dayOfWeek) {
                daysInMonthArray.add("")
            } else {
                daysInMonthArray.add((i - dayOfWeek).toString())
            }
        }
        return daysInMonthArray
    }

    private fun monthYearFromDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        return date.format(formatter)
    }

    fun previousMonthAction(view: View) {
        selectedDate = selectedDate.minusMonths(1)
        setMonthView()
    }

    fun nextMonthAction(view: View) {
        selectedDate = selectedDate.plusMonths(1)
        setMonthView()
    }

    override fun onItemClick(position: Int, dayText: String) {
        if (dayText.isNotEmpty()) {
            // Convert dayText to an integer.
            val day = dayText.toIntOrNull()
            if (day != null) {
                // Combine with the month and year from selectedDate.
                // selectedDate is the LocalDate for the currently displayed month.
                val fullDate = selectedDate.withDayOfMonth(day)
                // Format the full date as a string; using ISO-8601 (YYYY-MM-DD) is a good choice.
                val formattedDate = fullDate.toString()  // e.g., "2025-03-15"

                // Create an Intent to launch HabitListActivity.
                val intent = Intent(requireContext(), HabitListActivity::class.java)
                intent.putExtra("selectedDate", formattedDate)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Invalid day selected", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}