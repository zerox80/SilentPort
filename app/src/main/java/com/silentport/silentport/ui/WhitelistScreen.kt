package com.silentport.silentport.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
fun WhitelistScreen(
    apps: List<AppUsageInfo>,
    whitelistedPackages: Set<String>,
    hardcodedAllowlist: Set<String>,
    onAddToWhitelist: (String) -> Unit,
    onRemoveFromWhitelist: (String) -> Unit,
    onOpenNavigation: () -> Unit
) {
    val context = LocalContext.current
    
    // Sort apps: Hardcoded first, then Whitelisted, then alphabetically
    val sortedApps = remember(apps, whitelistedPackages, hardcodedAllowlist) {
        apps.sortedWith(
            compareByDescending<AppUsageInfo> { it.packageName in hardcodedAllowlist }
                .thenByDescending { it.packageName in whitelistedPackages }
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
                title = { Text(text = stringResource(id = R.string.nav_whitelist)) },
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
                    text = stringResource(id = R.string.whitelist_description),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (sortedApps.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.whitelist_empty),
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
                    val isHardcoded = app.packageName in hardcodedAllowlist
                    val isWhitelisted = isHardcoded || app.packageName in whitelistedPackages
                    WhitelistItem(
                        app = app,
                        isWhitelisted = isWhitelisted,
                        isHardcoded = isHardcoded,
                        onToggle = { checked ->
                            if (checked) {
                                onAddToWhitelist(app.packageName)
                            } else {
                                onRemoveFromWhitelist(app.packageName)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WhitelistItem(
    app: AppUsageInfo,
    isWhitelisted: Boolean,
    isHardcoded: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isWhitelisted) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        ListItem(
            headlineContent = { Text(text = app.appLabel) },
            supportingContent = { 
                if (isHardcoded) {
                    Text(text = "${app.packageName} (System)")
                } else {
                    Text(text = app.packageName)
                }
            },
            trailingContent = {
                Switch(
                    checked = isWhitelisted,
                    onCheckedChange = onToggle,
                    enabled = !isHardcoded
                )
            }
        )
    }
}
