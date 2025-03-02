package com.washburn.habitguard

import android.os.Bundle

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

import android.widget.CalendarView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.habitguard.FirestoreActivity

class MainActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestoreActivity = FirestoreActivity()
    private val eventsMap = mutableMapOf<String, MutableList<String>>()
    private val eventList = mutableListOf<String>()

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

    private fun signInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    if (uid != null) {
                        firestoreActivity.updateUserData()
                    }
                } else {
                    // Handle sign-in failure
                }
            }
    }
}

