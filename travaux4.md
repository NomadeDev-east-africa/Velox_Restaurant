# Travaux Session 4 — 2026-05-05

## Résumé des corrections appliquées

---

## 1. Performance — `StatsViewModel.buildWeeklyData()` parallélisé

**Fichier :** `app/src/main/java/com/example/velox_restaurant/ui/stats/StatsViewModel.kt`

**Problème :** 7 requêtes Firestore séquentielles au chargement des stats → temps total = 7× la latence réseau.

**Correction :** Remplacement par `coroutineScope { ... async { } ... .map { it.await() } }` — les 7 requêtes partent simultanément.

**Imports ajoutés :** `kotlinx.coroutines.async`, `kotlinx.coroutines.coroutineScope`

---

## 2. Performance — Coil `ImageLoaderFactory` global

**Fichier :** `app/src/main/java/com/example/velox_restaurant/RestaurantApplication.kt`

**Problème :** Coil sans configuration globale → instances non partagées, `DiskLruCache` en contention, décoding non optimisé.

**Correction :** `RestaurantApplication` implémente `ImageLoaderFactory` avec :
- Cache mémoire : 20% de la RAM
- Cache disque : 2% du stockage, répertoire `image_cache`
- `crossfade(true)`

---

## 3. ProGuard — Règle `SnapshotStateList`

**Fichier :** `app/proguard-rules.pro`

**Problème :** Warning `failed lock verification` sur `SnapshotStateList` en build Release.

**Correction :** Ajout de `-keep class androidx.compose.runtime.snapshots.SnapshotStateList { *; }`

---

## 4. Fix critique — Logo 856 KB décodé sur le main thread

**Fichiers :**
- `app/src/main/res/drawable/logo_restaurant.png`
- `app/src/main/java/com/example/velox_restaurant/ui/auth/LoginScreen.kt`

**Problème :** `logo_restaurant.png` = 1254×1254px, 856 KB. Utilisé avec `painterResource()` = décodage **synchrone sur le main thread** → freeze Davey 1333ms/1362ms au démarrage.

**Corrections :**
1. Image redimensionnée de 1254×1254px (856 KB) → 480×480px (215 KB) — réduction de 75%
2. `painterResource(R.drawable.logo_restaurant)` remplacé par `AsyncImage(model = R.drawable.logo_restaurant)` (Coil) — décodage en arrière-plan

**Résultat mesuré :** Freeze réduit de ~1333ms à ~1131ms (amélioration confirmée dans le log).

---

## 5. Fix — `OnyxTopBar` chargeait l'image à pleine résolution

**Fichier :** `app/src/main/java/com/example/velox_restaurant/ui/components/OnyxTopBar.kt`

**Problème :** `AsyncImage(model = restaurantImageUrl)` sans contrainte de taille → Coil décode l'image entière (ex. 476×256px) pour l'afficher dans un avatar de 36dp (≈108px). Decode inutile + contenu du log `Corrupt JPEG data: 19761 extraneous bytes`.

**Correction :** Remplacement par `ImageRequest.Builder(context).data(url).size(108, 108).crossfade(true).build()` — décode uniquement les pixels nécessaires.

---

## 6. Fix — Compression des images avant upload Firebase Storage

**Nouveau fichier :** `app/src/main/java/com/example/velox_restaurant/core/utils/ImageUtils.kt`

Utilitaire partagé :
```kotlin
fun Context.compressImageUri(uri: Uri, maxDimension: Int = 1080, quality: Int = 85): ByteArray
```
- Utilise `BitmapFactory.Options.inSampleSize` pour downsampler efficacement
- Compresse en JPEG au niveau de qualité demandé
- Recycle le `Bitmap` après compression

**Fichier :** `app/src/main/java/com/example/velox_restaurant/data/remote/MenuRepositoryImpl.kt`

- Ajout `@ApplicationContext context: Context` au constructeur
- `uploadMenuImage` : `putFile(uri)` → `putBytes(context.compressImageUri(uri, maxDimension = 800, quality = 85))`
- Photos uploadées à max 800×800px JPEG 85% (~50-100 KB au lieu de 3-5 MB)

**Fichier :** `app/src/main/java/com/example/velox_restaurant/data/remote/RestaurantRepositoryImpl.kt`

- Ajout `@ApplicationContext context: Context` au constructeur
- `uploadProfileImage` : `putFile(uri)` → `putBytes(context.compressImageUri(uri, maxDimension = 1080, quality = 85))`
- Bannière restaurant uploadée à max 1080×1080px JPEG 85%

---

## 7. Nettoyage — Suppression du package vide `com.nomade253`

**Répertoire supprimé :** `app/src/main/java/com/nomade253/`

**Raison :** Arborescence de dossiers vides (résidu d'un "Refactor → Rename Package" avorté). 0 fichier Kotlin dedans. Tout le code est dans `com.example.velox_restaurant`.

---

## 8. Fix — `NetworkMonitor.kt` receiver type mismatch (session précédente, confirmé)

**Fichier :** `app/src/main/java/com/example/velox_restaurant/core/utils/NetworkMonitor.kt`

`cm.isCurrentlyOnline()` → `context.isCurrentlyOnline()` (`cm` est `ConnectivityManager`, la fonction est une extension de `Context`)

---

## État des métriques après corrections

| Métrique | Avant | Après |
|---|---|---|
| Freeze démarrage (Davey) | 1333ms / 1362ms | 1131ms / 1165ms |
| Frames sautées | 78 frames | 65 frames |
| Logo taille | 1254×1254px, 856 KB | 480×480px, 215 KB |
| Logo décodage | Main thread (`painterResource`) | Background thread (Coil `AsyncImage`) |
| Upload images menu | Brut (3-5 MB) | Compressé max 800px, 85% JPEG |
| Upload bannière restaurant | Brut (3-5 MB) | Compressé max 1080px, 85% JPEG |
| OnyxTopBar avatar | Pleine résolution | Contraint à 108×108px |
| 7 requêtes Firestore stats | Séquentielles | Parallèles (`coroutineScope+async`) |

## Action manuelle restante

- **Vider le cache de l'app** une fois : `Paramètres → Applications → Velox Restaurant → Stockage → Vider le cache`
  → Élimine l'image JPEG corrompue déjà en cache Coil.

- **Firebase Storage** : Le MCP est connecté au projet Firebase par défaut, pas à `nomade253-478a9`. Les images uploadées sont accessibles via la Firebase Console → Storage → `restaurants/`.
