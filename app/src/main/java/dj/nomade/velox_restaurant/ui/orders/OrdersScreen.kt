package dj.nomade.velox_restaurant.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dj.nomade.velox_restaurant.R
import dj.nomade.velox_restaurant.data.model.Order
import dj.nomade.velox_restaurant.data.model.OrderItem
import dj.nomade.velox_restaurant.data.model.OrderStatus
import dj.nomade.velox_restaurant.ui.components.StatusChip
import dj.nomade.velox_restaurant.ui.components.TimeAgoText
import dj.nomade.velox_restaurant.ui.theme.InterFontFamily
import dj.nomade.velox_restaurant.ui.theme.ManropeFontFamily
import dj.nomade.velox_restaurant.ui.theme.NeonGreen
import dj.nomade.velox_restaurant.ui.theme.OnNeonGreen
import dj.nomade.velox_restaurant.ui.theme.SurfaceL1
import dj.nomade.velox_restaurant.ui.theme.SurfaceL2
import dj.nomade.velox_restaurant.ui.theme.SurfaceL3
import dj.nomade.velox_restaurant.ui.theme.TagExtra
import dj.nomade.velox_restaurant.ui.theme.TagSauce
import kotlinx.coroutines.delay

@Composable
fun OrdersScreen(viewModel: OrdersViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {

            // ── En-tête ────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    stringResource(R.string.orders_header_kicker),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = InterFontFamily,
                    letterSpacing = 2.sp,
                )
                Text(
                    stringResource(R.string.orders_header_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .background(SurfaceL2, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(6.dp).background(NeonGreen, RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.orders_system_active),
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    )
                }
            }

            // ── Tabs ───────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = NeonGreen,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .background(NeonGreen)
                    )
                },
                divider = {},
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                OnyxTab(
                    text = stringResource(R.string.orders_tab_new),
                    count = uiState.pendingOrders.size,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                )
                OnyxTab(
                    text = stringResource(R.string.orders_tab_active),
                    count = uiState.activeOrders.size,
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Listes ─────────────────────────────────────────────────
            if (selectedTab == 0) {
                PendingOrdersList(
                    orders = uiState.pendingOrders,
                    onAccept = { viewModel.acceptOrder(it) },
                    onCancel = { id, reason -> viewModel.cancelOrder(id, reason) },
                )
            } else {
                ActiveOrdersList(
                    orders = uiState.activeOrders,
                    resendingOrderId = uiState.resendingOrderId,
                    onStartPrep = { viewModel.startPreparation(it) },
                    onMarkReady = { viewModel.markReady(it) },
                    onCancel = { id, reason -> viewModel.cancelOrder(id, reason) },
                    onResend = { viewModel.resendToDrivers(it) },
                )
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ── Tab personnalisé ──────────────────────────────────────────────────────────

@Composable
private fun OnyxTab(text: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Tab(selected = selected, onClick = onClick) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                fontFamily = ManropeFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = if (selected) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) NeonGreen else SurfaceL3,
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) OnNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = InterFontFamily,
                    )
                }
            }
        }
    }
}

// ── Liste commandes en attente ────────────────────────────────────────────────

@Composable
private fun PendingOrdersList(
    orders: List<Order>,
    onAccept: (String) -> Unit,
    onCancel: (String, String) -> Unit,
) {
    if (orders.isEmpty()) {
        EmptyState(stringResource(R.string.orders_empty_new))
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        items(orders, key = { it.id }) { order ->
            PendingOrderCard(
                order = order,
                onAccept = { onAccept(order.id) },
                onCancel = onCancel,
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PendingOrderCard(
    order: Order,
    onAccept: () -> Unit,
    onCancel: (String, String) -> Unit,
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceL2, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        // ── Header ──
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    stringResource(R.string.orders_incoming_signal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontFamily = InterFontFamily,
                    letterSpacing = 1.5.sp,
                )
                Text(
                    stringResource(R.string.orders_id_format, order.id.takeLast(4).uppercase()),
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TimeAgoText(order.createdAt)
        }

        Spacer(Modifier.height(16.dp))

        // ── Client ──
        InfoRow(Icons.Default.Person, stringResource(R.string.orders_customer), order.customerName)
        Spacer(Modifier.height(8.dp))
        InfoRow(Icons.Default.Phone, stringResource(R.string.orders_contact), order.customerPhone)

        Spacer(Modifier.height(16.dp))

        // ── Payload / Value ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.orders_payload), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                Text(
                    pluralStringResource(R.plurals.orders_item_count, order.itemCount, order.itemCount),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.orders_value), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                Text(
                    stringResource(R.string.price_fdj, order.subtotal.toInt()),
                    color = NeonGreen,
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
                if (order.deliveryFee > 0) {
                    Text(
                        stringResource(R.string.orders_delivery_fee_line, order.deliveryFee.toInt()),
                        color = Color(0xFFFFC107),
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Détail articles ──
        OrderItemsDetail(order.items)

        Spacer(Modifier.height(20.dp))

        // ── Boutons ──
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OnNeonGreen),
        ) {
            Text(stringResource(R.string.orders_accept_button), fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp, letterSpacing = 2.sp)
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { showCancelDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Cancel, contentDescription = null,
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.orders_reject_button), color = MaterialTheme.colorScheme.error,
                fontFamily = InterFontFamily, fontSize = 12.sp)
        }
    }

    if (showCancelDialog) {
        CancelOrderDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = { reason -> onCancel(order.id, reason); showCancelDialog = false },
        )
    }
}

// ── Liste commandes actives ───────────────────────────────────────────────────

@Composable
private fun ActiveOrdersList(
    orders: List<Order>,
    resendingOrderId: String?,
    onStartPrep: (String) -> Unit,
    onMarkReady: (String) -> Unit,
    onCancel: (String, String) -> Unit,
    onResend: (String) -> Unit,
) {
    if (orders.isEmpty()) {
        EmptyState(stringResource(R.string.orders_empty_active))
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        items(orders, key = { it.id }) { order ->
            ActiveOrderCard(
                order = order,
                isResending = resendingOrderId == order.id,
                onStartPrep = { onStartPrep(order.id) },
                onMarkReady = { onMarkReady(order.id) },
                onCancel = onCancel,
                onResend = { onResend(order.id) },
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** Secondes restantes avant de pouvoir relancer (cooldown 60s côté Cloud Function). */
@Composable
private fun rememberResendCooldownSeconds(lastResendAt: Long?): State<Int> =
    produceState(initialValue = 0, lastResendAt) {
        if (lastResendAt == null) {
            value = 0
            return@produceState
        }
        while (true) {
            val elapsedSeconds = (System.currentTimeMillis() - lastResendAt) / 1000
            val remaining = (60 - elapsedSeconds).toInt().coerceAtLeast(0)
            value = remaining
            if (remaining <= 0) break
            delay(1_000)
        }
    }

@Composable
private fun ActiveOrderCard(
    order: Order,
    isResending: Boolean,
    onStartPrep: () -> Unit,
    onMarkReady: () -> Unit,
    onCancel: (String, String) -> Unit,
    onResend: () -> Unit,
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    // Barre de statut latérale colorée
    val statusColor = when (order.status) {
        OrderStatus.ACCEPTED  -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        OrderStatus.PREPARING -> NeonGreen.copy(alpha = 0.7f)
        OrderStatus.READY     -> NeonGreen
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(SurfaceL2, RoundedCornerShape(16.dp)),
    ) {
        // Barre latérale statut
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(statusColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
        )

        Column(Modifier.padding(16.dp).weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.orders_id_hash_format, order.id.takeLast(4).uppercase()),
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                StatusChip(order.status)
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.orders_customer_label, order.customerName),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.price_fdj, order.subtotal.toInt()),
                        color = NeonGreen,
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                    if (order.deliveryFee > 0) {
                        Text(
                            stringResource(R.string.orders_delivery_fee_line, order.deliveryFee.toInt()),
                            color = Color(0xFFFFC107),
                            fontFamily = InterFontFamily,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Items
            OrderItemsDetail(order.items)

            Spacer(Modifier.height(16.dp))

            // Action button selon statut
            when (order.status) {
                OrderStatus.ACCEPTED -> Button(
                    onClick = onStartPrep,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceL3, contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text(stringResource(R.string.orders_start_prep_button), fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                }

                OrderStatus.PREPARING -> Button(
                    onClick = onMarkReady,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen, contentColor = OnNeonGreen),
                ) {
                    Text(stringResource(R.string.orders_mark_ready_button), fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.5.sp)
                }

                OrderStatus.READY -> if (order.deliveryDriverId.isNullOrBlank()) {
                    val cooldown by rememberResendCooldownSeconds(order.lastResendAt)
                    Button(
                        onClick = onResend,
                        enabled = !isResending && cooldown == 0,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen, contentColor = OnNeonGreen,
                            disabledContainerColor = NeonGreen.copy(alpha = 0.3f),
                            disabledContentColor = OnNeonGreen.copy(alpha = 0.7f),
                        ),
                    ) {
                        Text(
                            when {
                                isResending -> stringResource(R.string.orders_resend_sending)
                                cooldown > 0 -> stringResource(R.string.orders_resend_cooldown, cooldown)
                                else -> stringResource(R.string.orders_resend_button)
                            },
                            fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp, letterSpacing = 1.sp,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (order.resendCount > 0) {
                            Text(
                                stringResource(R.string.orders_resend_count, order.resendCount),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = InterFontFamily, fontSize = 10.sp,
                            )
                        }
                        TimeAgoText(order.readyAt)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonGreen.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.orders_driver_en_route), color = NeonGreen,
                            fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }

                else -> Unit
            }

            TextButton(onClick = { showCancelDialog = true }) {
                Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.error,
                    fontFamily = InterFontFamily, fontSize = 11.sp)
            }
        }
    }

    if (showCancelDialog) {
        CancelOrderDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = { reason -> onCancel(order.id, reason); showCancelDialog = false },
        )
    }
}

// ── Détail articles (nom, sauces, suppléments) ────────────────────────────────

/**
 * Affichage commun aux deux onglets (réception → préparation → envoi livraison) :
 * même taille de police et mêmes couleurs partout pour que rien ne se "perde"
 * quand une commande change d'écran.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderItemsDetail(items: List<OrderItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.orders_item_quantity, item.quantity),
                        color = NeonGreen,
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.sauces.isNotEmpty() || item.extras.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        if (item.sauces.isNotEmpty()) {
                            OptionTag(stringResource(R.string.orders_tag_sauce), item.sauces.joinToString(", "), TagSauce)
                        }
                        if (item.extras.isNotEmpty()) {
                            OptionTag(stringResource(R.string.orders_tag_extra), item.extras.joinToString(", "), TagExtra)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionTag(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            "$label : $value",
            color = color,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

// ── Composables utilitaires ───────────────────────────────────────────────────

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp, fontFamily = InterFontFamily, letterSpacing = 1.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurface,
                fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = InterFontFamily, fontSize = 14.sp)
    }
}

@Composable
private fun CancelOrderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceL2,
        title = { Text(stringResource(R.string.orders_cancel_dialog_title), color = MaterialTheme.colorScheme.onSurface,
            fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text(stringResource(R.string.orders_cancel_dialog_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = SurfaceL3,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (reason.isNotBlank()) onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.orders_cancel_dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.orders_cancel_dialog_back), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
