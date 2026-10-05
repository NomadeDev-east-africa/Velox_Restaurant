# Prompt — App Restaurant (Velox Corp)

> À coller dans une session Claude Code ouverte sur le projet de l'app restaurant.

---

## Contexte de l'écosystème

Velox Corp (Djibouti) est composé de plusieurs apps Android + un dépôt Cloud Functions partagé :

- **App client** — commande de repas et de courses taxi
- **App restaurant** — celle sur laquelle tu travailles
- **App livreur** (`Velox_Livreur`, package `dj.nomade.velox_livreur`) — livraison des commandes food
- **Cloud Functions** — `C:\Users\PC\StudioProjects\nomade_client\functions\index.js` (Firebase Functions v2, Node.js)

Firestore contient notamment les collections `orders`, `restaurants`, `livreurs`, `livreurNotifications`, `users`.

Tu as **deux tâches indépendantes**. Traite-les dans l'ordre.

---

# TÂCHE 1 — Son de notification personnalisé pour les nouvelles commandes

## Objectif

Quand une nouvelle commande arrive, l'app restaurant doit jouer un **son personnalisé** (fichier mp3 fourni) au lieu du son système par défaut.

## Ce qui a déjà été fait sur l'app livreur (à répliquer)

Le même travail vient d'être fait sur l'app livreur. Voici les pièges rencontrés — ils ont coûté trois itérations, ne les refais pas.

### Piège 1 — Le son d'un channel est immuable après création

`NotificationChannel.setSound()` n'a **aucun effet** si un channel avec cet ID existe déjà sur l'appareil. Modifier le code ne change rien pour les utilisateurs qui ont déjà l'app.

→ Il faut **créer un nouvel ID de channel** (ex. `orders` → `orders_v2`) et supprimer l'ancien via `manager.deleteNotificationChannel("orders")`.

### Piège 2 — Le channel doit exister AVANT que Firebase affiche la notification

Quand un message FCM avec un bloc `notification` arrive alors que l'app est **tuée**, c'est le SDK Firebase qui affiche la notification lui-même — `onMessageReceived()` n'est **jamais appelé**. Si le channel n'existe pas encore, Firebase retombe sur son channel de secours, avec le son système.

→ La création des channels doit se faire dans **`Application.onCreate()`**, pas au moment où une classe est injectée par Hilt/Koin dans un service. Le process étant démarré par FCM, `onCreate()` s'exécute avant l'affichage.

### Piège 3 — Le `channelId` du payload FCM prime sur le manifest

C'était la cause racine côté livreur. Le payload envoyé par la Cloud Function contenait `channelId: "orders"`, un channel qui n'existait pas dans l'app cible. **Un `channelId` explicite dans le payload court-circuite le meta-data `default_notification_channel_id` du manifest.** Aucune correction côté Android ne pouvait compenser.

→ Le `channelId` envoyé par la Cloud Function **doit** correspondre exactement à un channel réellement créé par l'app destinataire.

## Étapes à réaliser

### 1. Placer le fichier son

```
app/src/main/res/raw/new_order.mp3
```

Contraintes : nom en minuscules, chiffres et underscores uniquement (pas de majuscule, tiret, espace ni accent — sinon échec de compilation). Durée **≤ 5 secondes** : au-delà, Android tronque le son de notification.

### 2. Créer le channel avec le son

```kotlin
private val newOrderSound: Uri =
    Uri.parse("android.resource://${context.packageName}/${R.raw.new_order}")

fun ensureChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    manager.deleteNotificationChannel("orders")   // ancien channel sans son
    manager.createNotificationChannel(
        NotificationChannel(
            "orders_v2",
            "Nouvelles commandes",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                newOrderSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
    )
}
```

Attention à l'ordre de déclaration en Kotlin : si `ensureChannels()` est appelée depuis un bloc `init`, la propriété `newOrderSound` doit être **déclarée avant** ce bloc, sinon elle vaut `null` à l'exécution.

Ajoute aussi `.setSound(newOrderSound)` sur le `NotificationCompat.Builder` : ignoré sur API 26+, mais c'est le seul chemin sur API 24-25 si le `minSdk` le permet.

### 3. Appeler `ensureChannels()` dans `Application.onCreate()`

### 4. Déclarer le channel par défaut dans le manifest

```xml
<meta-data
    android:name="com.google.firebase.messaging.default_notification_channel_id"
    android:value="@string/default_notification_channel_id" />
```

avec dans `strings.xml` :

```xml
<string name="default_notification_channel_id" translatable="false">orders_v2</string>
```

Fais pointer le code Kotlin sur cette même ressource pour garder une source de vérité unique.

### 5. Corriger la Cloud Function — INDISPENSABLE

Dans `C:\Users\PC\StudioProjects\nomade_client\functions\index.js`, **deux** envois ciblent le restaurant :

- `onOrderCreated` (~ligne 1204) : `android: { priority: "high", notification: { sound: "default", channelId: "orders" } }`
- `sendRestaurantNotification` (~ligne 1245) : même bloc

Remplace dans **les deux** :

```js
android: {
  priority: "high",
  notification: { sound: "new_order", channelId: "orders_v2", priority: "high" },
},
```

`sound` = nom du fichier dans `res/raw` **sans extension**. Ignoré sur Android 8+ (le channel fait foi), utilisé sur Android 7.

Ne touche pas aux `channelId: "velox_orders_v2"` (app livreur), `"rides"`, `"earnings"`, `"ride_requests"` — ils visent d'autres apps.

### 6. Vérifier sur appareil

**Désinstalle avant de réinstaller** (réglages de channel figés à la création) :

```
adb uninstall <package.restaurant>
adb install app/build/outputs/apk/debug/app-debug.apk
adb logcat -s <TonTag>
```

Ajoute un log temporaire affichant `manager.getNotificationChannel("orders_v2")?.sound` : tu dois voir `android.resource://...`, pas `content://settings/system/notification_sound`.

Teste les **deux** états : app au premier plan (passe par `onMessageReceived`) **et** app tuée (passe par le SDK Firebase). Ce sont deux chemins de code différents, et c'est le second qui était cassé côté livreur. Ne teste pas depuis la console Firebase : elle n'envoie pas le même payload que la production.

---

# TÂCHE 2 — Bouton « Relancer la notification vers les livreurs »

## Le problème à résoudre

Quand le restaurant passe une commande en « prête », la Cloud Function `sendOrderReadyNotifications` notifie **uniquement les livreurs qui sont en ligne et disponibles à cet instant précis**.

Conséquences :

- Un livreur hors ligne à ce moment-là ne reçoit **jamais** rien — aucun document `livreurNotifications` n'est créé pour lui. Quand il se connecte 10 minutes plus tard, la commande est invisible pour lui.
- Un livreur en ligne qui n'a pas vu la notification à temps la perd : l'app livreur supprime côté client les notifications de plus de **5 heures**.
- Résultat : la commande reste bloquée en `ready`, personne ne peut la prendre, et le restaurant n'a **aucun moyen de la remettre en circulation**.

Il faut donc un bouton **« Relancer vers les livreurs »** côté restaurant.

## Modèle de données existant (à respecter scrupuleusement)

L'app livreur écoute la collection `livreurNotifications` avec cette requête :

```kotlin
collection("livreurNotifications")
    .whereEqualTo("userId", livreurId)
    .whereEqualTo("accepted", false)
    .limit(NOTIFICATION_LIMIT)
// puis, côté client : filtre !rejected, tri par createdAt desc, distinctBy orderId
```

Un document = **une offre destinée à UN livreur précis** (`userId` = ID du livreur). Forme exacte produite aujourd'hui par la Cloud Function :

```js
{
  userId: livreurId,
  type: "food_delivery",
  orderId,
  title: "🍽️ Nouvelle livraison !",
  body: `De ${order.restaurantName} vers ${order.deliveryAddress}`,
  data: {
    order_type: "food",
    restaurant_name, pickup_address, delivery_address,
    customer_name, customer_phone,
    total, estimated_distance: 5, estimated_time: 15,
    delivery_location,   // optionnel
  },
  createdAt: FieldValue.serverTimestamp(),
  read: false, accepted: false, rejected: false,
}
```

Et le message FCM correspondant (déjà corrigé, à reproduire à l'identique) :

```js
{
  token: livreur.fcmToken,
  notification: { title: "🛵 Nouvelle livraison !", body: "..." },
  data: { type: "order_ready_for_delivery", orderId, restaurantName, restaurantId,
          customerName, customerPhone, deliveryAddress, total },
  android: {
    priority: "high",
    notification: { sound: "new_order", channelId: "velox_orders_v2", priority: "high" },
  },
}
```

⚠️ `channelId: "velox_orders_v2"` et `sound: "new_order"` visent l'**app livreur**. Ne les remplace pas par les valeurs de l'app restaurant.

## ⚠️ Piège critique — ne PAS réutiliser `sendOrderReadyNotifications`

La tentation évidente est de rappeler `sendOrderReadyNotifications` depuis le bouton. **Ne le fais pas.** Cette fonction fait bien plus que notifier les livreurs — à chaque appel elle :

1. Repasse la commande en `status: "ready"` avec un nouveau `readyAt`
2. Renvoie au client la notification « Votre commande est prête »
3. **Incrémente `totalOrders` et `totalRevenue` du restaurant** (~ligne 1388)

Le point 3 est le plus grave : chaque clic sur « Relancer » gonflerait le chiffre d'affaires du restaurant. Les statistiques deviendraient fausses de façon invisible et irrécupérable.

→ Il faut une **nouvelle Cloud Function dédiée**.

## Cloud Function à créer : `resendOrderToDrivers`

Dans `C:\Users\PC\StudioProjects\nomade_client\functions\index.js`, ajoute un `onCall` v2 (même style que les fonctions existantes) :

**Signature** : `{ orderId }` → `{ success, driversNotified, message }`

**Garde-fous obligatoires, dans cet ordre :**

1. `request.auth` présent, sinon `HttpsError("unauthenticated")`
2. `orderId` présent, sinon `HttpsError("invalid-argument")`
3. La commande existe, sinon `HttpsError("not-found")`
4. **Le caller est bien le restaurant propriétaire de la commande.** Vérifie la règle d'authentification réellement utilisée par l'app restaurant dans ce projet (compte Firebase Auth lié au doc `restaurants/{id}` ?) et implémente le contrôle correspondant. Sans ça, n'importe quel utilisateur authentifié peut spammer tous les livreurs.
5. La commande est encore prenable : `status === "ready"` **et** aucun livreur assigné (`deliveryDriverId` vide/absent). Sinon retourne un message clair du type « commande déjà prise en charge » — pas une erreur.
6. **Cooldown anti-spam** : stocke `lastResendAt` sur le document `orders`. Refuse tout renvoi à moins de 60 secondes du précédent, avec un message explicite.

**Traitement :**

- Requête `livreurs` avec `isOnline == true && isAvailable == true`
- Pour chaque livreur ayant un `fcmToken` :
  - Vérifie qu'il n'existe pas **déjà** un doc `livreurNotifications` pour ce couple (`userId`, `orderId`) avec `rejected == false` → sinon on créerait des doublons à chaque relance (la requête de l'app livreur ne déduplique que sur `orderId`, mais les docs orphelins s'accumuleraient en base). Cette requête composite nécessitera probablement un **index Firestore** : `livreurNotifications` sur (`userId` ASC, `orderId` ASC). Crée-le si le déploiement le réclame.
  - Décision de conception à prendre explicitement : un livreur ayant **refusé** cette commande (`rejected == true`) doit-il être re-sollicité ? Par défaut **non** — mais expose ce choix dans un commentaire pour qu'il soit modifiable.
  - Sinon : crée le doc `livreurNotifications` (forme exacte ci-dessus) **et** envoie le FCM
- Écris `lastResendAt` + incrémente un compteur `resendCount` sur la commande (utile pour le support)
- **Ne touche ni aux stats restaurant, ni au statut, ni à la notification client**
- Utilise `Promise.allSettled` (comme le code existant) pour qu'un token invalide ne fasse pas échouer tout le lot
- Gère le token périmé : `messaging/registration-token-not-registered` → mets `fcmToken: null` sur le doc livreur, comme le fait déjà le code existant
- Si aucun livreur en ligne : retourne `{ success: false, driversNotified: 0, message: "Aucun livreur en ligne actuellement" }` — ce n'est pas une erreur, c'est une information à afficher

## UI côté app restaurant

- Bouton **« Relancer vers les livreurs »**, visible **uniquement** si `status === "ready"` **et** aucun livreur assigné
- État de chargement pendant l'appel, bouton désactivé
- Retour explicite à l'utilisateur :
  - `driversNotified > 0` → « N livreur(s) notifié(s) »
  - `driversNotified === 0` → « Aucun livreur en ligne actuellement, réessayez dans quelques minutes »
  - cooldown actif → « Patientez X secondes avant de relancer »
- Désactive visuellement le bouton pendant les 60 s de cooldown (calcul local à partir de `lastResendAt`)
- Idéalement, affiche depuis combien de temps la commande attend un livreur — c'est ce qui déclenche le besoin de relancer

## Vérifications finales

1. `node --check index.js` sur la Cloud Function
2. Déploiement ciblé : `firebase deploy --only functions:resendOrderToDrivers`
3. Test bout en bout : commande en `ready` sans livreur → livreur hors ligne → il se connecte → clic sur « Relancer » → il reçoit bien la notification **avec le bon son**, et peut accepter la commande
4. Vérifie qu'aucun doublon n'apparaît dans `livreurNotifications` après deux relances successives
5. Vérifie que `totalOrders` / `totalRevenue` du restaurant n'ont **pas** bougé après une relance

---

## Règles de travail

- Compile après chaque modification Android, et `node --check` après chaque modification de la Cloud Function
- Ne déploie **rien** en production sans validation explicite — la CF est partagée par plusieurs apps en service
- Les fichiers `index.js` et les apps client/livreur/taxi sont en production : ne modifie que ce qui relève des deux tâches ci-dessus
- Si une hypothèse te manque (schéma d'auth restaurant, nom exact d'un champ), vérifie dans le code plutôt que de deviner, et signale ce que tu n'as pas pu confirmer
