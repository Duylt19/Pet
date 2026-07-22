package com.asianmobile.privatebrower.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.asianmobile.privatebrower.R

/**
 * Formats a timestamp into a human-readable "time ago" string.
 * Uses string resources for localization support.
 */
@Composable
fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp

    val minutes = (diffMs / (1000 * 60)).toInt()
    val hours = (diffMs / (1000 * 60 * 60)).toInt()
    val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()

    return when {
        minutes < 1 -> stringResource(R.string.time_ago_just_now)
        hours < 1 -> stringResource(R.string.time_ago_minutes, minutes)
        days < 1 -> stringResource(R.string.time_ago_hours, hours)
        else -> stringResource(R.string.time_ago_days, days)
    }
}


