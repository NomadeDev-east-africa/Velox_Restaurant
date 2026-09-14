package dj.nomade.velox_restaurant.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import dj.nomade.velox_restaurant.R
import dj.nomade.velox_restaurant.ui.theme.InterFontFamily
import dj.nomade.velox_restaurant.ui.theme.NeonGreen
import kotlinx.coroutines.delay

@Composable
fun TimeAgoText(createdAtMs: Long?, modifier: Modifier = Modifier) {
    var minutesAgo by remember { mutableStateOf(minutesSince(createdAtMs)) }

    LaunchedEffect(createdAtMs) {
        while (true) {
            minutesAgo = minutesSince(createdAtMs)
            delay(30_000)
        }
    }

    val label = when {
        createdAtMs == null -> ""
        minutesAgo < 1 -> stringResource(R.string.time_ago_now)
        minutesAgo < 60 -> stringResource(R.string.time_ago_minutes, minutesAgo)
        else -> stringResource(R.string.time_ago_hours, minutesAgo / 60)
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

private fun minutesSince(ms: Long?): Long {
    if (ms == null) return 0L
    return (System.currentTimeMillis() - ms) / 60_000
}
