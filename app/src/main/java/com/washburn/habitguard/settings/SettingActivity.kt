package com.washburn.habitguard.settings

import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.washburn.habitguard.LoginActivity
import com.washburn.habitguard.R
import com.washburn.habitguard.databinding.ActivitySettingsBinding
import com.washburn.habitguard.firebase.AuthUtils.showToast
import com.washburn.habitguard.firebase.FirebaseAuthHelper
import com.washburn.habitguard.notification.NotificationHelper
import androidx.core.content.edit
import androidx.work.WorkManager

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.O)
class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var authHelper: FirebaseAuthHelper
    private lateinit var notificationHelper: NotificationHelper

    private var mediaPlayer: MediaPlayer? = null
    private var isMusicPrepared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authHelper = FirebaseAuthHelper(this)
        notificationHelper = NotificationHelper(this)

        val notificationsEnabled = getNotificationPreference()
        val currentTheme = getThemePreference()

        setupMusicControls()
        setupAccountSettings()
        setupNotifications(notificationsEnabled)
        setupThemeSelection(currentTheme)
        setupMusicControls()
        setupVersionInfo()
        setupLogout()
    }

    private fun setupAccountSettings() {
        binding.accountSettingsButton.setOnClickListener {
            startActivity(Intent(this, ProfileEditActivity::class.java))
        }
    }

    private fun getNotificationPreference(): Boolean {
        return getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getBoolean("notifications_enabled", true)
    }
    private fun getThemePreference(): Int {
        return getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getInt("theme_preference", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }


    private fun setupNotifications(initialState: Boolean) {
        binding.notificationsSwitch.isChecked = initialState

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableNotifications()
            } else {
                disableNotifications()
            }

            getSharedPreferences("AppPreferences", MODE_PRIVATE).edit {
                putBoolean("notifications_enabled", isChecked)
            }
        }
    }

    private fun enableNotifications() {
        notificationHelper.initializeNotificationSystem()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationHelper.checkNotificationPermission()
        }

        notificationHelper.scheduleDailyChecks()
        showToast(this, "Notifications enabled")
    }

    private fun disableNotifications() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        WorkManager.getInstance(this).cancelUniqueWork("DailyNotificationCheck")
        showToast(this, "Notifications disabled")
    }

    private fun setupThemeSelection(currentTheme: Int) {
        // Set initial selection
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.lightThemeRadio.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.darkThemeRadio.isChecked = true
            else -> binding.systemDefaultRadio.isChecked = true
        }

        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val themeMode = when (checkedId) {
                R.id.lightThemeRadio -> {
                    showToast(this, "Light theme selected")
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                R.id.darkThemeRadio -> {
                    showToast(this, "Dark theme selected")
                    AppCompatDelegate.MODE_NIGHT_YES
                }
                else -> {
                    showToast(this, "System default theme selected")
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }

            AppCompatDelegate.setDefaultNightMode(themeMode)
            getSharedPreferences("AppPreferences", MODE_PRIVATE).edit {
                putInt("theme_preference", themeMode)
            }
        }
    }

    private fun setupMusicControls() {
        // Initialize buttons state
        updateMusicButtons(false)

        // Initialize media player with placeholder music
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            try {
                // Using raw resource for better reliability
                val afd = resources.openRawResourceFd(R.raw.sample_music)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepareAsync()
                setOnPreparedListener {
                    isMusicPrepared = true
                    updateMusicButtons(true)
                }
                setOnCompletionListener {
                    updateMusicButtons(false)
                    isMusicPrepared = false
                    mediaPlayer?.prepareAsync()
                }
            } catch (e: Exception) {
                showToast(this@SettingActivity, "Error initializing music player")
                e.printStackTrace()
            }
        }

        // Volume control
        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val volume = progress / 100f
                mediaPlayer?.setVolume(volume, volume)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Button listeners
        binding.playMusicButton.setOnClickListener {
            if (isMusicPrepared) {
                mediaPlayer?.start()
                updateMusicButtons(true)
                showToast(this, "Music playing")
            }
        }

        binding.pauseMusicButton.setOnClickListener {
            mediaPlayer?.pause()
            showToast(this, "Music paused")
        }

        binding.stopMusicButton.setOnClickListener {
            mediaPlayer?.stop()
            isMusicPrepared = false
            updateMusicButtons(false)
            mediaPlayer?.prepareAsync()
            showToast(this, "Music stopped")
        }
    }

    private fun updateMusicButtons(isPlaying: Boolean) {
        binding.playMusicButton.isEnabled = !isPlaying
        binding.pauseMusicButton.isEnabled = isPlaying
        binding.stopMusicButton.isEnabled = isPlaying
    }

    private fun setupVersionInfo() {
        binding.appVersionTextView.text = AppVersionUtils.getVersionName(this)
    }

    private fun setupLogout() {
        binding.logoutButton.setOnClickListener {
            authHelper.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            showToast(this, "Logged out successfully")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}