package dj.nomade.velox_restaurant.ui.menu

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dj.nomade.velox_restaurant.data.model.MenuItem
import dj.nomade.velox_restaurant.data.model.Promotion
import dj.nomade.velox_restaurant.ui.theme.InterFontFamily
import dj.nomade.velox_restaurant.ui.theme.ManropeFontFamily
import dj.nomade.velox_restaurant.ui.theme.NeonGreen
import dj.nomade.velox_restaurant.ui.theme.OnNeonGreen
import dj.nomade.velox_restaurant.ui.theme.SurfaceL2
import dj.nomade.velox_restaurant.ui.theme.SurfaceL3

@Composable
fun MenuScreen(viewModel: MenuViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Le sélecteur d'image est partagé : on retient le plat visé le temps du choix.
    var pendingImageItem by remember { mutableStateOf<MenuItem?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = pendingImageItem
        pendingImageItem = null
        if (uri != null && target != null) viewModel.changeItemImage(target, uri)
    }

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    val isEditable = uiState.selectedCategory in MenuViewModel.EDITABLE

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "MENU",
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // ── Filtre catégories ─────────────────────────────────────
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                item {
                    CategoryChip(
                        label = MenuViewModel.ALL,
                        selected = uiState.selectedCategory == MenuViewModel.ALL,
                        onClick = { viewModel.selectCategory(MenuViewModel.ALL) },
                    )
                }
                items(categories, key = { it }) { name ->
                    CategoryChip(
                        label = name,
                        selected = uiState.selectedCategory == name,
                        onClick = { viewModel.selectCategory(name) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Ajout d'une sauce / d'un supplément ───────────────────
            if (isEditable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(SurfaceL2, RoundedCornerShape(10.dp))
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonGreen,
                        modifier = Modifier.size(18.dp))
                    Text(
                        "Ajouter ${uiState.selectedCategory.lowercase().removeSuffix("s")}",
                        color = NeonGreen,
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Liste plats ───────────────────────────────────────────
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    val promo = uiState.activePromotions.firstOrNull { p ->
                        (p.type == "item" && p.targetName.equals(item.name, ignoreCase = true)) ||
                                (p.type == "category" && p.targetName.equals(item.category, ignoreCase = true))
                    }
                    MenuItemCard(
                        item = item,
                        promotion = promo,
                        isUploadingImage = uiState.uploadingImageFor == item.id,
                        onToggle = { viewModel.toggleAvailability(item) },
                        onEditImage = {
                            pendingImageItem = item
                            imagePicker.launch("image/*")
                        },
                        onDelete = if (item.category in MenuViewModel.EDITABLE) {
                            { viewModel.deleteExtra(item) }
                        } else null,
                    )
                }
                if (isEditable && filteredItems.isEmpty()) {
                    item {
                        Text(
                            "Aucun élément dans cette catégorie pour le moment.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showAddDialog) {
        AddExtraDialog(
            category = uiState.selectedCategory,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, price ->
                viewModel.addExtra(name, price, uiState.selectedCategory)
                showAddDialog = false
            },
        )
    }
}

// ── Dialogue d'ajout sauce / supplément ───────────────────────────────────────

@Composable
private fun AddExtraDialog(
    category: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val parsedPrice = price.toDoubleOrNull()
    // Les règles Firestore imposent `price > 0` à la création d'un menuItem :
    // un prix à 0 partirait en PERMISSION_DENIED. On bloque donc en amont.
    val canConfirm = name.isNotBlank() && parsedPrice != null && parsedPrice > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceL2,
        title = {
            Text(
                "Nouvelle ${category.lowercase().removeSuffix("s")}",
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text("Prix (FDJ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, parsedPrice ?: 0.0) }, enabled = canConfirm) {
                Text("Ajouter", color = if (canConfirm) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

// ── Chip catégorie ────────────────────────────────────────────────────────────

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) NeonGreen else SurfaceL2, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(),
            color = if (selected) OnNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
        )
    }
}

// ── Carte plat (photo modifiable + bascule disponibilité) ──────────────────────

@Composable
private fun MenuItemCard(
    item: MenuItem,
    promotion: Promotion?,
    isUploadingImage: Boolean,
    onToggle: () -> Unit,
    onEditImage: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    var imageError by remember(item.imageUrl) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceL2, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Vignette cliquable : seule la photo est modifiable, jamais le nom ni le prix.
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceL3)
                .clickable(enabled = !isUploadingImage) { onEditImage() },
            contentAlignment = Alignment.Center,
        ) {
            if (!item.imageUrl.isNullOrBlank() && !imageError) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.imageUrl)
                        .size(200, 200)  // ← REDIMENSIONNEMENT CRUCIAL
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { imageError = true },
                )
            } else {
                Text("🍽", fontSize = 28.sp)
            }

            if (isUploadingImage) {
                Box(
                    modifier = Modifier.fillMaxSize().background(SurfaceL3.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp))
                }
            } else {
                // Pastille appareil photo : rend l'action découvrable.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .background(NeonGreen, RoundedCornerShape(50))
                        .padding(3.dp),
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Changer la photo",
                        tint = OnNeonGreen,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            if (promotion != null) {
                Box(
                    modifier = Modifier
                        .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "PROMO -${promotion.discountPercent}%",
                        color = NeonGreen,
                        fontSize = 9.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
            Text(item.name, fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold,
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(item.category.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp, fontFamily = InterFontFamily, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            if (promotion != null) {
                val promoPrice = item.price * (1 - promotion.discountPercent / 100.0)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "${item.price.toInt()} FDJ",
                        style = TextStyle(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                    )
                    Text(
                        "${promoPrice.toInt()} FDJ",
                        color = NeonGreen,
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                }
            } else {
                Text("${item.price.toInt()} FDJ", color = NeonGreen,
                    fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }

        Switch(
            checked = item.isAvailable,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnNeonGreen,
                checkedTrackColor = NeonGreen,
                uncheckedTrackColor = SurfaceL3,
            ),
        )

        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Supprimer ${item.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
