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
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

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

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}