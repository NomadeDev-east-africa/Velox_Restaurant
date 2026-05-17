# Contexte applicatif & schéma Firestore — Velox Restaurant (Nomade253)

Ce document décrit le fonctionnement complet de l'application, ses interactions
Firebase/Firestore, et les exigences pour la génération des règles de sécurité Firestore.

---

## 1. Présentation de l'application

**Nom :** Velox Restaurant (marque Nomade253)
**Package :** `com.example.velox_restaurant`
**Firebase project :** `nomade253-478a9`
**Type :** Application Android native Kotlin — interface de gestion pour les restaurants
partenaires de la plateforme Nomade253.

### Rôle de l'app
L'app est utilisée **uniquement par les gérants de restaurant** (pas les clients finaux,
pas les livreurs). Elle permet de :
- Recevoir et gérer les commandes en temps réel
- Gérer le menu (plats, catégories, disponibilités)
- Créer et gérer des promotions
- Consulter les statistiques de vente
- Recevoir des notifications push (FCM) pour les nouvelles commandes

### Autres acteurs de la plateforme (apps séparées)
- **App client** : passe des commandes, paie, suit la livraison
- **App livreur** : reçoit des notifications de livraison disponible, accepte les courses

---

## 2. Authentification Firebase Auth

- **Méthode :** Email + mot de passe (`signInWithEmailAndPassword`)
- **UID Firebase Auth = restaurantId Firestore** : le document du restaurant dans
  `restaurants/{uid}` a exactement le même ID que l'UID Firebase Auth du compte
- **Règle critique à la connexion :** `restaurants/{uid}.isActive == true` doit être
  vrai — sinon la connexion est refusée et l'utilisateur est déconnecté immédiatement
- **FCM token :** à chaque connexion, le token FCM courant est sauvegardé dans
  `restaurants/{uid}.fcmToken` avec `fcmTokenUpdatedAt`

---

## 3. Collections Firestore

> Convention : tous les champs sont en **camelCase**. Aucun underscore.

---

### 3.1 `restaurants` (collection racine)

Document ID = UID Firebase Auth du gérant.

| Champ | Type | Description |
|---|---|---|
| `name` | String | Nom du restaurant |
| `email` | String | Email du compte |
| `phone` | String | Téléphone |
| `address` | String | Adresse physique |
| `imageUrl` | String | URL Firebase Storage de la bannière |
| `description` | String | Description courte |
| `isOpen` | Boolean | Restaurant ouvert/fermé (visible clients) |
| `isActive` | Boolean | Compte activé par l'admin Nomade253 |
| `rating` | Number | Note moyenne (calculée externalement) |
| `totalOrders` | Number | Compteur total de commandes complétées |
| `totalRevenue` | Number | Revenus totaux cumulés |
| `fcmToken` | String | Token FCM actuel pour notifications push |
| `fcmTokenUpdatedAt` | Timestamp | Date de mise à jour du token FCM |
| `createdAt` | Timestamp | Date de création du compte |
| `updatedAt` | Timestamp | Dernière modification |

**Accès depuis l'app restaurant :**
- **Lecture** : l'app lit son propre document pour afficher le profil et les stats
- **Écriture** : l'app met à jour `fcmToken`, `fcmTokenUpdatedAt`, `isOpen`
- **Écriture via Cloud Function** : l'incrément de `totalOrders` (+1) et `totalRevenue`
  (+montant) est déclenché par la CF `sendOrderReadyNotifications` via Admin SDK

---

### 3.2 `restaurants/{restaurantId}/categories` (sous-collection)

| Champ | Type | Description |
|---|---|---|
| `name` | String | Nom de la catégorie |
| `isDefault` | Boolean | Catégorie par défaut (Pizzas, Burgers…) |
| `restaurantId` | String | ID du restaurant parent |
| `createdAt` | Timestamp | Date de création |

**Accès depuis l'app restaurant :**
- **Lecture** : stream temps réel des catégories du restaurant connecté
- **Écriture** : ajout/suppression de catégories personnalisées
- Initialisation des catégories par défaut si la sous-collection est vide

---

### 3.3 `menuItems` (collection racine)

Document ID = auto-généré par Firestore.

| Champ | Type | Description |
|---|---|---|
| `restaurantId` | String | ID du restaurant propriétaire |
| `name` | String | Nom du plat |
| `description` | String | Description |
| `price` | Number | Prix en FDJ |
| `imageUrl` | String? | URL Firebase Storage de la photo |
| `category` | String | Nom de la catégorie |
| `isAvailable` | Boolean | Plat disponible à la commande |
| `preparationTime` | Number | Temps de préparation en minutes |
| `createdAt` | Timestamp | Date de création |
| `updatedAt` | Timestamp | Dernière modification |

**Accès depuis l'app restaurant :**
- **Lecture** : stream temps réel filtré par `restaurantId == auth.uid`
- **Écriture** : CRUD complet — création, modification, suppression, toggle disponibilité
- **Accès client** : l'app client lit aussi cette collection pour afficher le menu

---

### 3.4 `orders` (collection racine)

Document ID = auto-généré par Firestore (créé par l'app client).

| Champ | Type | Description |
|---|---|---|
| `restaurantId` | String | ID du restaurant concerné |
| `userId` | String | UID du client qui a passé la commande |
| `restaurantName` | String | Snapshot du nom du restaurant |
| `restaurantImageUrl` | String | Snapshot de l'image du restaurant |
| `customerName` | String | Nom du client |
| `customerPhone` | String | Téléphone du client |
| `items` | Array<Map> | Liste des articles (voir structure ci-dessous) |
| `deliveryFee` | Number | Frais de livraison |
| `total` | Number | Total de la commande en FDJ |
| `status` | String | Statut (voir valeurs ci-dessous) |
| `paymentMethod` | String | Mode de paiement |
| `deliveryAddress` | String | Adresse de livraison |
| `deliveryLocation` | GeoPoint? | Coordonnées GPS de livraison |
| `addressDetails` | String | Compléments d'adresse |
| `deliveryDriverId` | String? | UID du livreur assigné |
| `deliveryDriverName` | String? | Nom du livreur |
| `estimatedPreparationTime` | Number? | Temps de préparation estimé (minutes) |
| `acceptedAt` | Timestamp? | Date d'acceptation |
| `readyAt` | Timestamp? | Date de mise en prêt |
| `pickedUpAt` | Timestamp? | Date de récupération par le livreur |
| `deliveredAt` | Timestamp? | Date de livraison |
| `cancelledAt` | Timestamp? | Date d'annulation |
| `cancellationReason` | String? | Motif d'annulation |
| `createdAt` | Timestamp | Date de création (par l'app client) |
| `updatedAt` | Timestamp | Dernière modification |

**Structure d'un item dans `items` (Array<Map>) :**
```json
{
  "name": "Pizza Margarita",
  "quantity": 2,
  "price": 1500.0,
  "imageUrl": "https://...",
  "category": "Pizzas",
  "preparationTime": 15,
  "sauces": ["ketchup", "mayo"],
  "extras": ["fromage supplémentaire"]
}
```

**Valeurs du champ `status` (camelCase interdit — ce sont des strings lowercase) :**
| Valeur | Signification |
|---|---|
| `pending` | Nouvelle commande, en attente d'acceptation |
| `accepted` | Acceptée par le restaurant |
| `preparing` | En cours de préparation |
| `ready` | Prête, en attente du livreur |
| `delivering` | Prise en charge par le livreur |
| `completed` | Livrée et finalisée |
| `cancelled` | Annulée |

**Accès depuis l'app restaurant :**
- **Lecture (stream temps réel)** : commandes où `restaurantId == auth.uid`
  et `status in [pending, accepted, preparing, ready]`
- **Écriture directe** : le restaurant écrit `status` et les timestamps correspondants
  **uniquement pour** : `accepted` (`acceptedAt`), `preparing`, `cancelled` (`cancelledAt`,
  `cancellationReason`), et `estimatedPreparationTime`
- **`status: "ready"` — jamais écrit directement** : le restaurant appelle la Cloud
  Function `sendOrderReadyNotifications(orderId)` qui écrit via Admin SDK :
  `{ status: "ready", readyAt: serverTimestamp() }` — l'app ne touche pas ce champ
- L'app restaurant ne crée **jamais** de document `orders` — c'est l'app client

---

### 3.5 `promotions` (collection racine)

Document ID = auto-généré.

| Champ | Type | Description |
|---|---|---|
| `restaurantId` | String | ID du restaurant propriétaire |
| `type` | String | `"item"` ou `"category"` |
| `targetId` | String | ID du plat ou de la catégorie ciblée |
| `targetName` | String | Nom du plat/catégorie (snapshot) |
| `discountPercent` | Number | Pourcentage de réduction (1-100) |
| `label` | String | Texte affiché sur le badge promo |
| `startDate` | Timestamp? | Date de début |
| `endDate` | Timestamp? | Date de fin |
| `isActive` | Boolean | Promotion activée |
| `createdAt` | Timestamp | Date de création |

**Accès depuis l'app restaurant :**
- **Lecture** : stream filtré par `restaurantId == auth.uid`
- **Écriture** : création, toggle `isActive`, suppression
- **Accès client** : l'app client lit les promotions actives pour afficher les prix réduits

---

### 3.6 `livreurNotifications` (collection racine)

Document ID = auto-généré. Créé par la Cloud Function `sendOrderReadyNotifications`
lors du passage à `status: "ready"` (pas par l'app restaurant directement).

| Champ | Type | Description |
|---|---|---|
| `userId` | String | UID du livreur destinataire |
| `livreurName` | String | Nom du livreur |
| `type` | String | `"food_delivery"` |
| `orderId` | String | ID de la commande |
| `title` | String | Titre de la notification |
| `body` | String | Corps du message |
| `data` | Map | Données additionnelles (order_id, total, adresses…) |
| `createdAt` | Timestamp | Date de création |
| `read` | Boolean | Notification lue par le livreur |
| `accepted` | Boolean | Course acceptée par le livreur |

**Accès depuis l'app restaurant :**
- **Écriture** : la création batch est faite par la Cloud Function `sendOrderReadyNotifications`
  (Admin SDK) — elle cherche `livreurs` où `isOnline==true && isAvailable==true` et crée
  une notification par livreur disponible
- **Suppression** : l'app restaurant supprime directement les notifications non-acceptées
  (`accepted==false`) lors de l'annulation d'une commande
- L'app restaurant ne **lit** jamais cette collection

---

### 3.7 `livreurs` (collection racine) — lecture seule depuis l'app restaurant

Document ID = UID Firebase Auth du livreur.

| Champ | Type | Utilisé par l'app restaurant |
|---|---|---|
| `name` | String | Oui — pour remplir `livreurName` dans les notifications |
| `isOnline` | Boolean | Oui — filtre pour l'envoi de notifications |
| `isAvailable` | Boolean | Oui — filtre pour l'envoi de notifications |

**Accès depuis l'app restaurant :**
- **Lecture ponctuelle** (non-stream) : `livreurs.whereEqualTo("isOnline", true).whereEqualTo("isAvailable", true).get()`
  uniquement lors de `markReady` pour identifier les destinataires

---

## 4. Firebase Storage

**Bucket :** `nomade253-478a9.appspot.com`

| Usage | Chemin | Qui écrit |
|---|---|---|
| Photo de profil restaurant | `restaurants/{restaurantId}/banner.jpg` | App restaurant |
| Photos des plats | `menuItems/{restaurantId}/{itemId}.jpg` | App restaurant |

L'app utilise Coil (`AsyncImage`) pour charger les images avec fallback en cas d'erreur.

---

## 5. Firebase Cloud Messaging (FCM)

### Notifications reçues par l'app restaurant
- **Déclencheur :** Cloud Function `sendOrderReadyNotifications` appelée par l'app livreur
  (ou directement depuis le backend) pour notifier le restaurant
- **Canal Android :** `ORDERS_CHANNEL` (défini dans `RestaurantApplication`)
- **Handler :** `RestaurantFcmService` — affiche une notification système avec
  `PendingIntent` vers `MainActivity`

### Notifications envoyées par l'app restaurant
- Lors de `markReady` : appel Cloud Function `sendOrderReadyNotifications(orderId)`
  qui envoie FCM aux livreurs ET au client via le backend

### Mise à jour du token FCM
- À chaque login : `restaurants/{uid}.fcmToken` mis à jour
- Quand le token change : `RestaurantFcmService.onNewToken()` met à jour Firestore

---

## 6. Règles Firestore recommandées

> À partir de ce contexte, voici les exigences pour les règles de sécurité.

### Principe général
- Un restaurant **authentifié** ne peut lire/écrire que **ses propres données**
  (son document `restaurants/{uid}`, ses menuItems où `restaurantId == request.auth.uid`)
- Un restaurant ne peut **jamais créer** une commande
- Un restaurant ne peut **modifier que le statut** d'une commande (pas les montants, pas les items)
- Les livreurs ont leur propre UID distinct — pas accès à la collection `restaurants`
- Les clients créent les commandes mais ne peuvent pas modifier le statut

### Matrice d'accès par collection

| Collection | App Restaurant (read) | App Restaurant (write) | Règle clé |
|---|---|---|---|
| `restaurants/{uid}` | ✅ son document | ✅ son document | `request.auth.uid == uid` |
| `restaurants/{uid}/categories` | ✅ ses catégories | ✅ CRUD | `request.auth.uid == uid` |
| `menuItems` | ✅ où restaurantId == uid | ✅ CRUD sur les siens | `resource.data.restaurantId == request.auth.uid` |
| `orders` | ✅ où restaurantId == uid | ⚠️ statuts directs seulement | `accepted`/`preparing`/`cancelled` directs — `ready` via CF (Admin SDK) |
| `promotions` | ✅ où restaurantId == uid | ✅ CRUD sur les siennes | `resource.data.restaurantId == request.auth.uid` |
| `livreurs` | ✅ lecture (isOnline+isAvailable) | ❌ jamais | Lecture filtrée uniquement |
| `livreurNotifications` | ❌ | ✅ création + suppression | Écriture si auth != null |

### Champs protégés dans `orders`
Le restaurant peut mettre à jour **directement** (via app) uniquement :
`status` (valeurs `accepted`/`preparing`/`cancelled`), `acceptedAt`, `cancelledAt`,
`cancellationReason`, `estimatedPreparationTime`, `updatedAt`

> `status: "ready"` et `readyAt` sont écrits **exclusivement par la Cloud Function**
> via Admin SDK — les règles Firestore n'ont pas à les autoriser côté app.

Il ne doit **jamais** pouvoir modifier : `total`, `items`, `userId`, `deliveryFee`,
`restaurantId`, `createdAt`, `customerName`, `customerPhone`

---

## 7. Points d'attention pour les règles

1. **`isActive` non vérifiable dans les règles Firestore** : la vérification
   `isActive == true` est faite côté app au moment du login. Les règles Firestore
   ne peuvent pas facilement bloquer un restaurant désactivé sans lecture préalable —
   prévoir une Cloud Function ou un Custom Claim Firebase Auth pour ça.

2. **`livreurNotifications`** : la collection est écrite par l'app restaurant
   mais lue par l'app livreur. Les règles doivent permettre :
   - Création par n'importe quel restaurant authentifié
   - Lecture par le livreur destinataire (`userId == request.auth.uid`)
   - Mise à jour par le livreur (`read`, `accepted`)
   - Suppression par le restaurant (lors d'annulation)

3. **`markReady` via Cloud Function (Admin SDK)** : le passage à `status: "ready"` n'est
   **pas** une écriture directe depuis l'app — c'est un appel `onCall` à la CF
   `sendOrderReadyNotifications`. Le SDK Admin contourne les règles Firestore. En conséquence :
   - Les règles Firestore n'ont **pas besoin** d'autoriser le restaurant à écrire
     `status: "ready"` directement
   - La protection contre l'écriture non autorisée de `readyAt` est assurée par le fait
     que seule la CF (admin) peut le faire, pas l'app cliente
   - Les règles doivent quand même permettre à l'app de supprimer des documents
     `livreurNotifications` (annulation) depuis l'UID restaurant authentifié
