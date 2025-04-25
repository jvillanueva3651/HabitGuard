/**===========================================================================================
 * SideActivity for main navigator view
 * REF    : USE_BY -> .ui/home & .ui/calendar & .ui/finance
 *                      .settings/[*]
 *          USING  -> .LoginActivity
 *          LAYOUT -> layout/content_side.xml & layout/app_bar_side.xml
 *                      layout/nav_header_side.xml & layout/activity_side.xml
 * Purpose: Handles navigation between fragments and etc.
 * Fun:  1. Navigation drawer with user profile header
 *       2. Navigation between home, calendar, and finance fragments
 *       3. Settings menu option
 *       4. TODO: Floating action button (placeholder functionality)
============================================================================================*/
package com.washburn.habitguard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.washburn.habitguard.databinding.ActivitySideBinding
import com.washburn.habitguard.databinding.GeminiDialogBinding
import com.washburn.habitguard.firebase.AuthUtils.showToast
import com.washburn.habitguard.notification.NotificationHelper
import com.washburn.habitguard.settings.SettingActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RequiresApi(Build.VERSION_CODES.O)
class SideActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySideBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var generativeModel: GenerativeModel
    private var geminiDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize notification system
        notificationHelper = NotificationHelper(this)
        notificationHelper.checkNotificationPermission()

        setSupportActionBar(binding.appBarSide.toolbar)
        setupNavigation()
        initializeGemini()
        setupTestButtonBehavior()

        // Hide test button by default (visible on long-press)
        binding.appBarSide.btnTestNotifications.isVisible = false
    }

    private fun setupNavigation() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_side)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_calendar, R.id.nav_finance),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            fetchUserData(userId)
        }
    }

    private fun initializeGemini() {
        generativeModel = GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = "AIzaSyB0xO9Zd-aaWpVE5XAem2B7tjddHPPuNpM"
        )
        binding.appBarSide.fab.setOnClickListener { showGeminiDialog() }
    }

    private fun setupTestButtonBehavior() {
        // Single click - show test notifications
        binding.appBarSide.btnTestNotifications.setOnClickListener {
            try {
                if (notificationHelper.canShowNotifications()) {
                    notificationHelper.showFinanceAlert(
                        "Test Budget Alert",
                        "This is a test financial notification"
                    )
                    notificationHelper.showImportantDateAlert(
                        "Test Reminder",
                        "This is a test event reminder"
                    )
                    showToast(this, "Test notifications sent")
                } else {
                    showToast(this, "Please enable notifications first")
                    notificationHelper.checkNotificationPermission()
                }
            } catch (_: SecurityException) {
                showToast(this, "Notification permission required")
                notificationHelper.checkNotificationPermission()
            }
        }

        // Long-press toolbar to toggle test button visibility
        binding.appBarSide.toolbar.setOnLongClickListener {
            binding.appBarSide.btnTestNotifications.isVisible =
                !binding.appBarSide.btnTestNotifications.isVisible
            showToast(this,
                if (binding.appBarSide.btnTestNotifications.isVisible)
                    "Dev mode enabled" else "Dev mode disabled")
            true
        }
    }

    private fun fetchUserData(userId: String) {
        val db = Firebase.firestore
        db.collection("HabitGuard")
            .document(userId)
            .collection("UserInfo")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val email = document.getString("email") ?: "No Email"
                    val username = document.getString("username") ?: "Unknown User"
                    val photoUrl = document.getString("photoUri")
                    updateNavigationHeader(username, email, photoUrl)
                }
            }
            .addOnFailureListener { e ->
                showToast(this, "Failed to fetch user data: ${e.message}")
            }
    }

    private fun updateNavigationHeader(username: String, email: String, photoUrl: String?) {
        val headerView = binding.navView.getHeaderView(0)
        val imageView = headerView.findViewById<ImageView>(R.id.imageView)
        val titleTextView = headerView.findViewById<TextView>(R.id.userNameTextView)
        val subtitleTextView = headerView.findViewById<TextView>(R.id.userEmailTextView)

        titleTextView.text = username
        subtitleTextView.text = email

        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    private fun showGeminiDialog() {
        geminiDialog?.dismiss()

        val dialogBinding = GeminiDialogBinding.inflate(LayoutInflater.from(this))

        geminiDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                window?.setLayout(
                    (resources.displayMetrics.widthPixels * 0.85).toInt(),
                    (resources.displayMetrics.heightPixels * 0.7).toInt()
                )
                window?.setGravity(Gravity.BOTTOM or Gravity.END)
                window?.attributes?.y = -100
            }

        dialogBinding.btnSubmit.setOnClickListener {
            val query = dialogBinding.etUserQuery.text.toString()
            if (query.isNotEmpty()) {
                dialogBinding.progressBar.isVisible = true
                dialogBinding.tvResponse.text = ""

                MainScope().launch {
                    try {
                        val response = generativeModel.generateContent(query)
                        dialogBinding.tvResponse.text = response.text ?: "No response received"
                    } catch (e: Exception) {
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Request timed out"
                            is UnknownHostException -> "No internet connection"
                            is IOException -> "Network error"
                            else -> "Error: ${e.localizedMessage}"
                        }
                        dialogBinding.tvResponse.text = errorMsg
                        showToast(this@SideActivity, errorMsg)
                    } finally {
                        dialogBinding.progressBar.isVisible = false
                    }
                }
            } else {
                dialogBinding.etUserQuery.error = "Please enter a question"
            }
        }

        dialogBinding.btnClose.setOnClickListener {
            geminiDialog?.dismiss()
        }

        geminiDialog?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.side, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_side)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        geminiDialog?.dismiss()
    }
}