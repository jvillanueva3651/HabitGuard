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

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.ui.navigateUp
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.washburn.habitguard.settings.SettingActivity
import com.washburn.habitguard.databinding.ActivitySideBinding
import com.washburn.habitguard.databinding.GeminiDialogBinding
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SideActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySideBinding

    private lateinit var appBarConfiguration: AppBarConfiguration

    private var geminiDialog: AlertDialog? = null
    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarSide.toolbar)

        generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = getString(R.string.gemini_api_key),
        )

        binding.appBarSide.fab.setOnClickListener { view ->
            showGeminiDialog()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_side)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_calendar, R.id.nav_finance
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            fetchUserData(userId)
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.side, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        return when (item.itemId) {
            R.id.action_settings -> {
                // Navigate to SettingActivity
                val intent = Intent(this, SettingActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_side)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun fetchUserData(userId: String) {
        val db = Firebase.firestore
        db.collection("HabitGuard")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Extract data from the document
                    val email = document.getString("email") ?: "No Email"
                    val username = document.getString("username") ?: "Unknown User"
                    val photoUrl = document.getString("photoUri")

                    // Update the NavigationView header
                    updateNavigationHeader(username, email, photoUrl)
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateNavigationHeader(username: String, email: String, photoUrl: String?) {
        val navView: NavigationView = binding.navView
        val headerView = navView.getHeaderView(0)

        val imageView = headerView.findViewById<ImageView>(R.id.imageView)
        val titleTextView = headerView.findViewById<TextView>(R.id.userNameTextView)
        val subtitleTextView = headerView.findViewById<TextView>(R.id.userEmailTextView)

        // Update the text views
        titleTextView.text = username
        subtitleTextView.text = email

        // Load the profile image (if available)
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this) // Use Glide to load the image
                .load(photoUrl)
                .circleCrop() // Optional: Crop the image to a circle
                .into(imageView)
        } else {
            // Set a default image if no photo URL is provided
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

                lifecycleScope.launch {
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
                        Toast.makeText(this@SideActivity, errorMsg, Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        geminiDialog?.dismiss()
    }
}