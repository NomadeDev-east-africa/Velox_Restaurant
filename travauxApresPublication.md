# Travaux après publication — v1.0.0 → v1.0.1

Session du **21 juillet 2026**. Corrections faites suite aux retours reçus après
la mise en ligne de la v1.0.0 (versionCode 1) sur le Play Store.

Commit : `859a23f` — branche `fix/menu-categories-stats-comptables`
Version : **versionCode 2 / versionName 1.0.1**

---

## 1. Demandes traitées

### 1.1 Les catégories du menu étaient en dur

**Diagnostic.** `CategoryRepositoryImpl` lisait `restaurants/{uid}/categories`, une
sous-collection qui n'existait pas en base. La trouvant vide, `ensureDefaultCategories`
y écrivait 7 noms codés en dur — *Entrées, Pizzas, Burgers, Pâtes, Salades, Desserts,
Boissons* — identiques pour tous les restaurants.

Les vraies catégories vivent dans la collection **racine `menuCategories`** (~58 docs),
mais elle **n'a pas de champ `restaurantId`** : c'est un référentiel global partagé,
elle ne peut donc pas dire quelles catégories appartiennent à qui.

Exemple mesuré — **Restaurant Yemenite**, 40 plats, 8 catégories réelles :

| L'app affichait | La réalité |
|---|---|
| Entrées ✅ | Entrées |
| Pizzas ❌ vide | Accompagnements ⛔ absente |
| Burgers ❌ vide | Grillades & Brochettes ⛔ absente |
| Pâtes ❌ vide | Spécialités Yéménites ⛔ absente |
| Salades ✅ | Salades |
| Desserts ✅ | Desserts |
| Boissons ✅ | Boissons |
| — | Boissons Chaudes ⛔ absente |

**Correction.** Les chips proviennent maintenant des valeurs `category` distinctes des
plats du restaurant. `menuCategories` ne sert plus qu'à l'ordre d'affichage (`order`),
en lecture *fail-soft* : si elle est illisible, repli sur l'ordre alphabétique plutôt
qu'un écran cassé. `DEFAULTS`, `ensureDefaultCategories`, `addCategory` et
`deleteCategory` sont supprimés.

### 1.2 Catégories Sauces et Suppléments

Deux catégories **toujours proposées, même vides** (`MenuViewModel.EDITABLE`), pour que
le restaurateur puisse créer sa première sauce. Elles sont les seules où l'app autorise
la création et la suppression d'éléments.

- Bouton « + » → dialogue nom + prix
- Même interrupteur disponible/rupture que les plats
- Icône de suppression **limitée à ces deux catégories** — aucun risque de supprimer un
  plat par erreur

⚠️ Les règles Firestore imposent `price > 0` à la création d'un `menuItem`. Un article
gratuit est donc **structurellement impossible** ; la validation est faite en amont dans
l'UI et le ViewModel pour éviter un `PERMISSION_DENIED` incompréhensible. Si vous voulez
pouvoir offrir une sauce, c'est la règle qu'il faudra changer, pas l'app.

### 1.3 Modifier uniquement la photo d'un plat

Vignette cliquable avec pastille appareil photo et indicateur de progression.
`updateItemImage()` n'écrit que `imageUrl` et `updatedAt` — le nom, le prix et la
description sont intouchables depuis cet écran.

Conforme à la règle `menuItems/update` :
`onlyFields(['name','description','price','imageUrl','category','isAvailable','preparationTime','updatedAt'])`.

### 1.4 Promotions sur les plats uniquement

Le sélecteur PLAT / CATÉGORIE est retiré, `type` est figé à `"item"`.

**Motif.** 9 des 14 promotions en base étaient de type `category` et ciblaient les
fausses catégories (*Pizzas*, *Pâtes*, *Burgers*, *Entrées*) — elles ne remisaient rien.
La *lecture* des promos catégorie est conservée dans `MenuScreen` pour ne pas casser
l'existant ni désaligner l'app client ; seule leur création est supprimée.

---

## 2. Comptabilité — correction du chiffre d'affaires

### 🔴 Le CA affiché était surévalué

Décomposition réelle d'une commande (`5LVvYhPTzXSLmNZ79Usd`, Chez Davido) :

```
subtotal        4 510 FDJ   ← ce que le restaurant encaisse
deliveryFee       500 FDJ   ← revient au livreur / à la plateforme
                 ─────────
total           5 010 FDJ   ← subtotal + deliveryFee
driverEarnings    400 FDJ
```

L'app sommait **`total`** partout : `getTotalStats`, `todayRevenue`, `buildWeeklyData`.
Le restaurateur voyait donc **~500 FDJ de trop par commande**, soit environ **+11 %**.

**Correction.** `subtotal` et `discount` ajoutés au modèle `Order` (avec repli
`total - deliveryFee` pour les commandes anciennes qui n'ont pas le champ), propriété
`Order.restaurantRevenue`, cache Room aligné (DB **version 2**, `fallbackToDestructiveMigration`
déjà actif donc aucune migration à écrire).

> **Piège à retenir.** Ne jamais sommer `subtotal` via `AggregateField.sum` : les agrégats
> Firestore ignorent silencieusement les documents où le champ est absent, et les commandes
> d'avril/mai n'en ont pas — elles auraient compté pour 0 FDJ. On somme `total` et
> `deliveryFee` (présents partout) puis on soustrait : résultat identique, sans trou.

### Écran Stats refondu

- **Sélecteur Jour / Semaine / Mois / Année**
- Par période : commandes honorées, CA hors livraison, panier moyen, annulations + taux
- **Mode Année** : tableau comptable mois par mois, ligne TOTAL issue d'un agrégat annuel
  indépendant (elle recoupe donc la somme des lignes)
- Calculs en `aggregate queries` — quelques lectures facturées quel que soit le volume

**Défauts corrigés au passage :**

| Avant | Après |
|---|---|
| `todayOrders` comptait tout (annulées incluses), `todayRevenue` seulement les `completed` | Les deux portent sur les `completed`, les annulations ont leur propre case |
| `buildWeeklyData` lançait 7 requêtes parallèles de documents complets | Une seule requête de plage, répartie par jour en mémoire |
| `getOrdersForMonth` sans borne haute (`>= début` jusqu'à l'infini) | `getOrdersInRange(start, end)` |
| Cumuls « depuis l'ouverture » chargés une seule fois au `init` | Rafraîchis à chaque changement de période et à chaque retour sur l'écran |

Ce dernier point expliquait le « 0 commande honorée » observé après une commande réelle :
le ViewModel survit aux changements d'onglet, donc les cumuls restaient figés à leur
valeur de démarrage.

### Correction de `markReady`

L'échec d'écriture Firestore et l'échec d'envoi des notifications sont maintenant
distingués : si le statut passe bien à « prêt » mais que la notification échoue, le
restaurateur est prévenu que **le client n'a pas été averti**.

---

## 3. Nettoyage Firestore — DÉJÀ APPLIQUÉ EN PRODUCTION

| Cible | Volume | Vérification |
|---|---|---|
| `restaurants/{uid}/categories` | **120 documents**, 16 restaurants | requête collection group → 0 |
| `promotions` type `category` | **9 documents** | requête filtrée → 0 |
| `menuCategories` — ajouts | `Sauces` (order 59), `Suppléments` (order 60) | présents |

La suppression était sans risque pour l'app client : la règle
`restaurants/{restaurantId}/categories` était `isAuth() && isOwner(restaurantId)`, donc
seul le restaurant propriétaire y avait accès.

> ⚠️ **Le nettoyage n'est pas définitif tant que la v1.0.0 reste la version publiée.**
> L'ancienne app contient encore `ensureDefaultCategories` et recrée les catégories
> fantômes à chaque ouverture de l'écran Menu — c'est arrivé le 21/07 à 07:06 pour le
> Restaurant Yemenite. Voir l'action 1 ci-dessous.

---

## 4. ⛔ ACTIONS RESTANTES

### Action 1 — Déployer les règles Firestore (préparé, non déployé)

Fichier : `C:\Users\PC\Desktop\FINAL 253 NOMADE\client app\production\firestores regles unique\firestore_rules.rules`

Le bloc `restaurants/{restaurantId}/categories` a été scindé :

```
match /restaurants/{restaurantId}/categories/{categoryId} {
  allow read:  if isAuth() && isOwner(restaurantId);   // condition inchangée
  allow write: if false;                               // sous-collection abandonnée
}
```

**Effet.** Le `ensureDefaultCategories` de la v1.0.0 est enveloppé dans un `runCatching`
sans `onFailure` : l'échec est avalé silencieusement, sans crash ni message. La base
cesse donc de se re-polluer **dès le déploiement des règles**, sans attendre la validation
Play Store.

**Garder `read`** est important : le fermer ferait remonter une erreur dans le snackbar
de la v1.0.0. Avec la lecture conservée, elle reçoit une liste vide et n'affiche qu'un
chip « Toutes » — dégradé mais propre.

Validé par `firebase deploy --only firestore:rules --dry-run` → *compiled successfully*.

> ⚠️ Ce fichier contient les règles des **4 apps**. Un déploiement publie tout le fichier,
> pas seulement ce bloc — vérifier qu'aucune modification d'un autre chantier n'y traîne.

`storage.rules` : **aucune modification nécessaire**, `match /restaurants/{restaurantId}/menu/{fileName}`
couvre déjà l'upload de photos de plats.

### Action 2 — Créer l'index composite Firestore

`orders (restaurantId ASC, status ASC, createdAt ASC)` — **absent**, vérifié le 21/07.

```
https://console.firebase.google.com/v1/r/project/nomade253-478a9/firestore/indexes?create_composite=Ck5wcm9qZWN0cy9ub21hZGUyNTMtNDc4YTkvZGF0YWJhc2VzLyhkZWZhdWx0KS9jb2xsZWN0aW9uR3JvdXBzL29yZGVycy9pbmRleGVzL18QARoQCgxyZXN0YXVyYW50SWQQARoKCgZzdGF0dXMQARoNCgljcmVhdGVkQXQQARoMCghfX25hbWVfXxAB
```

> ⚠️ Passer par ce lien, **pas** par `firebase deploy --only firestore:indexes` : le projet
> n'a pas de `firestore.indexes.json`, un déploiement proposerait de supprimer les index
> existants — dont celui qui fait tourner l'écran Commandes.

Sans cet index l'écran fonctionne quand même (repli local sur lecture de documents), mais
le mode Année devient lent et coûteux quand le volume montera.

### Action 3 — 🔴 Bug des promotions dans `nomade_client` (URGENT)

**Symptôme.** Un plat mis en promotion par le restaurant arrive en commande au **plein
tarif**, côté restaurant *et* côté livreur.

**Diagnostic.** Les trois apps lisent le même document `orders`. Si le restaurant et le
livreur voient tous deux le plein tarif, c'est que le document contient le plein tarif :
la remise n'est jamais appliquée à la création de la commande. **Le client paie plein
pot alors que la promo lui est affichée.**

L'app restaurant est hors de cause : `OrdersScreen.kt:298` est le seul endroit où un
montant est affiché, et il rend `order.total` tel quel, sans jamais consulter le menu.

**Deux pistes dans `nomade_client` :**

1. La création de commande ne lit pas `promotions`. Les règles l'autorisent pourtant
   (`allow read: if true`).
2. Le champ `priceValidated: true` présent sur toutes les commandes suggère une
   validation des prix. Si ce validateur compare le prix commandé à `menuItems.price`
   — le plein tarif — un prix remisé serait **rejeté**. Corriger le panier ne suffirait
   alors pas : il faudra apprendre les promotions au validateur.

**Effet de bord à connaître.** `MenuScreen.kt:222` affiche au restaurateur le prix barré
et le prix remisé. Tant que la chaîne client n'applique pas la promo, cet affichage lui
fait croire que sa remise est active.

### Action 4 — Biais de `restaurants/{uid}.totalRevenue`

Le champ dénormalisé alimenté par une Cloud Function somme `total`, donc **il inclut les
frais de livraison** — même biais que l'app avant correction. Exemple : Restaurant
Yemenite affiche `totalRevenue: 6600` pour une commande dont le CA réel est 6 100 FDJ.

L'app restaurant ne s'en sert pas (elle recalcule depuis les commandes), mais si l'Admin
Web ou l'app client affichent ce champ, ils annoncent un CA surévalué. Chantier backend.

---

## 5. Build

```bash
# APK de production, signé avec la clé NOMADE_R (keystore.properties)
./gradlew clean assembleRelease
# → app/build/outputs/apk/release/

# Bundle pour le Play Store
./gradlew clean bundleRelease
# → app/build/outputs/bundle/release/
```

En PowerShell : `.\gradlew.bat clean assembleRelease`

> `clean` efface le dossier `app/build/`, y compris l'AAB de juin qui s'y trouve encore.
> Le copier ailleurs d'abord si vous voulez le garder comme référence.

---

## 6. État de la vérification

**Confirmé fonctionnel** (test utilisateur du 21/07) : écran Menu, ajout de sauces,
modification de photo, écran Stats.

**Non vérifié en conditions réelles :**

- L'upload d'image n'a été validé que **contre les règles Storage**, jamais sur appareil.
- Le mode Année lance 36 requêtes en parallèle — comportement en latence non observé.
- Aucune commande de test avec `discount ≠ 0` : l'articulation exacte entre `subtotal` et
  `discount` reste une hypothèse. Le CA utilise `subtotal` seul, sans re-soustraire la
  remise.

**Fichiers non commités volontairement :** `.idea/appInsightsSettings.xml`,
`.idea/deploymentTargetSelector.xml`, `.claude/settings.local.json` (config locale d'IDE),
et `hs_err_pid19676.log` — dump de crash JVM de 1,3 Mo du 27 juin, à supprimer ou à
ajouter au `.gitignore`.

---

## 7. Note d'outillage

Le MCP Firebase **aplatit les structures imbriquées à l'affichage** : un tableau d'objets
apparaît comme `"[[object Object]]"` et une map comme `"[Object]"`. Ce n'est **pas** une
corruption de données — `orders.items` a exactement le même rendu alors que toute la
chaîne de production en dépend. Ne pas diagnostiquer de corruption à partir de ce rendu ;
vérifier via un SDK réel avant toute conclusion.

---

## 8. Session du 23 juillet 2026 — sauces & suppléments visibles + toggle rupture

Suite au retour : « les sauces et suppléments du restaurant sont invisibles dans sa page
menu ». Diagnostic et correction complète du côté app restaurant.

### 8.1 Pourquoi c'était invisible

Les sauces et suppléments **ne sont pas des documents séparés** : ils vivent dans le champ
**`optionGroups`** (`Array<Map>`) embarqué **dans chaque plat** `menuItems`. Il n'existe
aucune collection dédiée. Or :

- le modèle Kotlin `MenuItem` **ignorait totalement** ce champ ;
- l'ancien système « Sauces/Suppléments » (`MenuViewModel.EDITABLE`, `addExtra`,
  `deleteExtra`) créait des documents `menuItems` avec `category = "Sauces"` que **rien
  en base ni côté client ne consomme** — 0 document de ce type existe. Système mort.

Mesuré sur **Chez Davido** (`E6IrEGqgMGQzWOEgwiNa7dINh4y1`, 81 plats) via l'API REST
Firestore : le groupe `Suppléments` est présent sur **15 plats**, chacun portant sa propre
copie des 7 mêmes sauces à 700 FDJ (roquefort, ail, crème, poivre, citronnée, diable,
champignons). C'est cette duplication qui impose un **batch write** pour toute mise à jour.

### 8.2 Lecture + affichage (livré, testé à l'écran)

| Fichier | Changement |
|---|---|
| `data/model/OptionGroup.kt` **(nouveau)** | `OptionGroup` + `OptionChoice(available=true)`, parsing tolérant (absent/null/mal typé → liste vide), `toFirestoreList()`, `String.normalizeOptionName()` |
| `data/model/MenuItem.kt` | champ `optionGroups: List<OptionGroup>` |
| `data/remote/FirestoreMapper.kt` | `toMenuItem()` lit `optionGroups` ; **jamais** réécrit par `toFirestoreMap()` (préserve les options existantes) |
| `data/local/entity/MenuItemEntity.kt` | colonne `optionGroups` (JSON sérialisé) |
| `data/local/RestaurantDatabase.kt` | **DB version 3** + `MIGRATION_2_3` explicite (pas de destruction — le cache sert de mode dégradé hors ligne) |
| `core/di/AppModule.kt` | `.addMigrations(MIGRATION_2_3)` |
| `ui/menu/MenuViewModel.kt` | sections Sauces/Suppléments **agrégées** depuis `optionGroups` ; système mort `EDITABLE`/`addExtra`/`deleteExtra` **supprimé** |
| `ui/menu/MenuScreen.kt` | `AddExtraDialog` supprimé, nouvelle `ExtraOptionCard` |

**Rangement par nom de GROUPE, jamais de choix** (`MenuViewModel.sectionOf`, aligné sur
`isSauceGroup()` du client) : « Sauce roquefort » appartient au groupe `Suppléments` →
s'affiche dans **Suppléments**, pas dans Sauces, malgré le mot « sauce » dans son nom. Un
chip Sauces/Suppléments n'apparaît que si le menu contient vraiment ce type de groupe —
d'où l'absence de chip Sauces chez Davido (il n'a pas de groupe nommé « sauce »). Les
15 copies de « Sauce roquefort » sont dédupliquées en **une seule ligne** portant le
nombre de plats.

### 8.3 Toggle de rupture (livré, non fonctionnel tant que les règles ne sont pas déployées)

`MenuRepository.setChoiceAvailability(restaurantId, choiceName, available)` :

- lit les plats du restaurant, ne garde que ceux qui portent réellement ce choix
  (comparaison normalisée casse + accents) ;
- reconstruit l'`optionGroups` complet de chacun avec le flag modifié ;
- écrit dans un **`WriteBatch`** découpé en tranches de 400 (limite Firestore 500).

**Schéma additif strict** : `available` n'est écrit **que lorsqu'il vaut `false`**
(absent = disponible) — aucun document gonflé, compatibilité préservée avec les menus
importés. L'écran se rafraîchit seul via le snapshot listener. UI : un `Switch` par choix,
spinner pendant le batch, libellé `EN RUPTURE` / `ÉTAT MIXTE`.

La rupture d'un **plat entier** passait déjà par `isAvailable` (Switch existant) — aucune
règle à changer pour ça.

### 8.4 Règles Firestore modifiées (préparé, NON déployé)

Fichier : `C:\Users\PC\Desktop\FINAL 253 NOMADE\client app\production\firestores regles unique\firestore_rules.rules`

Seule modif : ajout de `'optionGroups'` au `onlyFields` de la branche **restaurant** de
`menuItems/update` (rien d'autre touché). Sans déploiement, le toggle des sauces renvoie
`PERMISSION_DENIED`.

```
firebase deploy --only firestore:rules   # publie les 4 apps — vérifier le fichier avant
```

### 8.5 ⛔ RESTE À FAIRE — l'app client ne voit pas la rupture

**Symptôme constaté par l'utilisateur** : une fois un supplément mis en rupture côté resto,
l'app client continue de l'afficher comme disponible.

**Deux causes possibles à départager :**

1. **Règles non déployées** → l'écriture `available: false` part en `PERMISSION_DENIED`,
   le champ n'est jamais écrit en base → le client voit « disponible » parce que ça
   **l'est** encore réellement. À vérifier en premier.
2. **Le client ne lit pas le champ `available`.** `OptionChoice.fromMap`
   (`Velox_Client-main` / `nomade_client`, `lib/models/option_group.dart`) ne lisait que
   `name` et `price`. Tant que le client n'est pas patché pour filtrer les choix
   `available == false`, le toggle reste **décoratif** côté client. Chantier séparé, dans
   le repo client — non traité dans cette session.

> Analyse du code client interrompue par l'utilisateur ; le patch client reste à faire.

---

## 9. Session des 30-31 juillet 2026 — son de notification, option Légumes, relance vers les livreurs

Aucun commit n'a été fait pendant cette session : tout ce qui suit est en
working tree, au-dessus de `859a23f`. `versionCode` passé à **3**,
`versionName` à **1.0.2**.

### 9.1 Son de notification personnalisé pour les nouvelles commandes

Reprend le pattern déjà validé côté app **livreur** (channel immuable après
création → nouvel ID obligatoire, meta-data manifest indispensable pour le cas
« app tuée », `channelId` du payload FCM qui prime sur le manifest).

| Fichier | Changement |
|---|---|
| `RestaurantApplication.kt` | Nouveau channel `orders_channel_v2` (id lu depuis `strings.xml`, source de vérité unique) ; `orders_channel` (ancien, sans son) supprimé via `deleteNotificationChannel` ; son attaché via `AudioAttributes` |
| `res/raw/new_order.mp3` **(nouveau)** | Copié depuis `Velox_Livreur` (aucun fichier son n'existait dans ce repo avant) |
| `strings.xml` | `default_notification_channel_id` = `orders_channel_v2` |
| `AndroidManifest.xml` | meta-data `com.google.firebase.messaging.default_notification_channel_id` ajoutée (absente jusqu'ici) |
| `RestaurantFcmService.kt` | Référence la ressource string plutôt qu'une constante Kotlin |

Cloud Function (`nomade_client/functions/index.js`), **déployée** :
`onOrderCreated` et `sendRestaurantNotification` — `channelId` passe de
`"orders"` (qui ne correspondait à **aucun** channel réel de l'app resto) à
`"orders_channel_v2"`, `sound` de `"default"` à `"new_order"`.

⚠️ **Non revérifié sur device après les changements ultérieurs de la session**
(seule la compilation a été validée) — à tester avant publication : app au
premier plan et app tuée, cf. protocole déjà décrit pour l'app livreur.

### 9.2 Nouvelle section d'options « Légumes »

Le mécanisme de rupture (`setChoiceAvailability`, `ExtraOptionCard`, le batch
Firestore) était déjà 100 % générique depuis la session du 23 juillet — il ne
raisonne que sur des noms de choix, jamais de catégorie. Seul
`MenuViewModel.sectionOf()` limitait l'affichage à Sauces/Suppléments.

- Ajout de `LEGUMES = "Légumes"` à `OPTION_SECTIONS`, et d'un cas
  `n.contains("legume") -> LEGUMES` dans `sectionOf()`.
- Aucun autre fichier touché (UI, repository, règles Firestore déjà
  compatibles).

**Vérifié en base réelle** (lecture directe via l'API REST Firestore, en
contournant l'aplatissement du MCP — cf. [[menu_categories_source]]) : sur
1114 plats, un groupe nommé exactement « Légumes » existait déjà chez
plusieurs restaurants (Salade/Tomate/Oignon), et un groupe « Sauces » à part
entière aussi (Ketchup/Mayonnaise/Algérienne…) — donc le cas Sauces
fonctionnait déjà, seul Légumes manquait.

### 9.3 Migration de données — DÉJÀ APPLIQUÉE EN PRODUCTION

À la demande explicite de l'utilisateur : unification des groupes nommés
« Ingrédients » composés exactement de Salade/Tomate/Oignon vers « Légumes »,
pour qu'ils héritent du toggle de rupture.

| Restaurant | Plats renommés |
|---|---|
| KPB (`BkCioNVTn2dUU34phFBNfoaqDV73`) | 8 |
| STREAT (`ametD7ozAyNtCzG4gfPXgqP2Eoh2`) | 11 |

**Exclu volontairement** : un 3ᵉ restaurant (`7FiEFKJNh3SVlVCCMMkGNlGirr63`,
4 plats « Vital'Quinoa ») dont le groupe « Ingrédients » a une composition
différente (Quinoa/Brocoli/Carottes/Feta/Olives/Concombre — un bol composé,
pas une liste de légumes simples).

Seul le champ `name` du groupe a été modifié (`Ingrédients` → `Légumes`) ;
tous les autres groupes de chaque plat (Sauces, Suppléments, Menu Frite et
Soda…) ont été relus et réécrits à l'identique. Vérifié après coup par
re-requête : 0 document restant avec ce couple nom/composition.

### 9.4 Bouton « Relancer vers les livreurs » (Tâche 2 du prompt initial)

Cloud Function `resendOrderToDrivers` (`nomade_client/functions/index.js`),
**créée puis corrigée et déployée** :

- Garde-fous dans l'ordre : `auth` présent → `orderId` présent → commande
  existante → **`request.auth.uid === order.restaurantId`** (ownership) →
  `status === "ready"` et `deliveryDriverId` vide → cooldown 60 s sur
  `orders.lastResendAt`.
- Notifie les livreurs `isOnline && isAvailable`, écrit `lastResendAt` +
  incrémente `resendCount` sur la commande dans tous les cas.
- Ne touche ni au statut, ni aux stats restaurant, ni à la notif client
  (contrairement à `sendOrderReadyNotifications`, volontairement non réutilisée).

🔴 **Bug trouvé et corrigé avant publication** : la première version sautait
purement et simplement l'envoi FCM dès qu'une offre `livreurNotifications`
existait déjà pour ce livreur — exactement le cas dans le scénario que le
bouton est censé résoudre (personne n'a accepté). Trois tests réels sont
passés avec 0 notification envoyée. Corrigé : le push repart **toujours**,
seule la création d'un nouveau document Firestore est évitée si une offre non
rejetée existe déjà (anti-doublon, pas anti-relance). Redéployé et confirmé
par les logs serveur.

Côté app resto (`Velox_Restaurant`) :

| Fichier | Changement |
|---|---|
| `data/model/Order.kt` | `lastResendAt: Long?`, `resendCount: Int` (additif) |
| `data/remote/FirestoreMapper.kt` | Lecture des deux champs |
| `data/model/ResendResult.kt` **(nouveau)** | `{ success, driversNotified, message }` |
| `domain/repository/OrderRepository.kt` + impl | `resendToDrivers(orderId)` |
| `ui/orders/OrdersViewModel.kt` | `resendingOrderId`, `resendToDrivers()` |
| `ui/orders/OrdersScreen.kt` | Bouton visible uniquement si `ready` sans `deliveryDriverId`, décompte de cooldown 60 s (`produceState`), compteur de relances, sinon bandeau « livreur en route » inchangé |

### 9.5 Diagnostic FCM — fausses pistes écartées avant de trouver la vraie cause

Pendant les tests, des `PERMISSION_DENIED` sur `livreurNotifications` et un
token FCM nettoyé ont été (à tort) attribués à `resendOrderToDrivers` par un
autre outil d'IA consulté en parallèle. Écarté méthodiquement :

- Une Cloud Function tourne avec l'Admin SDK, qui **contourne toujours** les
  règles Firestore — elle ne peut structurellement pas produire de
  `PERMISSION_DENIED`. Confirmé par les logs d'exécution réels (3 appels de
  test, 0 erreur serveur).
- Règles déployées en prod comparées octet pour octet au fichier local (API
  Firebase Rules) : identiques, aucun écart de déploiement.
- `messaging/registration-token-not-registered` n'est pas un bug : c'est le
  signal FCM officiel qu'un token est mort. Le nettoyer est la pratique
  recommandée par Google, déjà en place dans `sendOrderReadyNotifications`
  avant même cette session.
- Preuve empirique : push de test envoyé directement via l'API FCM (hors
  toute Cloud Function) à un livreur en ligne (ABDOURAHMAN, token de 3 jours)
  → HTTP 200, accepté. Le pipeline FCM est sain.
- Le token du livreur de test (Kassim) était rejeté `UNREGISTERED`
  **immédiatement après régénération** — impossible pour un token périmé,
  révélateur d'un problème d'app/device (build obsolète ou Google Play
  Services cassé), pas de code. Résolu par désinstallation/réinstallation
  complète de l'app livreur depuis le Play Store.

### 9.6 ⛔ Cause racine trouvée — hors périmètre de ce repo (app livreur)

Une fois le token de Kassim réparé, nouveau symptôme : le push arrive bien,
mais **la commande n'apparaît jamais dans l'app livreur**, uniquement pour les
relances (la toute première notification d'une commande s'affiche
normalement).

**Diagnostiqué par requête directe sur Firestore** (contournement du MCP,
lecture REST via le token OAuth du CLI `firebase`) : Kassim a **70 documents**
dans `livreurNotifications`, dont 30+ avec `accepted == false` remontant
jusqu'au 6 juillet, jamais nettoyés. La requête de l'app livreur
(`NotificationRepositoryImpl.kt`) fait
`.whereEqualTo("accepted", false).limit(20)` **sans `.orderBy()`** : au-delà
de 20 documents accumulés, Firestore peut renvoyer un lot qui ne contient pas
les plus récents — la commande du jour existe en base mais n'est jamais lue
par le client. Aggravant : `cleanupExpiredOffers` (planifiée, déployée) ne
nettoie que `taxiRides`, rien n'existe pour `livreurNotifications`.

Hors périmètre de `Velox_Restaurant` — prompt complet écrit pour une session
Claude Code dédiée : `Velox_Livreur/PROMPT_CLEANUP_LIVREUR_NOTIFICATIONS.md`
(purge urgente, ajout du tri manquant + index composite, nettoyage planifié
récurrent). **Le test de bout en bout de la relance reste bloqué tant que ce
correctif n'est pas fait.**

### 9.7 Masquage des commandes bloquées après 24h

Demande utilisateur suite au constat que des commandes (ex. `ready` sans
livreur depuis des jours) polluent l'écran « Commandes ». Décision validée
avec l'utilisateur : **affichage uniquement**, aucune écriture Firestore
(ni annulation automatique, ni changement de statut — zéro risque métier,
réversible instantanément).

`OrdersViewModel.kt` : `activeOrders` filtre désormais les commandes de plus
de 24h (`createdAt`), tick de ré-évaluation toutes les 15 min (une commande
bloquée peut ne générer aucune écriture Firestore pendant des jours, donc rien
ne redéclencherait le filtre sans ce tick). Les commandes « Nouvelles »
(jamais acceptées) ne sont **jamais** masquées — un client attend encore.

### 9.8 État de la vérification

**Confirmé fonctionnel par l'utilisateur** : sections Sauces/Suppléments/
Légumes avec toggle de rupture.

**Confirmé fonctionnel côté serveur** : `resendOrderToDrivers` (logs
d'exécution propres, plus aucun `PERMISSION_DENIED` imputable).

**Non vérifié de bout en bout** : réception réelle d'une relance affichée
côté app livreur — bloqué par la cause racine de la section 9.6, à retester
une fois le repo `Velox_Livreur` corrigé.

**Non revérifié** : son de notification personnalisé sur device (compilé
uniquement, cf. 9.1).

### 9.9 Build

```bash
./gradlew.bat clean bundleRelease
```

→ `app/build/outputs/bundle/release/app-release.aab` (9,9 Mo, signé,
versionCode 3 / versionName 1.0.2).

---

## 10. Session des 4-5 octobre 2026 — intégration caisse Loyverse (connexion + envoi des ventes)

Objectif : les restaurants qui utilisent la caisse **Loyverse** y retrouvent
leurs ventes Velox, sans ressaisie. Le travail touche 4 projets : cette app,
les Cloud Functions (`nomade_client/functions`), l'app client Kotlin
(`Velox_client`) et l'admin Flutter (`ReelADMIN/ADMIN3/NOMADE_ADMIN_COMPLETE`).
L'app iOS est sur un autre PC, hors de cette session.

**Limite de l'API Loyverse (à ne jamais promettre) :** elle ne crée que des
tickets **déjà clôturés**. Rien n'arrive « à préparer » sur la caisse ni en
cuisine, et rien ne s'imprime automatiquement : la vente apparaît dans
l'historique des reçus.

### 10.1 Connexion d'un restaurant à son compte Loyverse (OAuth)

- **Serveur** — `functions/loyverse.js` : `loyverseStartOAuth`,
  `loyverseOAuthCallback`, `loyverseStatus`, `loyverseDisconnect`.
  Jetons stockés dans `loyverseIntegrations/{restaurantId}` (aucune règle
  Firestore → illisible par les apps, Admin SDK uniquement). `state` OAuth à
  usage unique, valable 10 min ; jeton renouvelé automatiquement, statut
  `reconnect_required` si le renouvellement est refusé.
- **Permissions demandées** : `RECEIPTS_READ/WRITE`, `ITEMS_READ`,
  `ITEMS_WRITE` (création de l'article générique), `STORES_READ`,
  `PAYMENT_TYPES_READ`, `INVENTORY_READ`, `MERCHANT_READ`.
- **Secret** `LOYVERSE_CLIENT_SECRET` dans Secret Manager (version 1).
  ⚠️ Il a été collé dans la conversation : à régénérer dans le portail
  Loyverse puis `firebase functions:secrets:set LOYVERSE_CLIENT_SECRET` et
  redéploiement des fonctions `loyverse*`.
- **App restaurant** : section « CAISSE LOYVERSE » dans le Profil (statut,
  connexion via le navigateur, déconnexion avec confirmation, textes
  FR/EN/AR). Fichiers : `LoyverseRepository(Impl)`, `LoyverseConnection`,
  `ProfileViewModel`, `ProfileScreen`.
- **« Cannot approve uninitialized request »** sur la page Loyverse = second
  clic sur « Allow » : la connexion avait réussi au premier.

### 10.2 Affichage des options par groupe dans les commandes

`OrderItem.optionTags()` + `FirestoreMapper` : les options s'affichent par
groupe du menu (« VIANDE : Tenders », « BOISSON : Fanta… ») quand l'app
client écrit `group`. Sans `group` (anciennes commandes), l'ancien affichage
SAUCE / SUPPLÉMENT est conservé. Couvert par `OrderItemTest`.

### 10.3 Envoi des ventes et remboursements

- **Quand** : la vente est créée quand le restaurant **accepte** la commande
  (choix utilisateur). Si la commande est annulée ensuite, un ticket de
  **remboursement** lié est créé. Commande annulée avant acceptation : rien.
  Constat sur 86 commandes annulées : 23 l'avaient été après acceptation.
- **Fonctions** : `loyverseOnOrderStatus` (déclencheur, `europe-west1`) et
  `loyverseRetrySync` (toutes les 15 min, `us-central1`). Logique du ticket
  isolée dans `functions/loyverseReceipt.js`, 11 tests :
  `node --test test/loyverseReceipt.test.js` (passer le fichier, pas le
  dossier, sous Node 24).
- **Montant** = `subtotal`. Livraison exclue ; codes promo et points payés
  par Velox (décision utilisateur) → le ticket reste au prix plein.
- **Plats** : plat relié dans `loyverseMappings/{restaurantId}.items` →
  son article Loyverse ; sinon article générique **« Vente Velox »** à prix
  libre, créé automatiquement une fois par restaurant, nom du plat en note.
  Options écrites en note de ligne (pas de `line_modifiers` : format d'envoi
  non documenté et leur prix s'ajouterait au prix de la ligne).
- **Paiement** : correspondance `loyverseMappings.paymentTypes`, sinon type
  Loyverse du même nom (« Waafi »), sinon `CASH` pour `cash` et `OTHER`
  pour le reste. **Magasin** : choisi automatiquement s'il n'y en a qu'un.
- **Fiabilité** : suivi dans `loyverseSales/{orderId}` (jamais dans
  `orders`, pour ne pas relancer les autres déclencheurs), réservation par
  transaction, recherche du ticket existant (`GET /receipts?order=`) avant
  chaque nouvel essai → aucun doublon. Erreurs temporaires rejouées après 1,
  5, 15, 60, 180, 360, 720 min, puis statut `error`. Devise ≠ DJF refusée.
- **Contraintes API constatées en réel** : `order` ≤ **20 caractères**
  (on envoie l'id Firestore seul) ; `POST /receipts` vérifie `payments` avant
  les autres champs.

### 10.4 Apps client et admin (prompts rédigés, appliqués par d'autres agents)

- **Contrat** : chaque choix d'option porte un `id` stable `ch_xxxxxxxx`
  généré par l'admin ; l'app client recopie `choiceId` + `group` dans
  `orders.items[].extras[]/sauces[]`. Prompts :
  `Velox_client/PROMPT_LOYVERSE_{ANDROID,IOS}.md`,
  `NOMADE_ADMIN_COMPLETE/PROMPT_LOYVERSE_ADMIN.md`.
- **Velox_client (Kotlin)** : écrit déjà `choiceId` et `group`.
- **Admin** : fait sur `feat/option-choice-ids` (38 tests OK) — `id` et
  `available` conservés à la sauvegarde (corrige l'effacement des ruptures
  posées par le restaurant), bouton de migration avec comptage + confirmation.
  ⏳ À commiter, fusionner, déployer, puis lancer la migration une fois.

### 10.5 Tests réels (compte Loyverse de test, restaurant ALI PASTA)

- ✅ Connexion OAuth : `loyverseIntegrations/vlvQZc6i4AUdXjYpyULGqsxvDXB3`.
- ❌ 1re vente refusée (`order` de 26 caractères) → corrigé (`77b9b06`).
- ✅ 2e vente : **reçu 0001**, 1 000 FDJ (= `subtotal`, livraison exclue),
  paiement Cash, article « Vente Velox », note avec le plat et ses options.
- ✅ Annulation d'une vente jamais créée → suivi clos en `skipped`.
- ⏳ **Remboursement d'une vente réelle : non testé.** Le bouton
  « Annuler » de l'onglet EN COURS n'apparaissait pas sur le téléphone de
  test, alors que le code l'affiche toujours (`OrdersScreen.kt:582`) :
  à revérifier avec la version 1.0.9.
- ⏳ **Restaurant réel** : Loyverse exige l'abonnement **« Integrations »**
  (9 $/mois/magasin, essai gratuit 14 jours) pour connecter une app externe.
  Décision à prendre : qui paie (restaurant ou Velox).

### 10.6 Commits et déploiements

- `Velox_Restaurant` (branche `fix/menu-categories-stats-comptables`) :
  `bdf8a74` (Loyverse Profil + options par groupe), version **10 / 1.0.9**.
- `nomade_client` (branche `feature/design-review`) : `984d080` (OAuth),
  `68b25ec` (`ITEMS_WRITE`), `74b6404` (envoi des ventes), `77b9b06`
  (référence ≤ 20 caractères). Les 6 fonctions `loyverse*` sont déployées.
  ⚠️ `functions/index.js` contient d'autres changements non commités, sans
  rapport avec Loyverse (heartbeat chauffeurs, statuts de course,
  `onOrderCreated`) : à vérifier par leur auteur.
- Déployer les fonctions avec `FUNCTIONS_DISCOVERY_TIMEOUT=90`, sinon le
  chargement d'`index.js` dépasse le délai de 10 s.

### 10.7 Reste à faire

1. Tester le remboursement (annuler une commande acceptée chez ALI PASTA).
2. Décider qui paie l'abonnement Loyverse « Integrations », puis pilote chez
   un vrai restaurant.
3. Admin : écran de correspondance plats ↔ articles Loyverse et choix du
   magasin / des types de paiement (écrit `loyverseMappings`, demande des
   règles Firestore pour cette collection).
4. Régénérer le secret Loyverse (cf. 10.1).
5. Fusionner `fix/menu-categories-stats-comptables` dans `main`.

### 10.8 Build

```bash
./gradlew.bat bundleRelease
```

→ `app/build/outputs/bundle/release/app-release.aab` (8,8 Mo, signé,
versionCode 10 / versionName 1.0.9), copié sur le Bureau sous
`Velox_Restaurant_1.0.9_v10.aab`. APK de test du même build :
`Velox_Restaurant_1.0.9_loyverse.apk`.
