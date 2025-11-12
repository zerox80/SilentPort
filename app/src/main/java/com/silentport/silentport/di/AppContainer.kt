package com.silentport.silentport.di

import android.content.Context
import com.silentport.silentport.data.local.AppDatabase
import com.silentport.silentport.data.repository.UsageRepository
import com.silentport.silentport.domain.ApplicationManager
import com.silentport.silentport.domain.UsageAnalyzer
import com.silentport.silentport.domain.UsagePolicy
import com.silentport.silentport.firewall.FirewallController
import com.silentport.silentport.firewall.FirewallPreferencesDataSource
import com.silentport.silentport.settings.SettingsPreferencesDataSource

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    private val database: AppDatabase = AppDatabase.getInstance(appContext)
    private val trackedAppDao = database.trackedAppDao()

    private val usagePolicy: UsagePolicy = UsagePolicy()

    val usageAnalyzer: UsageAnalyzer = UsageAnalyzer(appContext, trackedAppDao, usagePolicy)
    val applicationManager: ApplicationManager = ApplicationManager(appContext, trackedAppDao)
    val usageRepository: UsageRepository = UsageRepository(trackedAppDao)

    private val firewallPreferences: FirewallPreferencesDataSource = FirewallPreferencesDataSource(appContext)
    val firewallController: FirewallController = FirewallController(appContext, firewallPreferences)

    val settingsPreferences: SettingsPreferencesDataSource = SettingsPreferencesDataSource(appContext)
}
