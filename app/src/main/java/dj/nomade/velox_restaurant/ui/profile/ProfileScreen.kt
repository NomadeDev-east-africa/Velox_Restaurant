package dj.nomade.velox_restaurant.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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
                        .clickable { imagePicker.launch("image/*") },
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
                                if (restaurant.isOpen) "OUVERT" else "FERMÉ",
                                color = if (restaurant.isOpen) NeonGreen else MaterialTheme.colorScheme.error,
                                fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            )
                        }
                    }
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Changer la photo",
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
                        Text("Restaurant ouvert",
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
                    SectionTitle("INFORMATIONS")
                    ProfileInfoCard(Icons.Default.Mail, "EMAIL CONTACT", restaurant.email)
                    Spacer(Modifier.height(8.dp))
                    ProfileInfoCard(Icons.Default.Phone, "PHONE LINE", restaurant.phone)
                    Spacer(Modifier.height(8.dp))
                    ProfileInfoCard(Icons.Default.LocationOn, "HEADQUARTERS", restaurant.address)

                    Spacer(Modifier.height(24.dp))

                    // ── Stats rapides ─────────────────────────────────────
                    SectionTitle("STATISTIQUES GLOBALES")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MiniStatCard("Total Commandes",
                            uiState.totalOrders.toString(), Modifier.weight(1f))
                        MiniStatCard("Revenus Totaux",
                            "${uiState.totalRevenue.toInt()} FDJ", Modifier.weight(1f),
                            valueColor = NeonGreen)
                    }

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
                        Text("LOGOUT TERMINAL", fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.5.sp)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Nomade253 Restaurant v1.0.0",
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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = SurfaceL2,
            title = {
                Text("Déconnexion", fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text("Confirmer la déconnexion du terminal ?",
                    fontFamily = InterFontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.logout(); showLogoutDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Déconnecter") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(value.ifBlank { "—" }, color = MaterialTheme.colorScheme.onSurface,
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