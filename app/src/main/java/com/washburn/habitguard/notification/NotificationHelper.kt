package com.washburn.habitguard.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.washburn.habitguard.R
import com.washburn.habitguard.firebase.AuthUtils.showToast
import java.util.concurrent.TimeUnit

class NotificationHelper(private val context: Context) {

    companion object {
        const val FINANCE_CHANNEL_ID = "finance_alerts"
        const val IMPORTANT_DATES_CHANNEL_ID = "important_dates"
        val IMPORTANT_TITLES = setOf(
            "Birthday", "Exam", "Mortgage",
            "Doctor Appointment", "Tax Due",
            "Payment", "Bill", "Due"
        )
    }

    private var notificationId = 1000
    private val notificationManager = NotificationManagerCompat.from(context)

    private val requestPermissionLauncher = when (context) {
        is AppCompatActivity -> context.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                initializeNotificationSystem()
                showToast(context, "Notifications enabled")
            } else {
                showToast(context, "Notifications disabled")
            }
        }
        else -> throw IllegalArgumentException("Context must be AppCompatActivity")
    }

    fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    initializeNotificationSystem()
                }
                (context as? AppCompatActivity)?.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == true -> {
                    showPermissionExplanationDialog()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            initializeNotificationSystem()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showPermissionExplanationDialog() {
        if (context !is AppCompatActivity) return

        AlertDialog.Builder(context)
            .setTitle("Notification Permission Needed")
            .setMessage("This app needs notification permission to remind you about important habits and financial alerts")
            .setPositiveButton("Allow") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Deny") { _, _ ->
                showToast(context, "You can enable notifications later in Settings")
            }
            .show()
    }

    fun initializeNotificationSystem() {
        createNotificationChannels()
        scheduleDailyChecks()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val financeChannel = NotificationChannel(
                FINANCE_CHANNEL_ID,
                context.getString(R.string.finance_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.finance_channel_description)
            }

            val importantDatesChannel = NotificationChannel(
                IMPORTANT_DATES_CHANNEL_ID,
                context.getString(R.string.important_dates_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.important_dates_channel_description)
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(listOf(financeChannel, importantDatesChannel))
        }
    }

    private fun scheduleDailyChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            24, // Repeat every 24 hours
            TimeUnit.HOURS
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyNotificationCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showFinanceAlert(title: String, message: String) {
        if (!canShowNotifications()) {
            showToast(context, "Notifications are disabled")
            return
        }

        val notification = buildNotification(
            FINANCE_CHANNEL_ID,
            "💰 $title",
            message,
            R.drawable.ic_alert
        ).build()
        notificationManager.notify(++notificationId, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showImportantDateAlert(title: String, message: String) {
        if (!canShowNotifications()) {
            showToast(context, "Notifications are disabled")
            return
        }

        val notification = buildNotification(
            IMPORTANT_DATES_CHANNEL_ID,
            "📅 $title",
            message,
            R.drawable.ic_important
        ).build()
        notificationManager.notify(++notificationId, notification)
    }

    private fun buildNotification(channelId: String, title: String, message: String, icon: Int): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
    }

    fun canShowNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                notificationManager.areNotificationsEnabled()
    }
}