package com.silentport.silentport.domain

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.silentport.silentport.data.local.AppUsageStatus
import com.silentport.silentport.data.local.TrackedAppDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApplicationManager(
    private val context: Context,
    private val trackedAppDao: TrackedAppDao
) {

    private val packageManager: PackageManager = context.packageManager



    companion object {
        private const val TAG = "ApplicationManager"
    }
}
