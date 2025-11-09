package com.example.batteryanalyzer.di

import android.content.Context
import com.example.batteryanalyzer.data.local.AppDatabase
import com.example.batteryanalyzer.data.repository.UsageRepository
import com.example.batteryanalyzer.domain.ApplicationManager
import com.example.batteryanalyzer.domain.UsageAnalyzer
import com.example.batteryanalyzer.domain.UsagePolicy

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    private val database: AppDatabase = AppDatabase.getInstance(appContext)
    private val trackedAppDao = database.trackedAppDao()

    private val usagePolicy: UsagePolicy = UsagePolicy()

    val usageAnalyzer: UsageAnalyzer = UsageAnalyzer(appContext, trackedAppDao, usagePolicy)
    val applicationManager: ApplicationManager = ApplicationManager(appContext, trackedAppDao)
    val usageRepository: UsageRepository = UsageRepository(trackedAppDao)
}
