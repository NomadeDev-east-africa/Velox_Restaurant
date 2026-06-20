package dj.nomade.velox_restaurant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dj.nomade.velox_restaurant.data.model.OrderStatus
import dj.nomade.velox_restaurant.ui.theme.InterFontFamily
import dj.nomade.velox_restaurant.ui.theme.NeonGreen
import dj.nomade.velox_restaurant.ui.theme.OnNeonGreen
import dj.nomade.velox_restaurant.ui.theme.SurfaceL3
import dj.nomade.velox_restaurant.ui.theme.StatusCancelled
import dj.nomade.velox_restaurant.ui.theme.StatusPending
import dj.nomade.velox_restaurant.ui.theme.StatusPreparing

@Composable
fun StatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status) {
        OrderStatus.PENDING    -> Triple(StatusPending.copy(alpha = 0.18f),  StatusPending,   "NOUVELLE")
        OrderStatus.ACCEPTED   -> Triple(StatusPreparing.copy(alpha = 0.18f), StatusPreparing, "ACCEPTÉE")
        OrderStatus.PREPARING  -> Triple(StatusPreparing.copy(alpha = 0.18f), StatusPreparing, "EN CUISINE")
        OrderStatus.READY      -> Triple(NeonGreen.copy(alpha = 0.18f),       NeonGreen,       "PRÊTE")
        OrderStatus.DELIVERING -> Triple(SurfaceL3, Color.White,              "LIVRAISON")
        OrderStatus.COMPLETED  -> Triple(SurfaceL3, Color.Gray,               "TERMINÉE")
        OrderStatus.CANCELLED  -> Triple(StatusCancelled.copy(alpha = 0.18f), StatusCancelled, "ANNULÉE")
    }
    Text(
        text = label,
        color = fg,
        fontSize = 9.sp,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
