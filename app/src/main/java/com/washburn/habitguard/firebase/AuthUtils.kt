/**===========================================================================================
 * AuthUtils - Utility object for authentication-related helper functions.
 *
 * Provides:
 * 1. Toast message display
 * 2. Password visibility toggle
 * 3. Network connectivity check
 *
 * Note: As an object, these functions are available globally without instantiation.
============================================================================================*/
package com.washburn.habitguard.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import com.washburn.habitguard.R

object AuthUtils {
    // Toast message display
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    // Password visibility toggle
    fun togglePasswordVisibility(editText: EditText, toggleButton: ImageButton) {
        toggleButton.setOnClickListener {
            val isPasswordVisible = editText.transformationMethod == PasswordTransformationMethod.getInstance()
            editText.transformationMethod = if (isPasswordVisible) {
                toggleButton.setImageResource(R.drawable.ic_visibility)
                HideReturnsTransformationMethod.getInstance()
            } else {
                toggleButton.setImageResource(R.drawable.ic_visibility_off)
                PasswordTransformationMethod.getInstance()
            }
            editText.setSelection(editText.text.length)
        }
    }

    // Network connectivity check
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}