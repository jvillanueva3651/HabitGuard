package com.washburn.habitguard

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.washburn.habitguard.databinding.ActivitySideBinding
import com.washburn.habitguard.settings.SettingActivity

class SideActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivitySideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarSide.toolbar)

        binding.appBarSide.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
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
}