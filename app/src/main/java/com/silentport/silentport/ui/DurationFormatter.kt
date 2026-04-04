package com.silentport.silentport.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.silentport.silentport.R
import java.util.concurrent.TimeUnit
import kotlin.math.max

@Composable
fun rememberDurationLabel(durationMillis: Long): String {
    val context = LocalContext.current
    return remember(durationMillis) { formatDuration(context, durationMillis) }
}

fun formatDuration(context: Context, durationMillis: Long): String {
    val monthMillis = TimeUnit.DAYS.toMillis(30)
    val weekMillis = TimeUnit.DAYS.toMillis(7)
    val dayMillis = TimeUnit.DAYS.toMillis(1)
    val hourMillis = TimeUnit.HOURS.toMillis(1)
    val minuteMillis = TimeUnit.MINUTES.toMillis(1)

    val months = durationMillis / monthMillis
    if (durationMillis % monthMillis == 0L && months > 0) {
        return context.resources.getQuantityString(R.plurals.duration_months, months.toInt(), months)
    }

    val weeks = durationMillis / weekMillis
    if (durationMillis % weekMillis == 0L && weeks > 0) {
        return context.resources.getQuantityString(R.plurals.duration_weeks, weeks.toInt(), weeks)
    }

    val days = durationMillis / dayMillis
    if (durationMillis % dayMillis == 0L && days > 0) {
        return context.resources.getQuantityString(R.plurals.duration_days, days.toInt(), days)
    }

    val hours = durationMillis / hourMillis
    if (durationMillis % hourMillis == 0L && hours > 0) {
        return context.resources.getQuantityString(R.plurals.duration_hours, hours.toInt(), hours)
    }

    val minutes = durationMillis / minuteMillis
    if (minutes > 0) {
        return context.resources.getQuantityString(R.plurals.duration_minutes, minutes.toInt(), minutes)
    }

    val seconds = max(1L, durationMillis / 1000L)
    return context.resources.getQuantityString(R.plurals.duration_seconds, seconds.toInt(), seconds)
}

