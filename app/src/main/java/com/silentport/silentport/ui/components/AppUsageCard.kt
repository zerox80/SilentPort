package com.silentport.silentport.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silentport.silentport.R
import com.silentport.silentport.data.local.AppUsageStatus
import com.silentport.silentport.model.AppUsageInfo
import com.silentport.silentport.util.UsageTextFormatter
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppUsageCard(
    app: AppUsageInfo,
    onOpenAppInfo: () -> Unit,
    manualFirewallEnabled: Boolean,
    isManuallyBlocked: Boolean,
    onManualUnblock: () -> Unit
) {
    val context = LocalContext.current
    val lastUsedText = remember(app.lastUsedAt, context) {
        UsageTextFormatter.formatLastUsed(context, app.lastUsedAt)
    }
    val scheduledDisableText = remember(app.scheduledDisableAt, context) {
        UsageTextFormatter.formatScheduledDisable(context, app.scheduledDisableAt)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AppIcon(context = context, packageName = app.packageName, appLabel = app.appLabel)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = lastUsedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(app.status)
                scheduledDisableText?.let { text ->
                    SuggestionChip(
                        onClick = {},
                        enabled = false,
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        label = { Text(text = text) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onOpenAppInfo) {
                    Text(text = stringResource(id = R.string.action_open_app_info))
                }

                if (manualFirewallEnabled && isManuallyBlocked) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Button(
                        onClick = onManualUnblock,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(text = stringResource(id = R.string.firewall_manual_unblock_button))
                    }
                }

            }
        }
    }
}

@Composable
private fun AppIcon(context: Context, packageName: String, appLabel: String) {
    val packageManager = context.packageManager
    
    // Optimization: Try to get from cache synchronously first to avoid initial null state
    // and unnecessary recompositions for cached items.
    var drawable by remember(packageName) { 
        mutableStateOf(AppIconCache.getFromCache(packageName)) 
    }

    LaunchedEffect(packageName) {
        if (drawable == null) {
            withContext(Dispatchers.IO) {
                val loaded = AppIconCache.getOrLoad(packageManager, packageName)
                withContext(Dispatchers.Main) {
                    drawable = loaded
                }
            }
        }
    }

    if (drawable != null) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            androidx.compose.foundation.Image(
                painter = rememberDrawablePainter(drawable = drawable),
                contentDescription = null,
                modifier = Modifier.padding(4.dp)
            )
        }
    } else {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = appLabel.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: AppUsageStatus) {
    val labelRes = when (status) {
        AppUsageStatus.RECENT -> R.string.status_recent
        AppUsageStatus.RARE -> R.string.status_rare
    }
    val colors = when (status) {
        AppUsageStatus.RECENT -> SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        AppUsageStatus.RARE -> SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

    SuggestionChip(
        onClick = {},
        enabled = false,
        colors = colors,
        label = { Text(text = stringResource(id = labelRes)) }
    )
}

internal object AppIconCache {
    private const val CACHE_SIZE = 150
    private val cache = object : LruCache<String, Drawable>(CACHE_SIZE) {}

    fun getFromCache(packageName: String): Drawable? {
        synchronized(cache) {
            return cache.get(packageName)
        }
    }

    fun getOrLoad(packageManager: PackageManager, packageName: String): Drawable? {
        synchronized(cache) {
            cache.get(packageName)?.let { return it }
        }

        val drawable = runCatching { packageManager.getApplicationIcon(packageName).mutate() }.getOrNull()

        if (drawable != null) {
            synchronized(cache) {
                cache.put(packageName, drawable)
            }
        }

        return drawable
    }

    fun preload(packageManager: PackageManager, packageNames: Collection<String>) {
        packageNames.forEach { packageName ->
            getOrLoad(packageManager, packageName)
        }
    }
}

