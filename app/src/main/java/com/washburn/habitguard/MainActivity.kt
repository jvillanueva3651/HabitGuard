package com.washburn.habitguard

import android.os.Bundle

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

import android.widget.CalendarView
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val calendar: CalendarView = findViewById(R.id.calendar)
        val dateView: TextView = findViewById(R.id.date_view)

        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val date = (dayOfMonth.toString() + "-" + (month + 1) + "-" + year)
            dateView.text = date
        }

        val gotoMain = findViewById<Button>(R.id.gotoMain)
        gotoMain.setOnClickListener {
            startActivity(Intent(this, SideActivity::class.java))
            finish()  // finish() so user can't come back by pressing back
        }
    }
}