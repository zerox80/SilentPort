package com.silentport.silentport.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val DATA_STORE_NAME = "settings_prefs"
private val Context.settingsDataStore by preferencesDataStore(name = DATA_STORE_NAME)

class SettingsPreferencesDataSource(context: Context) {

    private val dataStore: DataStore<Preferences> = context.applicationContext.settingsDataStore

    val preferencesFlow: Flow<AppSettingsPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { prefs ->
            AppSettingsPreferences(
                allowDurationMillis = prefs[Keys.ALLOW_DURATION_MILLIS] ?: DEFAULT_ALLOW_DURATION_MILLIS,
                metricsEnabled = prefs[Keys.METRICS_ENABLED] ?: false,
                manualFirewallUnblock = prefs[Keys.MANUAL_FIREWALL_UNBLOCK] ?: false,
                hideSystemApps = prefs[Keys.HIDE_SYSTEM_APPS] ?: false,
                manualSystemApps = prefs[Keys.MANUAL_SYSTEM_APPS] ?: emptySet()
            )
        }

    suspend fun setAllowDurationMillis(durationMillis: Long) {
        require(durationMillis > 0) { "Duration must be positive" }
        dataStore.edit { prefs ->
            prefs[Keys.ALLOW_DURATION_MILLIS] = durationMillis
        }
    }

    suspend fun setMetricsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.METRICS_ENABLED] = enabled
        }
    }

    suspend fun setManualFirewallUnblock(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.MANUAL_FIREWALL_UNBLOCK] = enabled
        }
    }

    suspend fun setHideSystemApps(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.HIDE_SYSTEM_APPS] = enabled
        }
    }

    suspend fun setManualSystemApps(packages: Set<String>) {
        dataStore.edit { prefs ->
            if (packages.isEmpty()) {
                prefs.remove(Keys.MANUAL_SYSTEM_APPS)
            } else {
                prefs[Keys.MANUAL_SYSTEM_APPS] = packages
            }
        }
    }

    private object Keys {
        val ALLOW_DURATION_MILLIS = longPreferencesKey("allow_duration_millis")
        val METRICS_ENABLED = booleanPreferencesKey("metrics_enabled")
        val MANUAL_FIREWALL_UNBLOCK = booleanPreferencesKey("manual_firewall_unblock")
        val HIDE_SYSTEM_APPS = booleanPreferencesKey("hide_system_apps")
        val MANUAL_SYSTEM_APPS = stringSetPreferencesKey("manual_system_apps")
    }

    companion object {
        val DEFAULT_ALLOW_DURATION_MILLIS: Long = TimeUnit.DAYS.toMillis(4)
    }
}

data class AppSettingsPreferences(
    val allowDurationMillis: Long,
    val metricsEnabled: Boolean,
    val manualFirewallUnblock: Boolean,
    val hideSystemApps: Boolean,
    val manualSystemApps: Set<String> = emptySet()
)

