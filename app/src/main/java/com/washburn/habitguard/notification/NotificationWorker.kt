package com.washburn.habitguard.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat  // Add this import
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.washburn.habitguard.R
import com.washburn.habitguard.SideActivity
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = Firebase.auth.currentUser?.uid ?: return Result.failure()

        try {
            checkImportantDates(userId)
            checkFinancialStatus(userId)
            return Result.success()
        } catch (_: Exception) {
            // If failed, retry with exponential backoff
            return Result.retry()
        }
    }

    private suspend fun checkImportantDates(userId: String) {
        val today = LocalDate.now().toString()
        val habits = Firebase.firestore.collection("users")
            .document(userId)
            .collection("habits")
            .whereEqualTo("date", today)
            .get()
            .await()

        habits.forEach { doc ->
            val name = doc.getString("name") ?: ""
            if (NotificationHelper.IMPORTANT_TITLES.any { name.contains(it, ignoreCase = true) }) {
                showBackgroundNotification(
                    "📅 $name",
                    "Scheduled for today",
                    IMPORTANT_DATES_CHANNEL_ID,
                    R.drawable.ic_important
                )
            }
        }
    }

    private suspend fun checkFinancialStatus(userId: String) {
        val currentMonth = LocalDate.now().monthValue
        val currentYear = LocalDate.now().year

        val transactions = Firebase.firestore.collection("users")
            .document(userId)
            .collection("transactions")
            .whereEqualTo("month", currentMonth)
            .whereEqualTo("year", currentYear)
            .get()
            .await()

        val income = transactions.sumOf {
            if (it.getString("type") == "income") it.getDouble("amount") ?: 0.0 else 0.0
        }

        val expenses = transactions.sumOf {
            if (it.getString("type") == "expense") it.getDouble("amount") ?: 0.0 else 0.0
        }

        val credit = transactions.sumOf {
            if (it.getString("type") == "credit") it.getDouble("amount") ?: 0.0 else 0.0
        }

        if (expenses + credit > income) {
            val deficit = expenses + credit - income
            showBackgroundNotification(
                "💰 Budget Warning",
                "Your expenses exceed income by ${"%.2f".format(deficit)}",
                FINANCE_CHANNEL_ID,
                R.drawable.ic_alert
            )
        }
    }

    private fun showBackgroundNotification(title: String, message: String, channelId: String, icon: Int) {
        // Check if we have notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // If we don't have permission, just return (can't show notification)
                return
            }
        }

        // Create an explicit intent for the app's main activity
        val intent = Intent(applicationContext, SideActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (_: SecurityException) {
            // Handle case where notification permission was revoked
        }
    }

    companion object {
        const val FINANCE_CHANNEL_ID = "finance_alerts"
        const val IMPORTANT_DATES_CHANNEL_ID = "important_dates"
    }
}