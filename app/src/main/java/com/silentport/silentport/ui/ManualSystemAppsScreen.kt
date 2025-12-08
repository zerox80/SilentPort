package com.silentport.silentport.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    var showMarkedApps by remember { mutableStateOf(false) }
    
    // Sort apps: Real system apps first, then manually marked, then alphabetically
    val sortedApps = remember(apps, manualSystemApps) {
        apps.sortedWith(
            compareByDescending<AppUsageInfo> { it.isSystemApp }
                .thenByDescending { it.packageName in manualSystemApps }
                .thenBy { it.appLabel }
        )
    }
    
    // Find manually marked apps that might not be in the visible list
    val markedAppsInList = remember(apps, manualSystemApps) {
        apps.filter { it.packageName in manualSystemApps }
    }
    
    // Package names that are marked but not visible
    val hiddenMarkedPackages = remember(apps, manualSystemApps) {
        manualSystemApps - apps.map { it.packageName }.toSet()
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
            
            // Expandable section for marked apps
            if (manualSystemApps.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.manual_system_apps_marked_section),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(id = R.string.manual_system_apps_marked_count, manualSystemApps.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { showMarkedApps = !showMarkedApps }) {
                                    Text(
                                        text = if (showMarkedApps) 
                                            stringResource(id = R.string.manual_system_apps_collapse) 
                                        else 
                                            stringResource(id = R.string.manual_system_apps_expand)
                                    )
                                    Icon(
                                        imageVector = if (showMarkedApps) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        contentDescription = null
                                    )
                                }
                            }
                            
                            AnimatedVisibility(
                                visible = showMarkedApps,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Show marked apps that are in the visible list
                                    markedAppsInList.forEach { app ->
                                        MarkedAppItem(
                                            appLabel = app.appLabel,
                                            packageName = app.packageName,
                                            onRemove = { onRemoveFromManualSystemApps(app.packageName) }
                                        )
                                    }
                                    // Show marked apps that are hidden (not in visible list)
                                    hiddenMarkedPackages.forEach { packageName ->
                                        MarkedAppItem(
                                            appLabel = packageName.substringAfterLast('.'),
                                            packageName = packageName,
                                            onRemove = { onRemoveFromManualSystemApps(packageName) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkedAppItem(
    appLabel: String,
    packageName: String,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text = appLabel) },
        supportingContent = { Text(text = packageName) },
        trailingContent = {
            TextButton(onClick = onRemove) {
                Text(text = stringResource(id = R.string.manual_system_apps_remove))
            }
        }
    )
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
