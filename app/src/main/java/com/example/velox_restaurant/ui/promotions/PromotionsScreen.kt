package com.example.velox_restaurant.ui.promotions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.velox_restaurant.data.model.Category
import com.example.velox_restaurant.data.model.MenuItem
import com.example.velox_restaurant.data.model.Promotion
import com.example.velox_restaurant.ui.theme.InterFontFamily
import com.example.velox_restaurant.ui.theme.ManropeFontFamily
import com.example.velox_restaurant.ui.theme.NeonGreen
import com.example.velox_restaurant.ui.theme.OnNeonGreen
import com.example.velox_restaurant.ui.theme.SurfaceL2
import com.example.velox_restaurant.ui.theme.SurfaceL3
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsScreen(viewModel: PromotionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Promotion?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "PROMOTIONS",
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val activeCount = state.promotions.count { it.isActive && !it.isExpired }
                if (activeCount > 0) {
                    Text(
                        "$activeCount ACTIVES",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }

            when {
                state.isLoading -> Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NeonGreen)
                }

                state.promotions.isEmpty() -> Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocalOffer, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Aucune promotion",
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            fontSize = 15.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Appuyez sur + pour en créer une",
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.35f),
                            fontSize = 12.sp,
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.promotions, key = { it.id }) { promo ->
                        PromotionCard(
                            promotion = promo,
                            onToggle = { viewModel.togglePromotion(promo.id, promo.isActive) },
                            onDelete = { deleteTarget = promo },
                        )
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = NeonGreen,
            contentColor = OnNeonGreen,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter une promotion")
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showAddSheet) {
        AddPromotionSheet(
            isSaving = state.isSaving,
            menuItems = state.menuItems,
            categories = state.categories,
            onDismiss = { showAddSheet = false },
            onSave = { type, targetName, discount, label, endDate ->
                viewModel.addPromotion(type, targetName, discount, label, endDate)
                showAddSheet = false
            },
        )
    }

    deleteTarget?.let { promo ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceL2,
            title = {
                Text(
                    "Supprimer la promotion",
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    "Supprimer « ${promo.label} » ?",
                    fontFamily = InterFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deletePromotion(promo.id); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPromotionSheet(
    isSaving: Boolean,
    menuItems: List<MenuItem>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (type: String, targetName: String, discount: Int, label: String, endDate: Long?) -> Unit,
) {
    var type by remember { mutableStateOf("item") }
    var targetName by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var discount by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var hasEndDate by remember { mutableStateOf(false) }
    var endDays by remember { mutableStateOf("7") }

    LaunchedEffect(type) {
        targetName = ""
        dropdownExpanded = false
    }

    val options = if (type == "item") menuItems.map { it.name } else categories.map { it.name }

    val discountInt = discount.toIntOrNull()
    val isValid = targetName.isNotBlank()
            && discountInt != null && discountInt in 1..100
            && label.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceL2,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "NOUVELLE PROMOTION",
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("item" to "PLAT", "category" to "CATÉGORIE").forEach { (value, chipLabel) ->
                    FilterChip(
                        selected = type == value,
                        onClick = { type = value },
                        label = {
                            Text(chipLabel, fontFamily = InterFontFamily, fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen.copy(alpha = 0.2f),
                            selectedLabelColor = NeonGreen,
                        ),
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
            ) {
                OutlinedTextField(
                    value = targetName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (type == "item") "Sélectionner un plat" else "Sélectionner une catégorie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = sheetFieldColors(),
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(SurfaceL2),
                ) {
                    if (options.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (type == "item") "Aucun plat disponible" else "Aucune catégorie disponible",
                                    fontFamily = InterFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = { dropdownExpanded = false },
                        )
                    } else {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(option, fontFamily = InterFontFamily,
                                        color = MaterialTheme.colorScheme.onSurface)
                                },
                                onClick = {
                                    targetName = option
                                    dropdownExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = discount,
                onValueChange = { discount = it.filter { c -> c.isDigit() } },
                label = { Text("Remise (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = sheetFieldColors(),
                singleLine = true,
                trailingIcon = {
                    Text("%", color = NeonGreen, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp))
                },
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Libellé affiché") },
                placeholder = {
                    Text(
                        "ex: Promo weekend -20%",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = sheetFieldColors(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Date d'expiration",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = hasEndDate,
                    onCheckedChange = { hasEndDate = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = NeonGreen,
                        checkedThumbColor = OnNeonGreen,
                        uncheckedTrackColor = SurfaceL3,
                    ),
                )
            }

            AnimatedVisibility(visible = hasEndDate) {
                OutlinedTextField(
                    value = endDays,
                    onValueChange = { endDays = it.filter { c -> c.isDigit() } },
                    label = { Text("Expire dans (jours)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = sheetFieldColors(),
                    singleLine = true,
                )
            }

            Button(
                onClick = {
                    val endDate = if (hasEndDate) {
                        val days = endDays.toLongOrNull() ?: 7L
                        System.currentTimeMillis() + days * 24 * 60 * 60 * 1000L
                    } else null
                    onSave(type, targetName, discountInt!!, label, endDate)
                },
                enabled = isValid && !isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = OnNeonGreen,
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = OnNeonGreen,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        "CRÉER LA PROMOTION",
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PromotionCard(
    promotion: Promotion,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val isEffective = promotion.isActive && !promotion.isExpired
    val dateFormat = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceL2, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (isEffective) NeonGreen.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${promotion.discountPercent}%",
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = if (isEffective) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                promotion.label,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    promotion.targetName,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                promotion.endDate?.let { end ->
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(
                        if (promotion.isExpired) "EXPIRÉ"
                        else "jusqu'au ${dateFormat.format(Date(end))}",
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        color = if (promotion.isExpired) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Switch(
            checked = isEffective,
            onCheckedChange = { if (!promotion.isExpired) onToggle() },
            enabled = !promotion.isExpired,
            colors = SwitchDefaults.colors(
                checkedTrackColor = NeonGreen,
                checkedThumbColor = OnNeonGreen,
                uncheckedTrackColor = SurfaceL3,
            ),
        )

        Spacer(Modifier.width(4.dp))

        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Delete, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun sheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonGreen,
    focusedLabelColor = NeonGreen,
    cursorColor = NeonGreen,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedContainerColor = SurfaceL3,
    focusedContainerColor = SurfaceL3,
)
