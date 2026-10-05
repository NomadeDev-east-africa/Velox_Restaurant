package dj.nomade.velox_restaurant.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dj.nomade.velox_restaurant.R
import dj.nomade.velox_restaurant.core.locale.AppLocale
import dj.nomade.velox_restaurant.data.model.LoyverseConnection
import dj.nomade.velox_restaurant.data.model.LoyverseStatus
import dj.nomade.velox_restaurant.ui.theme.InterFontFamily
import dj.nomade.velox_restaurant.ui.theme.ManropeFontFamily
import dj.nomade.velox_restaurant.ui.theme.NeonGreen
import dj.nomade.velox_restaurant.ui.theme.NeonGreenDim
import dj.nomade.velox_restaurant.ui.theme.OnNeonGreen
import dj.nomade.velox_restaurant.ui.theme.SurfaceL2
import dj.nomade.velox_restaurant.ui.theme.SurfaceL3

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLoyverseDisconnectDialog by remember { mutableStateOf(false) }

    // Rafraîchit l'état Loyverse à chaque retour sur l'écran, notamment au retour
    // du navigateur après l'autorisation chez Loyverse.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshLoyverse()
        onPauseOrDispose { }
    }
    LaunchedEffect(uiState.loyverseAuthUrl) {
        val url = uiState.loyverseAuthUrl ?: return@LaunchedEffect
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        viewModel.onLoyverseAuthUrlOpened()
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.uploadProfileImage(it) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) onLogout()
    }

    val restaurant = uiState.restaurant
    var bannerError by remember(restaurant?.imageUrl) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (restaurant == null && uiState.isLoading) {
            CircularProgressIndicator(color = NeonGreen, modifier = Modifier.align(Alignment.Center))
        } else if (restaurant != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Hero image ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.linearGradient(listOf(NeonGreen.copy(0.3f), NeonGreenDim.copy(0.1f)))
                        )
                        .clickable {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.BottomStart,
                ) {
                    if (!restaurant.imageUrl.isNullOrBlank() && !bannerError) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(restaurant.imageUrl)
                                .size(360, 200)  // ← REDIMENSIONNEMENT CRUCIAL : taille réelle d'affichage (200dp ≈ 360px)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = { bannerError = true },
                        )
                    }
                    // Overlay
                    Box(Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(
                            MaterialTheme.colorScheme.background.copy(0f),
                            MaterialTheme.colorScheme.background.copy(0.7f),
                        ))
                    ))
                    Column(Modifier.padding(16.dp)) {
                        Text(restaurant.name, fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(
                                if (restaurant.isOpen) NeonGreen else MaterialTheme.colorScheme.error,
                                CircleShape,
                            ))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(if (restaurant.isOpen) R.string.profile_status_open else R.string.profile_status_closed),
                                color = if (restaurant.isOpen) NeonGreen else MaterialTheme.colorScheme.error,
                                fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            )
                        }
                    }
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.content_description_change_photo),
                        tint = NeonGreen.copy(0.8f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(20.dp),
                    )
                }

                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(20.dp))

                    // ── Toggle ouvert/fermé ───────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceL2, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.profile_toggle_open_label),
                            fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = restaurant.isOpen,
                            onCheckedChange = { viewModel.toggleIsOpen() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnNeonGreen,
                                checkedTrackColor = NeonGreen,
                                uncheckedTrackColor = SurfaceL3,
                            ),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Informations ──────────────────────────────────────
                    SectionTitle(stringResource(R.string.profile_section_information))
                    ProfileInfoCard(Icons.Default.Mail, stringResource(R.string.profile_label_email), restaurant.email)
                    Spacer(Modifier.height(8.dp))
                    ProfileInfoCard(Icons.Default.Phone, stringResource(R.string.profile_label_phone), restaurant.phone)
                    Spacer(Modifier.height(8.dp))
                    ProfileInfoCard(Icons.Default.LocationOn, stringResource(R.string.profile_label_address), restaurant.address)

                    Spacer(Modifier.height(24.dp))

                    // ── Stats rapides ─────────────────────────────────────
                    SectionTitle(stringResource(R.string.profile_section_stats))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MiniStatCard(stringResource(R.string.profile_stat_total_orders),
                            uiState.totalOrders.toString(), Modifier.weight(1f))
                        MiniStatCard(stringResource(R.string.profile_stat_total_revenue),
                            stringResource(R.string.price_fdj, uiState.totalRevenue.toInt()), Modifier.weight(1f),
                            valueColor = NeonGreen)
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Caisse Loyverse ───────────────────────────────────
                    SectionTitle(stringResource(R.string.profile_section_loyverse))
                    LoyverseSection(
                        connection = uiState.loyverse,
                        busy = uiState.loyverseBusy,
                        onConnect = viewModel::connectLoyverse,
                        onDisconnect = { showLoyverseDisconnectDialog = true },
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Langue ────────────────────────────────────────────
                    SectionTitle(stringResource(R.string.profile_section_language))
                    LanguagePicker()

                    Spacer(Modifier.height(32.dp))

                    // ── Logout ────────────────────────────────────────────
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_logout_button), fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.5.sp)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.profile_footer_version),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(80.dp))
                }
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showLoyverseDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showLoyverseDisconnectDialog = false },
            containerColor = SurfaceL2,
            title = {
                Text(stringResource(R.string.loyverse_disconnect_dialog_title), fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(stringResource(R.string.loyverse_disconnect_dialog_text),
                    fontFamily = InterFontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.disconnectLoyverse(); showLoyverseDisconnectDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.loyverse_disconnect_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showLoyverseDisconnectDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = SurfaceL2,
            title = {
                Text(stringResource(R.string.profile_logout_dialog_title), fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(stringResource(R.string.profile_logout_dialog_text),
                    fontFamily = InterFontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.logout(); showLogoutDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.profile_logout_dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ProfileInfoCard(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceL2, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(NeonGreen.copy(0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp, fontFamily = InterFontFamily, letterSpacing = 1.5.sp)
            Text(value.ifBlank { stringResource(R.string.profile_placeholder_dash) }, color = MaterialTheme.colorScheme.onSurface,
                fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun MiniStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier
            .background(SurfaceL2, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp, fontFamily = InterFontFamily)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}

/**
 * Carte de liaison avec la caisse Loyverse : un bouton « Connecter » tant que le
 * restaurant n'est pas relié, puis le nom du commerce Loyverse et « Déconnecter ».
 */
@Composable
private fun LoyverseSection(
    connection: LoyverseConnection?,
    busy: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val status = connection?.status
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceL2, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val warning = status == LoyverseStatus.RECONNECT_REQUIRED
            val accent = if (warning) MaterialTheme.colorScheme.error else NeonGreen
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (warning) Icons.Default.Warning else Icons.Default.PointOfSale,
                    contentDescription = null, tint = accent, modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (status) {
                        LoyverseStatus.CONNECTED -> stringResource(R.string.loyverse_status_connected)
                        LoyverseStatus.RECONNECT_REQUIRED -> stringResource(R.string.loyverse_status_reconnect)
                        else -> stringResource(R.string.loyverse_status_not_connected)
                    },
                    color = if (status == LoyverseStatus.CONNECTED) NeonGreen else MaterialTheme.colorScheme.onSurface,
                    fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                )
                Text(
                    when (status) {
                        LoyverseStatus.CONNECTED -> connection?.businessName
                            ?: stringResource(R.string.loyverse_description_connected)
                        LoyverseStatus.RECONNECT_REQUIRED -> stringResource(R.string.loyverse_description_reconnect)
                        else -> stringResource(R.string.loyverse_description_not_connected)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = InterFontFamily, fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            connection == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
            status == LoyverseStatus.CONNECTED -> TextButton(
                onClick = onDisconnect,
                enabled = !busy,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.loyverse_disconnect_button),
                    color = MaterialTheme.colorScheme.error, fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            else -> Button(
                onClick = onConnect,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OnNeonGreen),
            ) {
                if (busy) {
                    CircularProgressIndicator(color = OnNeonGreen, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        stringResource(
                            if (status == LoyverseStatus.RECONNECT_REQUIRED) R.string.loyverse_reconnect_button
                            else R.string.loyverse_connect_button
                        ),
                        fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguagePicker() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(AppLocale.getLanguage(context)) }
    val options = listOf(
        AppLocale.FRENCH to stringResource(R.string.language_name_fr),
        AppLocale.ENGLISH to stringResource(R.string.language_name_en),
        AppLocale.ARABIC to stringResource(R.string.language_name_ar),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceL2, RoundedCornerShape(12.dp)),
    ) {
        options.forEachIndexed { index, (code, name) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = code != selected) {
                        selected = code
                        AppLocale.setLanguage(context, code)
                        (context as? Activity)?.recreate()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    color = if (code == selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = InterFontFamily,
                    fontWeight = if (code == selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                )
                if (code == selected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonGreen,
                        modifier = Modifier.size(18.dp))
                }
            }
            if (index != options.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceL3))
            }
        }
    }
}