# Mission : toggle de disponibilité des sauces & suppléments

## Objectif

Le restaurateur doit pouvoir mettre une sauce ou un supplément **en rupture**
depuis l'app resto, et le client doit immédiatement cesser de pouvoir la commander.

Portée décidée : **globale au restaurant**. Un seul toggle « Sauce roquefort »
→ la sauce disparaît de **tous** les plats qui la proposent, en une action.

---

## 1. Où sont réellement stockées les sauces et suppléments

Dans Firestore, projet **`nomade253-478a9`**, collection **`menuItems`**,
champ **`optionGroups`** : un `Array<Map>` embarqué **dans chaque plat**.

Il n'existe **aucune collection dédiée** aux sauces ou aux suppléments.

Schéma d'un groupe :

```json
{
  "name": "Suppléments",
  "type": "multiple",          // "single" | "multiple"
  "required": false,
  "maxChoices": 0,             // optionnel, 0/absent = illimité
  "choices": [
    { "name": "Sauce roquefort", "price": 700 }
  ]
}
```

`price` = **supplément** en FDJ ajouté au prix de base (0 = inclus).

### Exemple réel — Chez Davido (`E6IrEGqgMGQzWOEgwiNa7dINh4y1`)

81 plats. Le groupe `Suppléments` est présent sur **15 plats**, chacun portant
**sa propre copie** des 7 mêmes choix :

| Choix | Prix |
|---|---|
| Sauce roquefort | 700 FDJ |
| Sauce à l'ail | 700 FDJ |
| Sauce crème | 700 FDJ |
| Sauce poivre | 700 FDJ |
| Sauce citronnée | 700 FDJ |
| Sauce diable | 700 FDJ |
| Sauce champignons | 700 FDJ |

C'est cette duplication qui impose un **batch write** : mettre « Sauce roquefort »
en rupture = réécrire 15 documents.

Autres groupes présents chez Davido, à ne **pas** traiter comme des suppléments
(ce sont des choix de configuration, pas des ingrédients en stock) :
`Cuisson`, `Type de pâtes`, `Boissons`, `Parfum`, `Garniture`, `Choix`.

---

## 2. PIÈGE — un système parallèle mort existe déjà, ne pas s'appuyer dessus

`ui/menu/MenuViewModel.kt:44-52` déclare :

```kotlin
const val SAUCES = "Sauces"
const val SUPPLEMENTS = "Suppléments"
val EDITABLE = listOf(SAUCES, SUPPLEMENTS)
```

Ces onglets créent des **documents `menuItems` séparés** avec `category = "Sauces"`
(via `addExtra()` ligne 130 et `deleteExtra()` ligne 151).

**Vérifié en base : 0 document** dans toute la collection `menuItems` ne porte
`category == "Sauces"` ou `"Suppléments"`. Et l'app client ne lit **jamais** ces
documents — elle ne consomme que `menuItems.optionGroups`.

→ Ce système n'a jamais été alimenté et n'a aucun effet côté client.
→ Un toggle branché dessus serait **sans effet**. Il faut le remplacer par une
   lecture de `optionGroups`, pas l'étendre.

Décide en connaissance de cause si tu supprimes `addExtra`/`deleteExtra`/`EDITABLE`
ou si tu les laisses en place ; signale ton choix.

---

## 3. Ce qui bloque aujourd'hui

### a) Les règles Firestore interdisent l'écriture

Source unique des règles :
`C:\Users\PC\Desktop\FINAL 253 NOMADE\client app\production\firestores regles unique\firestore_rules.rules`

Bloc `match /menuItems/{itemId}` → `allow update` :

```
onlyFields(['name','description','price','imageUrl',
            'category','isAvailable','preparationTime','updatedAt'])
```

`optionGroups` **n'y est pas** → toute écriture par le restaurant renvoie
`PERMISSION_DENIED`.

Il faut ajouter `'optionGroups'` à cette liste. **Ne déploie pas les règles
toi-même** : modifie le fichier, signale-le, et laisse le propriétaire déployer.

### b) Le modèle Kotlin ignore le champ

- `data/model/MenuItem.kt` — pas de champ `optionGroups`
- `data/remote/FirestoreMapper.kt:74-86` (`toMenuItem()`) — ne lit pas `optionGroups`
- `data/remote/MenuRepositoryImpl.kt:122-131` (`toFirestoreMap()`) — ne l'écrit pas

**Bonne nouvelle** : `updateMenuItem()` passe par `.update()` avec une map
explicite, donc l'app resto **n'écrase pas** les `optionGroups` existants
aujourd'hui. Rien n'est cassé, c'est juste invisible. **Préserve cette propriété** :
n'ajoute jamais `optionGroups` à `toFirestoreMap()` sans être certain que la
valeur en mémoire est complète et à jour, sinon tu écraseras les options du plat.

### c) Le cache Room doit migrer

- `data/local/entity/MenuItemEntity.kt` — pas de colonne `optionGroups`
- `data/local/RestaurantDatabase.kt:13` — `version = 2`

Ajouter la colonne impose de passer en `version = 3` **avec une `Migration(2, 3)`
explicite**. N'utilise pas `fallbackToDestructiveMigration()` : le cache sert de
mode dégradé hors ligne pour les commandes, le vider en production est inacceptable.
Stocke le JSON sérialisé dans une colonne `TEXT` (kotlinx.serialization est déjà
au projet, `MenuItem` est `@Serializable`).

---

## 4. Le champ à ajouter

Sur **chaque choix**, un booléen :

```json
{ "name": "Sauce roquefort", "price": 700, "available": false }
```

**Strictement additif.** Contraintes à respecter absolument :

- **Ne l'écris que lorsqu'il vaut `false`.** Absent = disponible. Ça évite de
  gonfler tous les documents et garde la compatibilité avec les menus déjà importés.
- Le client (`Velox_Client-main/lib/models/option_group.dart`) fait
  `OptionChoice.fromMap` qui ne lit que `name` et `price` → écrire `available`
  ne casse **rien** aujourd'hui. Mais tant que le client n'est pas patché pour
  filtrer les choix indisponibles, **le toggle reste décoratif**. C'est un chantier
  séparé, hors de ta mission : signale-le, ne touche pas au repo client.
- **camelCase strict** sur tous les noms de champs Firestore.
- Toute évolution de schéma doit être **additive** : les apps client et admin
  ignorent les champs inconnus, mais plantent si un champ attendu disparaît.

---

## 5. Travail attendu dans ce repo (`Velox_Restaurant`)

1. **Modèle** — `data/model/OptionGroup.kt` (nouveau) : `OptionGroup` +
   `OptionChoice` avec `available: Boolean = true`. Parsing tolérant : un
   `optionGroups` absent, `null` ou mal typé doit donner une liste vide, jamais
   une exception. Ajouter `optionGroups: List<OptionGroup> = emptyList()` à `MenuItem`.

2. **Mapper** — `FirestoreMapper.toMenuItem()` : lire `get("optionGroups")` en
   `List<*>`, mapper les `Map<*,*>`, ignorer silencieusement les entrées invalides.

3. **Repository** — nouvelle méthode dans `MenuRepository` + impl :

   ```kotlin
   suspend fun setChoiceAvailability(
       restaurantId: String,
       choiceName: String,
       available: Boolean,
   )
   ```

   Elle doit : retrouver tous les plats du restaurant dont un groupe contient un
   choix de ce nom, reconstruire l'`optionGroups` complet de chacun avec le flag
   modifié, et écrire le tout dans un **`WriteBatch`** (une seule transaction).
   Attention : la limite Firestore est de **500 opérations par batch** — découpe
   si nécessaire. Compare les noms de manière normalisée (casse + accents), comme
   le fait le client dans `option_selection.dart`.

4. **ViewModel** — exposer la liste agrégée : un choix distinct par `(nom, prix)`,
   avec le nombre de plats porteurs et l'état courant. Un choix est « en rupture »
   si `available == false` sur au moins un plat (et affiche un état incohérent si
   ce n'est pas uniforme).

5. **UI** — nouvel écran ou onglet « Sauces & Suppléments » listant ces choix
   agrégés avec un `Switch` chacun, le prix, et le nombre de plats impactés.
   Suivre le style Compose existant de `MenuScreen.kt`. Prévoir un état de
   chargement pendant le batch et remonter les erreurs comme le fait déjà
   `MenuViewModel` (`_uiState.update { it.copy(error = …) }`).

---

## 6. À ne pas casser

- **Le ticket de commande fonctionne déjà** : `FirestoreMapper.toOrderItem()`
  (ligne 103-127) lit les clés `extras` et `sauces` de chaque item de commande et
  n'en extrait que le `name`. `OrdersScreen.kt:320-332` et `467-479` les affichent.
  Ne modifie **ni** `toOrderItem()` **ni** l'affichage des commandes.
- Les sauces payantes de Davido arrivent dans **`extras`** (pas `sauces`), parce
  que le client route sur le nom du **groupe** : `option_selection.dart:40-41`,
  `isSauceGroup()` teste `groupName.contains("sauce")`. Le groupe s'appelle
  `Suppléments` → routage vers `extras`. C'est voulu : payant = supplément.
- Collection Firestore : `menuItems` (camelCase). Toute autre casse →
  `PERMISSION_DENIED`, cf. le commentaire `MenuRepositoryImpl.kt:33-35`.
- N'écris **jamais** un champ `id` dans un document `menuItems` : les règles
  `update` l'interdisent via `onlyFields`, cf. `MenuRepositoryImpl.kt:54-56`.

---

## 7. Livrable

Avant de coder, lis les fichiers cités et **confirme le plan**. Signale
explicitement :

- ce que tu fais du système mort `EDITABLE`/`addExtra`/`deleteExtra` ;
- le contenu exact de la migration Room 2 → 3 ;
- la ligne modifiée dans le fichier de règles (que tu ne déploies pas).

Ne modifie aucun fichier hors de `Velox_Restaurant`, à l'exception du fichier de
règles Firestore, dont tu ne changes que le `onlyFields` de `menuItems/update`.
