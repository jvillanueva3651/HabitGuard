package com.washburn.habitguard.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

class GalleryViewModel : ViewModel() {

    private val _selectedDate = MutableLiveData<LocalDate>()
    val selectedDate: LiveData<LocalDate> get() = _selectedDate

    init {
        _selectedDate.value = LocalDate.now()
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }
}