package com.washburn.habitguard.ui.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class CalendarViewModel : ViewModel() {

    private val _selectedDate = MutableLiveData<LocalDate>()
    val selectedDate: LiveData<LocalDate> get() = _selectedDate

    init {
        _selectedDate.value = LocalDate.now()
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToPreviousMonth() {
        _selectedDate.value = _selectedDate.value?.minusMonths(1)
    }

    fun goToNextMonth() {
        _selectedDate.value = _selectedDate.value?.plusMonths(1)
    }
}