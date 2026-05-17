package com.example.velox_restaurant.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.velox_restaurant.ui.theme.InterFontFamily
import com.example.velox_restaurant.ui.theme.NeonGreen
import kotlinx.coroutines.delay

@Composable
fun TimeAgoText(createdAtMs: Long?, modifier: Modifier = Modifier) {
    var label by remember { mutableStateOf(formatTimeAgo(createdAtMs)) }

    LaunchedEffect(createdAtMs) {
        while (true) {
            label = formatTimeAgo(createdAtMs)
            delay(30_000)
        }
    }

    val isNew = createdAtMs != null && (System.currentTimeMillis() - createdAtMs) < 60_000
    Text(
        text = label,
        color = if (isNew) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
        fontFamily = InterFontFamily,
        letterSpacing = 0.5.sp,
        modifier = modifier,
    )
}

private fun formatTimeAgo(ms: Long?): String {
    if (ms == null) return ""
    val diff = System.currentTimeMillis() - ms
    val minutes = diff / 60_000
    return when {
        minutes < 1  -> "NOW"
        minutes < 60 -> "${minutes}M AGO"
        else         -> "${minutes / 60}H AGO"
    }
}
