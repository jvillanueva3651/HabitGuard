package com.washburn.habitguard.ui.gallery

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.washburn.habitguard.R
import com.washburn.habitguard.ui.gallery.CalendarUtils.daysInMonthArray
import com.washburn.habitguard.ui.gallery.CalendarUtils.monthYearFromDate
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class GalleryFragment : Fragment(), CalendarAdapter.OnItemListener {

    private lateinit var viewModel: GalleryViewModel
    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)
        initWidgets(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)

        // Observe LiveData for selectedDate
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            setMonthView(date)
        }

        view.findViewById<View>(R.id.previousMonthButton).setOnClickListener {
            viewModel.goToPreviousMonth()
        }

        view.findViewById<View>(R.id.nextMonthButton).setOnClickListener {
            viewModel.goToNextMonth()
        }

        view.findViewById<View>(R.id.weeklyActionButton).setOnClickListener {
            weeklyAction()
        }
    }

    private fun initWidgets(view: View) {
        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView)
        monthYearText = view.findViewById(R.id.monthYearTV)
    }

    private fun setMonthView(date: LocalDate) {
        monthYearText.text = monthYearFromDate(date)
        val daysInMonth = daysInMonthArray(date)

        val calendarAdapter = CalendarAdapter(daysInMonth, this)
        val layoutManager = GridLayoutManager(requireContext(), 7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
    }

    override fun onItemClick(position: Int, date: LocalDate) {
        date?.let {
            viewModel.setSelectedDate(it)
        }
    }

    private fun weeklyAction() {
        requireActivity().startActivity(Intent(requireContext(), WeekViewActivity::class.java))
    }
}