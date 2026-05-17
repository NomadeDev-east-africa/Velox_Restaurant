# Travaux 5 — Velox Restaurant (session 5)

Récapitulatif des modifications apportées lors de la session d'analyse + refactoring.

---

## Analyse initiale du projet (MCPs Android)

Session d'analyse complète avec les MCPs `android-skills`, `javadocs` et `kotzilla`.
Stack vérifié : AGP 8.7.3 · Kotlin 2.1.0 · KSP 2.1.0-1.0.29 · Compose BOM 2025.02.00 ·
Hilt 2.56 · Navigation Compose 2.8.9 · Room 2.7.0 · Firebase BOM 33.9.0.

---

## Fix 1 — `markReady` : batch write → transaction atomique

**Fichier :** `data/remote/OrderRepositoryImpl.kt`

**Problème :** `markReady()` faisait un `get()` suivi d'un `batch.commit()` séparés.
Entre les deux opérations, un autre client pouvait modifier la commande → race condition
sur `totalOrders` / `totalRevenue`.

**Fix :** Remplacement par `firestore.runTransaction { transaction -> ... }`.
Dans une transaction Firestore, le `get()` et tous les `update()` sont atomiques :
Firestore réessaie automatiquement si un conflit est détecté.

```kotlin
// Avant
val orderDoc = orders.document(orderId).get().await()
val batch = firestore.batch()
batch.update(...)
batch.commit().await()

// Après
firestore.runTransaction { transaction ->
    val orderDoc = transaction.get(orders.document(orderId))
    transaction.update(orders.document(orderId), ...)
    transaction.update(firestore.collection("restaurants").document(restaurantId), ...)
}.await()
```

---

## Fix 2 — `collectAsStateWithLifecycle` dans HomeScreen

**Fichier :** `ui/home/HomeScreen.kt`

**Problème :** `ordersViewModel.uiState.collectAsState()` continue à collecter le flow
même quand l'app est en background, gaspillant des ressources.

**Fix :** Remplacement par `collectAsStateWithLifecycle()` (lifecycle-aware) — le flow
est suspendu automatiquement quand le Lifecycle passe sous `STARTED`.

```kotlin
// Avant
import androidx.compose.runtime.collectAsState
val ordersState by ordersViewModel.uiState.collectAsState()

// Après
import androidx.lifecycle.compose.collectAsStateWithLifecycle
val ordersState by ordersViewModel.uiState.collectAsStateWithLifecycle()
```

---

## Fix 3 — Double-stream Room + Firestore dans `OrdersViewModel`

**Fichier :** `ui/orders/OrdersViewModel.kt`

**Problème :** Deux coroutines tournaient simultanément :
1. Flow Room → émet au démarrage depuis le cache
2. Flow Firestore → émet les données live et appelle `cacheActiveOrders()`

Quand Firestore écrit dans Room, Room ré-émet → la coroutine Room écrase l'état
Firestore avec des données légèrement périmées. La condition `if (isLoading)` était
une protection fragile (race condition entre coroutines).

**Fix :** Le `cacheJob` (coroutine Room) est stocké et annulé dès que Firestore
émet son premier résultat. Firestore devient l'unique source de vérité en live ;
Room reste uniquement pour le chargement initial hors-ligne.

```kotlin
// Avant
viewModelScope.launch {
    orderRepository.streamCachedActiveOrders(restaurantId).collect { cached ->
        if (_uiState.value.isLoading) splitAndUpdate(cached)  // fragile
    }
}

// Après
val cacheJob = viewModelScope.launch {
    orderRepository.streamCachedActiveOrders(restaurantId).collect { cached ->
        splitAndUpdate(cached)
    }
}
viewModelScope.launch {
    orderRepository.streamActiveOrders(restaurantId)
        .catch { ... }
        .collect { orders ->
            cacheJob.cancel()  // annule le flow Room proprement
            orderRepository.cacheActiveOrders(orders)
            splitAndUpdate(orders, isLoading = false)
        }
}
```

---

## Fix 4 — Préservation état des tabs (TabContent)

**Fichier :** `ui/home/HomeScreen.kt`

**Problème :** Le bloc `when (selectedTab)` détruisait et recréait chaque écran à
chaque changement d'onglet. L'état de scroll, les champs de formulaire, et surtout
les ViewModels de chaque écran (Menu, Stats, Promos, Profile) étaient réinitialisés.

**Fix :** Nouveau composable `TabContent(visible, content)` — tous les 5 écrans
restent **toujours dans la composition**. Seule leur visibilité change :
- `zIndex(1f)` sur l'onglet actif → reçoit tous les événements tactiles
- `zIndex(0f)` + `alpha(0f)` sur les autres → invisibles et non-interactifs
- Les ViewModels et états de scroll survivent aux changements d'onglet

```kotlin
@Composable
private fun TabContent(visible: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (visible) 1f else 0f)
            .then(if (!visible) Modifier.alpha(0f) else Modifier),
    ) {
        content()
    }
}

// Usage
Box(Modifier.weight(1f)) {
    TabContent(selectedTab == 0) { OrdersScreen(viewModel = ordersViewModel) }
    TabContent(selectedTab == 1) { MenuScreen() }
    TabContent(selectedTab == 2) { PromotionsScreen() }
    TabContent(selectedTab == 3) { StatsScreen() }
    TabContent(selectedTab == 4) { ProfileScreen(onLogout = onLogout) }
}
```

---

## Fix 5 — Navigation type-safe `@Serializable`

**Fichiers :** `navigation/AppDestinations.kt`, `navigation/AppNavHost.kt`

**Problème :** Les routes étaient des `String` constants (`"login"`, `"home"`).
Aucune vérification à la compilation → erreurs silencieuses possibles en cas de typo.

**Fix :** Migration vers la navigation type-safe de Navigation Compose 2.8+ :
- `AppDestinations.kt` remplacé par `sealed interface AppDestination` avec
  `@Serializable data object Login` et `@Serializable data object Home`
- `AppNavHost.kt` utilise `composable<AppDestination.Login>` et
  `popUpTo<AppDestination.Login> { inclusive = true }`
- Aucune dépendance supplémentaire : `kotlin-serialization` était déjà présent

```kotlin
// Avant
object AppDestinations {
    const val LOGIN = "login"
    const val HOME  = "home"
}
composable(AppDestinations.LOGIN) { ... }
popUpTo(AppDestinations.LOGIN) { inclusive = true }

// Après
sealed interface AppDestination {
    @Serializable data object Login : AppDestination
    @Serializable data object Home  : AppDestination
}
composable<AppDestination.Login> { ... }
popUpTo<AppDestination.Login> { inclusive = true }
```

---

## Fichiers modifiés (récap)

| Fichier | Nature |
|---|---|
| `data/remote/OrderRepositoryImpl.kt` | markReady → runTransaction atomique |
| `ui/home/HomeScreen.kt` | collectAsStateWithLifecycle + TabContent |
| `ui/orders/OrdersViewModel.kt` | fix double-stream Room/Firestore |
| `navigation/AppDestinations.kt` | migration type-safe @Serializable |
| `navigation/AppNavHost.kt` | composable<T> + popUpTo<T> type-safe |
