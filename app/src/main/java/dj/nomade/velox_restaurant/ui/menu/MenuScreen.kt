package dj.nomade.velox_restaurant.ui.menu

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dj.nomade.velox_restaurant.data.model.Category
import dj.nomade.velox_restaurant.data.model.MenuItem
import dj.nomade.velox_restaurant.data.model.Promotion
import dj.nomade.velox_restaurant.ui.theme.InterFontFamily
import dj.nomade.velox_restaurant.ui.theme.ManropeFontFamily
import dj.nomade.velox_restaurant.ui.theme.NeonGreen
import dj.nomade.velox_restaurant.ui.theme.OnNeonGreen
import dj.nomade.velox_restaurant.ui.theme.SurfaceL1
import dj.nomade.velox_restaurant.ui.theme.SurfaceL2
import dj.nomade.velox_restaurant.ui.theme.SurfaceL3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(viewModel: MenuViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showForm by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MenuItem?>(null) }
    var showManageCategories by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        if (showManageCategories) {
            ManageCategoriesView(
                categories = uiState.categories,
                onBack = { showManageCategories = false },
                onAdd = { name -> viewModel.addCategory(name) },
                onDelete = { cat -> viewModel.deleteCategory(cat) },
            )
        } else {
            Column(Modifier.fillMaxSize()) {

                // ── Header ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                    TextButton(onClick = { showManageCategories = true }) {
                        Icon(
                            Icons.Default.Category, null,
                            tint = NeonGreen,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Gérer les catégories",
                            color = NeonGreen,
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // ── Filtre catégories ─────────────────────────────────────
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    item {
                        CategoryChip(
                            label = "Toutes",
                            selected = uiState.selectedCategory == "Toutes",
                            onClick = { viewModel.selectCategory("Toutes") },
                        )
                    }
                    items(uiState.categories) { cat ->
                        CategoryChip(
                            label = cat.name,
                            selected = uiState.selectedCategory == cat.name,
                            onClick = { viewModel.selectCategory(cat.name) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

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
                            onToggle = { viewModel.toggleAvailability(item) },
                            onEdit = { editingItem = item; showForm = true },
                            onDelete = { viewModel.deleteItem(item.id) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // ── FAB ajouter ───────────────────────────────────────────────
            FloatingActionButton(
                onClick = { editingItem = null; showForm = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = NeonGreen,
                contentColor = OnNeonGreen,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un plat")
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // ── Bottom sheet formulaire ───────────────────────────────────────────────
    if (showForm) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showForm = false },
            sheetState = sheetState,
            containerColor = SurfaceL1,
        ) {
            MenuItemForm(
                initialItem = editingItem,
                categories = uiState.categories,
                isSaving = uiState.isSaving,
                onAddCategory = { name -> viewModel.addCategory(name) },
                onSave = { item, uri ->
                    viewModel.saveItem(item, uri)
                    showForm = false
                },
                onDismiss = { showForm = false },
            )
        }
    }
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

// ── Carte plat ────────────────────────────────────────────────────────────────

@Composable
private fun MenuItemCard(
    item: MenuItem,
    promotion: Promotion?,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceL3),
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

        Column(horizontalAlignment = Alignment.End) {
            Switch(
                checked = item.isAvailable,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnNeonGreen,
                    checkedTrackColor = NeonGreen,
                    uncheckedTrackColor = SurfaceL3,
                ),
            )
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── Formulaire ajout/édition ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuItemForm(
    initialItem: MenuItem?,
    categories: List<Category>,
    isSaving: Boolean,
    onAddCategory: (String) -> Unit,
    onSave: (MenuItem, Uri?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var description by remember { mutableStateOf(initialItem?.description ?: "") }
    var price by remember { mutableStateOf(initialItem?.price?.toInt()?.toString() ?: "") }
    var prepTime by remember { mutableStateOf(initialItem?.preparationTime?.toString() ?: "15") }
    var selectedCategory by remember {
        mutableStateOf(initialItem?.category ?: categories.firstOrNull()?.name ?: "")
    }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imagePreviewError by remember(imageUri) { mutableStateOf(false) }

    // Correction du bug : réinitialiser l'erreur quand l'image change
    LaunchedEffect(initialItem?.imageUrl) {
        imagePreviewError = false
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Sync la catégorie sélectionnée si la liste de catégories change
    LaunchedEffect(categories) {
        if (selectedCategory.isBlank() && categories.isNotEmpty()) {
            selectedCategory = categories.first().name
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
        imagePreviewError = false  // Reset error on new image
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonGreen,
        unfocusedBorderColor = SurfaceL3,
        focusedLabelColor = NeonGreen,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = SurfaceL2,
        unfocusedContainerColor = SurfaceL2,
    )

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                if (initialItem == null) "NOUVEAU PLAT" else "MODIFIER LE PLAT",
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = NeonGreen,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
        }

        // ── Image ──
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceL2)
                    .border(1.dp, SurfaceL3, RoundedCornerShape(12.dp))
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                val displayUri = imageUri ?: if (!imagePreviewError) initialItem?.imageUrl?.let { Uri.parse(it) } else null
                if (displayUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(displayUri)
                            .size(400, 400)  // ← REDIMENSIONNEMENT CRUCIAL
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { imagePreviewError = true },
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null,
                            tint = NeonGreen, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("AJOUTER UNE IMAGE",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = InterFontFamily, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }

        item {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("NOM DU PLAT") }, modifier = Modifier.fillMaxWidth(),
                colors = fieldColors, shape = RoundedCornerShape(10.dp),
                placeholder = { Text("ex: Filet de Mignon au Poivre",
                    color = MaterialTheme.colorScheme.onSurfaceVariant) })
        }

        item {
            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("DESCRIPTION") }, modifier = Modifier.fillMaxWidth(),
                colors = fieldColors, shape = RoundedCornerShape(10.dp), minLines = 3,
                placeholder = { Text("Ingrédients, provenance, saveurs…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant) })
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text("PRIX (FDJ)") },
                    modifier = Modifier.weight(1f),
                    colors = fieldColors,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        Text("FDJ", color = NeonGreen,
                            fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp))
                    },
                )
                OutlinedTextField(
                    value = prepTime,
                    onValueChange = { prepTime = it.filter { c -> c.isDigit() } },
                    label = { Text("PRÉP (MIN)") },
                    modifier = Modifier.weight(1f),
                    colors = fieldColors,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }

        // ── Dropdown catégorie + bouton "+" ───────────────────────────────
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("CATÉGORIE") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                        },
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        containerColor = SurfaceL2,
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(cat.name, fontFamily = InterFontFamily,
                                        color = MaterialTheme.colorScheme.onSurface)
                                },
                                onClick = {
                                    selectedCategory = cat.name
                                    categoryDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                // Bouton "+" pour ajouter une catégorie inline
                IconButton(
                    onClick = { showAddCategoryDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nouvelle catégorie",
                        tint = NeonGreen, modifier = Modifier.size(22.dp))
                }
            }
        }

        item {
            Button(
                onClick = {
                    val item = (initialItem ?: MenuItem()).copy(
                        name = name,
                        description = description,
                        price = price.toDoubleOrNull() ?: 0.0,
                        preparationTime = prepTime.toIntOrNull() ?: 15,
                        category = selectedCategory,
                    )
                    onSave(item, imageUri)
                },
                enabled = !isSaving && name.isNotBlank() && price.isNotBlank() && selectedCategory.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen, contentColor = OnNeonGreen),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = OnNeonGreen, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        if (initialItem == null) "AJOUTER LE PLAT  +" else "ENREGISTRER",
                        fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp, letterSpacing = 2.sp,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Dialog ajout catégorie inline ─────────────────────────────────────────
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { newName ->
                onAddCategory(newName)
                selectedCategory = newName
                showAddCategoryDialog = false
            },
        )
    }
}

// ── Dialog ajout catégorie ────────────────────────────────────────────────────

@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceL2,
        title = {
            Text("Nouvelle catégorie", fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    focusedLabelColor = NeonGreen,
                    cursorColor = NeonGreen,
                    unfocusedBorderColor = SurfaceL3,
                    focusedContainerColor = SurfaceL3,
                    unfocusedContainerColor = SurfaceL3,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OnNeonGreen),
            ) { Text("Ajouter") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

// ── Écran gestion des catégories ──────────────────────────────────────────────

@Composable
private fun ManageCategoriesView(
    categories: List<Category>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (Category) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "CATÉGORIES",
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories, key = { it.id }) { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceL2, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (cat.isDefault) NeonGreen.copy(0.12f)
                                    else SurfaceL3,
                                    RoundedCornerShape(8.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (cat.isDefault) Icons.Default.Lock else Icons.Default.Category,
                                contentDescription = null,
                                tint = if (cat.isDefault) NeonGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            cat.name,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (cat.isDefault) {
                            Text(
                                "DÉFAUT",
                                fontFamily = InterFontFamily,
                                fontSize = 9.sp,
                                color = NeonGreen,
                                letterSpacing = 1.sp,
                            )
                        } else {
                            IconButton(
                                onClick = { deleteTarget = cat },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Default.Delete, contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = NeonGreen,
            contentColor = OnNeonGreen,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouvelle catégorie")
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name -> onAdd(name); showAddDialog = false },
        )
    }

    deleteTarget?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceL2,
            title = {
                Text("Supprimer la catégorie",
                    fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text("Supprimer « ${cat.name} » ? Les plats de cette catégorie ne seront pas supprimés.",
                    fontFamily = InterFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                Button(
                    onClick = { onDelete(cat); deleteTarget = null },
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