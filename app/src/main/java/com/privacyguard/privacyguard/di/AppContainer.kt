package com.privacyguard.privacyguard.di

import android.content.Context
import com.privacyguard.privacyguard.data.local.AppDatabase
import com.privacyguard.privacyguard.data.repository.UsageRepository
import com.privacyguard.privacyguard.domain.ApplicationManager
import com.privacyguard.privacyguard.domain.UsageAnalyzer
import com.privacyguard.privacyguard.domain.UsagePolicy
import com.privacyguard.privacyguard.firewall.FirewallController
import com.privacyguard.privacyguard.firewall.FirewallPreferencesDataSource
import com.privacyguard.privacyguard.settings.SettingsPreferencesDataSource

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
