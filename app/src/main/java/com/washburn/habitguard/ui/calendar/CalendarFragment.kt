package com.washburn.habitguard.ui.calendar

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.washburn.habitguard.databinding.FragmentCalendarBinding
import com.washburn.habitguard.ui.calendar.CalendarUtils.daysInMonthArray
import com.washburn.habitguard.ui.calendar.CalendarUtils.monthYearFromDate
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class CalendarFragment : Fragment(), CalendarAdapter.OnItemListener {

    private lateinit var binding: FragmentCalendarBinding
    private lateinit var viewModel: CalendarViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CalendarViewModel::class.java]

        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            setMonthView(date)
        }

        initWidgets()

        setMonthView(viewModel.selectedDate.value ?: LocalDate.now())

        binding.previousMonthButton.setOnClickListener {
            viewModel.goToPreviousMonth()
        }

        binding.nextMonthButton.setOnClickListener {
            viewModel.goToNextMonth()
        }

        binding.weeklyActionButton.setOnClickListener {
            weeklyAction()
        }

    }

    private fun initWidgets() {
        val calendarRecyclerView = binding.calendarRecyclerView

        calendarRecyclerView.layoutManager = GridLayoutManager(requireContext(), 7)
    }

    private fun setMonthView(date: LocalDate) {
        binding.monthYearTV.text = monthYearFromDate(date)
        val daysInMonth = daysInMonthArray(date)

        val calendarAdapter = CalendarAdapter(daysInMonth, this)

        binding.calendarRecyclerView.adapter = calendarAdapter
    }

    override fun onItemClick(position: Int, date: LocalDate) {
        date.let {
            viewModel.setSelectedDate(it)
        }
    }

    private fun weeklyAction() {
        requireActivity().startActivity(Intent(requireContext(), WeekViewActivity::class.java))
    }
}