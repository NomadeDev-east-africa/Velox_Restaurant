package dj.nomade.velox_restaurant.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dj.nomade.velox_restaurant.ui.theme.InterFontFamily
import dj.nomade.velox_restaurant.ui.theme.ManropeFontFamily
import dj.nomade.velox_restaurant.ui.theme.NeonGreen
import dj.nomade.velox_restaurant.ui.theme.OnNeonGreen
import dj.nomade.velox_restaurant.ui.theme.SurfaceL2
import dj.nomade.velox_restaurant.ui.theme.SurfaceL3

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonGreen)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Hero aujourd'hui ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeroCard(
                label = "COMMANDES",
                value = state.todayOrders.toString(),
                modifier = Modifier.weight(1f),
            )
            HeroCard(
                label = "CHIFFRE D'AFFAIRES",
                value = "${state.todayRevenue.toInt()}",
                unit = "FDJ",
                modifier = Modifier.weight(1f),
                highlight = true,
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Stat cards ────────────────────────────────────────────────────
        StatCard(
            icon = Icons.Default.Receipt,
            label = "Total Commandes",
            value = state.totalOrders.toString(),
        )
        Spacer(Modifier.height(8.dp))
        StatCard(
            icon = Icons.Default.MonetizationOn,
            label = "Revenus Totaux",
            value = "${state.totalRevenue.toInt()} FDJ",
            valueColor = NeonGreen,
        )
        Spacer(Modifier.height(8.dp))
        StatCard(
            icon = Icons.Default.Star,
            label = "Note Moyenne",
            value = String.format("%.1f", state.avgRating),
        )

        Spacer(Modifier.height(24.dp))

        // ── Bar chart 7 jours ─────────────────────────────────────────────
        Text(
            "REVENUS DES 7 DERNIERS JOURS",
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(12.dp))

        RevenueBarChart(
            data = state.weeklyRevenue,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(SurfaceL2, RoundedCornerShape(12.dp))
                .padding(16.dp),
        )

        Spacer(Modifier.height(24.dp))

        // ── Top 5 plats ───────────────────────────────────────────────────
        if (state.topItems.isNotEmpty()) {
            Text(
                "TOP 5 PLATS LES PLUS VENDUS",
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(10.dp))
            state.topItems.forEachIndexed { index, item ->
                TopItemRow(rank = index + 1, item = item)
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    label: String,
    value: String,
    unit: String? = null,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SurfaceL2, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp, fontFamily = InterFontFamily, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 36.sp,
            color = if (highlight) NeonGreen else MaterialTheme.colorScheme.onSurface,
        )
        if (unit != null) {
            Text(unit, color = NeonGreen, fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceL2, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = InterFontFamily, fontSize = 12.sp)
            Text(value, color = valueColor, fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }
    }
}

@Composable
private fun RevenueBarChart(data: List<DayRevenue>, modifier: Modifier = Modifier) {
    val maxVal = data.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0
    val barColor = NeonGreen
    val labelColor = Color(0xFF808080)

    Column(modifier) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(Modifier.fillMaxSize()) {
                val barCount = data.size
                if (barCount == 0) return@Canvas
                val gap = size.width * 0.04f
                val barWidth = (size.width - gap * (barCount + 1)) / barCount

                data.forEachIndexed { i, day ->
                    val barHeight = (day.amount / maxVal * size.height).toFloat()
                    val x = gap + i * (barWidth + gap)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            data.forEach { day ->
                Text(day.label, color = labelColor, fontSize = 10.sp, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun TopItemRow(rank: Int, item: TopItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceL2, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${rank.toString().padStart(2, '0')}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.width(32.dp),
        )
        Text(item.name, color = MaterialTheme.colorScheme.onSurface,
            fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,
            fontSize = 14.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .background(NeonGreen, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                "${item.count} VENDU${if (item.count > 1) "S" else ""}",
                color = OnNeonGreen,
                fontSize = 10.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
