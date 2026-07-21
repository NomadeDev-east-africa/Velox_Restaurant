package dj.nomade.velox_restaurant.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dj.nomade.velox_restaurant.data.model.PeriodStats
import dj.nomade.velox_restaurant.ui.theme.InterFontFamily
import dj.nomade.velox_restaurant.ui.theme.ManropeFontFamily
import dj.nomade.velox_restaurant.ui.theme.NeonGreen
import dj.nomade.velox_restaurant.ui.theme.OnNeonGreen
import dj.nomade.velox_restaurant.ui.theme.SurfaceL2
import dj.nomade.velox_restaurant.ui.theme.SurfaceL3
import java.text.NumberFormat
import java.util.Locale

/** 1234567 → "1 234 567" — lisible pour des montants en FDJ. */
private fun fdj(amount: Double): String =
    NumberFormat.getIntegerInstance(Locale.FRANCE).format(amount.toLong())

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Le ViewModel survit aux changements d'onglet : on redemande les cumuls à
    // chaque retour sur l'écran, sinon ils restent figés à leur valeur d'origine.
    LaunchedEffect(Unit) { viewModel.refreshTotals() }

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

        // ── Sélecteur de période ──────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsPeriod.entries.forEach { period ->
                PeriodChip(
                    label = period.label,
                    selected = state.period == period,
                    onClick = { viewModel.selectPeriod(period) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── KPI de la période ─────────────────────────────────────────────
        PeriodSummary(stats = state.periodStats, isLoading = state.isPeriodLoading)

        Spacer(Modifier.height(20.dp))

        // ── Tableau comptable annuel ──────────────────────────────────────
        if (state.period == StatsPeriod.YEAR && state.monthlyRows.isNotEmpty()) {
            SectionTitle("DÉTAIL MENSUEL ${state.year}")
            Spacer(Modifier.height(10.dp))
            MonthlyTable(rows = state.monthlyRows, total = state.periodStats)
            Spacer(Modifier.height(24.dp))
        }

        // ── Bar chart 7 jours ─────────────────────────────────────────────
        SectionTitle("REVENUS DES 7 DERNIERS JOURS")
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
            SectionTitle("TOP 5 PLATS — CE MOIS-CI")
            Spacer(Modifier.height(10.dp))
            state.topItems.forEachIndexed { index, item ->
                TopItemRow(rank = index + 1, item = item)
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Cumuls depuis l'ouverture ─────────────────────────────────────
        SectionTitle("DEPUIS L'OUVERTURE")
        Spacer(Modifier.height(10.dp))
        StatCard(
            icon = Icons.Default.Receipt,
            label = "Commandes honorées",
            value = state.totalOrders.toString(),
        )
        Spacer(Modifier.height(8.dp))
        StatCard(
            icon = Icons.Default.MonetizationOn,
            label = "Chiffre d'affaires",
            value = "${fdj(state.totalRevenue)} FDJ",
            valueColor = NeonGreen,
        )
        Spacer(Modifier.height(8.dp))
        StatCard(
            icon = Icons.Default.Star,
            label = "Note moyenne",
            value = String.format(Locale.FRANCE, "%.1f", state.avgRating),
        )

        Spacer(Modifier.height(80.dp))
    }
}

// ── Sélecteur ─────────────────────────────────────────────────────────────────

@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(if (selected) NeonGreen else SurfaceL2, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(),
            color = if (selected) OnNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
        )
    }
}

// ── Résumé de période ─────────────────────────────────────────────────────────

@Composable
private fun PeriodSummary(stats: PeriodStats, isLoading: Boolean) {
    Box {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeroCard(
                    label = "COMMANDES HONORÉES",
                    value = stats.completedCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                HeroCard(
                    label = "CHIFFRE D'AFFAIRES",
                    value = fdj(stats.revenue),
                    unit = "FDJ — hors livraison",
                    modifier = Modifier.weight(1f),
                    highlight = true,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniCard(
                    label = "PANIER MOYEN",
                    value = "${fdj(stats.averageBasket)} FDJ",
                    modifier = Modifier.weight(1f),
                )
                MiniCard(
                    label = "ANNULÉES",
                    value = "${stats.cancelledCount} · ${
                        String.format(Locale.FRANCE, "%.0f", stats.cancellationRate)
                    }%",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (isLoading) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ── Tableau mensuel ───────────────────────────────────────────────────────────

@Composable
private fun MonthlyTable(rows: List<MonthRow>, total: PeriodStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceL2, RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp),
    ) {
        MonthlyRowLayout(
            month = "MOIS",
            orders = "CMD",
            revenue = "CA (FDJ)",
            cancelled = "ANN.",
            isHeader = true,
        )
        rows.forEach { row ->
            MonthlyRowLayout(
                month = row.label,
                orders = row.stats.completedCount.toString(),
                revenue = fdj(row.stats.revenue),
                cancelled = row.stats.cancelledCount.toString(),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .height(1.dp)
                .background(SurfaceL3),
        )
        MonthlyRowLayout(
            month = "TOTAL",
            orders = total.completedCount.toString(),
            revenue = fdj(total.revenue),
            cancelled = total.cancelledCount.toString(),
            isTotal = true,
        )
    }
}

@Composable
private fun MonthlyRowLayout(
    month: String,
    orders: String,
    revenue: String,
    cancelled: String,
    isHeader: Boolean = false,
    isTotal: Boolean = false,
) {
    val color = when {
        isHeader -> MaterialTheme.colorScheme.onSurfaceVariant
        isTotal -> NeonGreen
        else -> MaterialTheme.colorScheme.onSurface
    }
    val weight = if (isHeader) FontWeight.Bold else if (isTotal) FontWeight.ExtraBold else FontWeight.Medium
    val fontSize = if (isHeader) 9.sp else 12.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(month, color = color, fontFamily = InterFontFamily, fontWeight = weight,
            fontSize = fontSize, modifier = Modifier.weight(1.5f))
        Text(orders, color = color, fontFamily = InterFontFamily, fontWeight = weight,
            fontSize = fontSize, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
        Text(revenue, color = if (isHeader) color else if (isTotal) NeonGreen else color,
            fontFamily = ManropeFontFamily, fontWeight = if (isHeader) weight else FontWeight.Bold,
            fontSize = fontSize, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
        Text(cancelled, color = color, fontFamily = InterFontFamily, fontWeight = weight,
            fontSize = fontSize, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
    }
}

// ── Composables partagés ──────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface,
        letterSpacing = 1.5.sp,
    )
}

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
            fontSize = 28.sp,
            color = if (highlight) NeonGreen else MaterialTheme.colorScheme.onSurface,
        )
        if (unit != null) {
            Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = InterFontFamily, fontSize = 9.sp)
        }
    }
}

@Composable
private fun MiniCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SurfaceL2, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp, fontFamily = InterFontFamily, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
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
            rank.toString().padStart(2, '0'),
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
