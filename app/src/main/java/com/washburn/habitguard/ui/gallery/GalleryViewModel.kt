package com.washburn.habitguard.ui.gallery

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class GalleryViewModel : ViewModel() {

    private val _selectedDate = MutableLiveData<LocalDate>()
    val selectedDate: LiveData<LocalDate> get() = _selectedDate

    init {
        // Initialize with the current date
        _selectedDate.value = LocalDate.now()
    }

    // Function to update the selected date
    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    // Functions to navigate to the previous and next month
    fun goToPreviousMonth() {
        _selectedDate.value = _selectedDate.value?.minusMonths(1)
    }

    fun goToNextMonth() {
        _selectedDate.value = _selectedDate.value?.plusMonths(1)
    }
}