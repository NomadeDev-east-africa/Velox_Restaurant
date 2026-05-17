# Travaux 3 — Velox Restaurant (session 3)

Récapitulatif de toutes les modifications apportées au projet Android Kotlin `com.example.velox_restaurant` (Firebase : nomade253-478a9).

---

## Session 1 — Bugs UI commandes & promotions

### 1. Sauces et extras dans les cards de commande

**Fichiers modifiés :**
- `data/model/OrderItem.kt` — ajout des champs `sauces: List<String>` et `extras: List<String>`
- `data/remote/FirestoreMapper.kt` — deserialisation des sauces/extras depuis Firestore, compatible format `String` et `Map<*, *>{name: String}`
- `ui/orders/OrdersScreen.kt` — affichage des sauces (`› Sauces:`) et extras (`› Extras:`) dans `PendingOrderCard` et `ActiveOrderCard`; sidebar passée de hauteur fixe à `IntrinsicSize.Min + fillMaxHeight()`

### 2. Dropdown réel dans "Créer une promotion"

**Problème :** Le champ catégorie/plat était un `OutlinedTextField` libre.  
**Fix :** Remplacé par un `ExposedDropdownMenuBox` alimenté par les vrais plats et catégories du restaurant.

**Fichiers modifiés :**
- `ui/promotions/PromotionsViewModel.kt` — injection de `MenuRepository` et `CategoryRepository`; `PromotionsUiState` enrichi avec `menuItems` et `categories`
- `ui/promotions/PromotionsScreen.kt` — `AddPromotionSheet` utilise `ExposedDropdownMenuBox`; `LaunchedEffect(type)` remet à zéro `targetName` à chaque changement de type; message "Aucun plat/catégorie disponible" si liste vide

---

## Session 2 — Stats, badges promo, crashes

### 3. Stats totalOrders = 0

**Cause racine :** Le champ `totalOrders` dans Firestore n'était jamais incrémenté.  
**Fix :**
- `data/remote/OrderRepositoryImpl.kt` — `markReady()` exécute un batch write Firestore atomique qui incrémente `totalOrders` (+1) et `totalRevenue` (+total de la commande) sur `restaurants/{restaurantId}`
- `ui/stats/StatsViewModel.kt` — ajout d'un `streamRestaurant()` en live dans `init` pour que les stats se mettent à jour instantanément sans rechargement

### 4. Badge promotion + prix barré dans la tab Menu

**Fichiers modifiés :**
- `ui/menu/MenuViewModel.kt` — injection de `PromotionRepository`; streaming des promotions actives non expirées dans `MenuUiState.activePromotions`
- `ui/menu/MenuScreen.kt` — `MenuItemCard` reçoit `promotion: Promotion?`; affiche badge vert "PROMO -XX%", prix original barré (`TextDecoration.LineThrough`), prix réduit en vert

### 5. ProGuard rules (release build)

**Fichier :** `app/proguard-rules.pro`  
Règles ajoutées pour : Kotlin, Coroutines, Compose/SnapshotStateList, Firebase/Firestore (data models `com.example.velox_restaurant.data.model.**`), Hilt/Dagger, Room, Kotlinx Serialization, Coil, WorkManager.  
Sans ces règles, R8 obfusquait les data classes → désérialisation Firestore cassée en release.

### 6. Fix AnimatedVisibility dans HomeScreen

**Problème :** Le slot `badge` de `BadgedBox` est `RowScope.() -> Unit` → l'import `AnimatedVisibility` non qualifié résolvait vers `RowScope.AnimatedVisibility` (mauvaise surcharge).  
**Fix :** Suppression de l'import; utilisation de `androidx.compose.animation.AnimatedVisibility(...)` entièrement qualifié.

### 7. Fix withRetry — dead code

**Fichier :** `core/utils/RetryHelper.kt`  
**Bug :** `if (attempt == times - 2) throw e` dans la boucle rendait le `return block()` final inatteignable. Avec `times=3`, seulement 2 tentatives s'exécutaient.  
**Fix :** La boucle avale silencieusement les exceptions; le `return block()` final est la dernière tentative qui propage l'erreur.

### 8. Suppression dépendances MCP SDK

**Fichiers :** `app/build.gradle.kts`, `gradle/libs.versions.toml`  
Suppression de `mcp-kotlin-sdk-client` et `ktor-client-okhttp` qui causaient un conflit de ressources Android (`No package ID 0x6a found for resource ID 0x6a0b0013`). Ces libs sont des outils dev, pas destinés à une APK Android.

### 9. Permissions manifest

**Fichier :** `app/src/main/AndroidManifest.xml`  
Ajout de `ACCESS_NETWORK_STATE` (nécessaire pour `ConnectivityManager`).

---

## Session 3 — Réseau & images

### 10. NetworkMonitor — détection hors-ligne

**Fichier créé :** `core/utils/NetworkMonitor.kt`  
Composable `rememberNetworkState(): State<Boolean>` basé sur `ConnectivityManager.registerNetworkCallback` + `produceState`. Vérifie `NET_CAPABILITY_INTERNET`. Se désabonne proprement via `awaitDispose`.

### 11. Bandeau hors-ligne dans HomeScreen

**Fichier modifié :** `ui/home/HomeScreen.kt`

- `val isOnline by rememberNetworkState()`
- `statusBarsPadding()` déplacé du `Box` contenu vers le `Column` parent
- Bandeau orange animé (`slideInVertically + fadeIn` / `slideOutVertically + fadeOut`) en haut de l'écran quand `!isOnline`
- Icône `WifiOff` + texte "Hors ligne — données en cache"

### 12. Fallback images corrompues — MenuScreen

**Fichier modifié :** `ui/menu/MenuScreen.kt`

- **MenuItemCard** : `var imageError by remember(item.imageUrl) { mutableStateOf(false) }`; `onError = { imageError = true }` sur `AsyncImage` → retombe sur l'emoji 🍽
- **MenuItemForm** (aperçu image) : `var imagePreviewError by remember(imageUri, initialItem?.imageUrl)`; masque l'URL distante corrompue et affiche le placeholder caméra

### 13. Fallback bannière corrompue — ProfileScreen

**Fichier modifié :** `ui/profile/ProfileScreen.kt`

- `var bannerError by remember(restaurant?.imageUrl) { mutableStateOf(false) }` avant le bloc null-check
- `onError = { bannerError = true }` sur `AsyncImage` du hero banner → si le JPEG est corrompu, le dégradé vert de fond reste visible sans crash ni écran blanc

---

## Fichiers touchés (récap global)

| Fichier | Nature |
|---|---|
| `data/model/OrderItem.kt` | +sauces, +extras |
| `data/remote/FirestoreMapper.kt` | deserialisation sauces/extras |
| `data/remote/OrderRepositoryImpl.kt` | batch write incrémentation stats |
| `ui/orders/OrdersScreen.kt` | affichage sauces/extras, sidebar IntrinsicSize |
| `ui/promotions/PromotionsViewModel.kt` | injection menuItems/categories |
| `ui/promotions/PromotionsScreen.kt` | ExposedDropdownMenuBox |
| `ui/stats/StatsViewModel.kt` | streamRestaurant() live |
| `ui/menu/MenuViewModel.kt` | streaming promotions actives |
| `ui/menu/MenuScreen.kt` | badge promo, prix barré, fallback images |
| `ui/profile/ProfileScreen.kt` | fallback bannière |
| `ui/home/HomeScreen.kt` | bandeau hors-ligne, fix AnimatedVisibility |
| `core/utils/NetworkMonitor.kt` | NOUVEAU — détection réseau |
| `core/utils/RetryHelper.kt` | fix dead code |
| `app/proguard-rules.pro` | règles R8 complètes |
| `app/build.gradle.kts` | suppression MCP SDK, isShrinkResources |
| `gradle/libs.versions.toml` | suppression entrées MCP/Ktor |
| `app/src/main/AndroidManifest.xml` | ACCESS_NETWORK_STATE |
