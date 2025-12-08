package com.silentport.silentport.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.silentport.silentport.R
import com.silentport.silentport.model.AppUsageInfo
import com.silentport.silentport.ui.components.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSystemAppsScreen(
    apps: List<AppUsageInfo>,
    manualSystemApps: Set<String>,
    onAddToManualSystemApps: (String) -> Unit,
    onRemoveFromManualSystemApps: (String) -> Unit,
    onOpenNavigation: () -> Unit
) {
    val context = LocalContext.current
    
    // Sort apps: Real system apps first, then manually marked, then alphabetically
    val sortedApps = remember(apps, manualSystemApps) {
        apps.sortedWith(
            compareByDescending<AppUsageInfo> { it.isSystemApp }
                .thenByDescending { it.packageName in manualSystemApps }
                .thenBy { it.appLabel }
        )
    }

    LaunchedEffect(apps) {
        if (apps.isNotEmpty()) {
            val packageManager = context.packageManager
            val packageNames = apps.take(24).map { it.packageName }
            withContext(Dispatchers.IO) {
                AppIconCache.preload(packageManager, packageNames)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.nav_manual_system_apps)) },
                navigationIcon = {
                    IconButton(onClick = onOpenNavigation) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = stringResource(id = R.string.navigation_open)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.manual_system_apps_description),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (sortedApps.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.manual_system_apps_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(
                    items = sortedApps,
                    key = { it.packageName }
                ) { app ->
                    val isRealSystemApp = app.isSystemApp
                    val isManuallyMarked = app.packageName in manualSystemApps
                    val isMarkedAsSystem = isRealSystemApp || isManuallyMarked
                    ManualSystemAppItem(
                        app = app,
                        isMarkedAsSystem = isMarkedAsSystem,
                        isRealSystemApp = isRealSystemApp,
                        onToggle = { checked ->
                            if (checked) {
                                onAddToManualSystemApps(app.packageName)
                            } else {
                                onRemoveFromManualSystemApps(app.packageName)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualSystemAppItem(
    app: AppUsageInfo,
    isMarkedAsSystem: Boolean,
    isRealSystemApp: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isMarkedAsSystem) 
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        ListItem(
            headlineContent = { Text(text = app.appLabel) },
            supportingContent = { 
                if (isRealSystemApp) {
                    Text(text = "${app.packageName} " + stringResource(id = R.string.manual_system_app_real_system))
                } else {
                    Text(text = app.packageName)
                }
            },
            trailingContent = {
                Switch(
                    checked = isMarkedAsSystem,
                    onCheckedChange = onToggle,
                    enabled = !isRealSystemApp
                )
            }
        )
    }
}
