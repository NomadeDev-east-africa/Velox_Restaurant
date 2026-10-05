package dj.nomade.velox_restaurant.ui.menu

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dj.nomade.velox_restaurant.R
import dj.nomade.velox_restaurant.data.model.MenuItem
import dj.nomade.velox_restaurant.data.model.Promotion
import dj.nomade.velox_restaurant.data.model.normalizeOptionName
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
    val sectionOptions by viewModel.sectionOptions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Le sélecteur d'image est partagé : on retient le plat visé le temps du choix.
    var pendingImageItem by remember { mutableStateOf<MenuItem?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingImageItem
        pendingImageItem = null
        if (uri != null && target != null) viewModel.changeItemImage(target, uri)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    val isOptionSection = uiState.selectedCategory in MenuViewModel.OPTION_SECTIONS

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
                    stringResource(R.string.menu_header),
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

            // ── Liste plats / options ─────────────────────────────────
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                if (isOptionSection) {
                    // Les sauces et suppléments ne sont pas des plats : ils vivent dans
                    // le champ `optionGroups` de chaque plat, agrégés ici en une ligne.
                    item {
                        Text(
                            stringResource(R.string.menu_options_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                        )
                    }
                    items(sectionOptions, key = { "${it.group}|${it.name}|${it.price}" }) { option ->
                        ExtraOptionCard(
                            option = option,
                            isToggling = uiState.togglingChoice == option.name.normalizeOptionName(),
                            onToggle = { available -> viewModel.setExtraAvailability(option, available) },
                        )
                    }
                    if (sectionOptions.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.menu_options_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    }
                } else {
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
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ── Ligne sauce / supplément ──────────────────────────────────────────────────

@Composable
private fun ExtraOptionCard(
    option: ExtraOption,
    isToggling: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceL2, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                option.name,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (option.available) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            val itemCountLabel = pluralStringResource(
                R.plurals.menu_option_item_count, option.itemCount, option.itemCount,
            )
            val stockSuffix = when {
                !option.available -> " · " + stringResource(R.string.menu_option_out_of_stock)
                !option.isUniform -> " · " + stringResource(R.string.menu_option_mixed_state)
                else -> ""
            }
            Text(
                "${option.group.uppercase()} · $itemCountLabel$stockSuffix",
                color = if (option.available) MaterialTheme.colorScheme.onSurfaceVariant else NeonGreen,
                fontSize = 9.sp,
                fontFamily = InterFontFamily,
                letterSpacing = 1.sp,
            )
        }

        Text(
            if (option.price > 0) stringResource(R.string.price_plus_fdj, option.price.toInt())
            else stringResource(R.string.menu_price_included),
            color = if (option.price > 0) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
        )

        Spacer(Modifier.width(10.dp))

        if (isToggling) {
            Box(Modifier.size(51.dp, 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp))
            }
        } else {
            Switch(
                checked = option.available,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnNeonGreen,
                    checkedTrackColor = NeonGreen,
                    uncheckedTrackColor = SurfaceL3,
                ),
            )
        }
    }
}

// ── Chip catégorie ────────────────────────────────────────────────────────────

@Composable
private fun categoryDisplayLabel(raw: String): String = when (raw) {
    MenuViewModel.ALL -> stringResource(R.string.menu_category_all)
    MenuViewModel.SAUCES -> stringResource(R.string.menu_category_sauces)
    MenuViewModel.SUPPLEMENTS -> stringResource(R.string.menu_category_supplements)
    MenuViewModel.LEGUMES -> stringResource(R.string.menu_category_legumes)
    else -> raw
}

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
            categoryDisplayLabel(label).uppercase(),
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
                        contentDescription = stringResource(R.string.content_description_change_photo),
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
                        stringResource(R.string.menu_promo_badge, promotion.discountPercent),
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
                        stringResource(R.string.price_fdj, item.price.toInt()),
                        style = TextStyle(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                    )
                    Text(
                        stringResource(R.string.price_fdj, promoPrice.toInt()),
                        color = NeonGreen,
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                }
            } else {
                Text(stringResource(R.string.price_fdj, item.price.toInt()), color = NeonGreen,
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
    }
}
