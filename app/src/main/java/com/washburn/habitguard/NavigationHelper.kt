package com.washburn.habitguard

import android.app.Activity
import android.content.Context
import android.content.Intent

object NavigationHelper {
    fun navigateTo(context: Context, destination: Class<*>, finishCurrent: Boolean = false) {
        context.startActivity(Intent(context, destination))
        if (finishCurrent && context is Activity) {
            context.finish()
        }
    }
}