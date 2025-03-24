package com.washburn.habitguard.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.washburn.habitguard.databinding.ActivitySettingsBinding

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    // Properties for Data Binding
    var notificationsEnabled: Boolean = false
    val appVersion: String = "Version 1.0.0" // Replace with actual version

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Data Binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.accountSettingsButton.setOnClickListener { accountSetting() }
    }

    // Handle account settings button click
    private fun accountSetting() {
        startActivity(Intent(this, ProfileEditActivity::class.java))
        finish()
    }


}