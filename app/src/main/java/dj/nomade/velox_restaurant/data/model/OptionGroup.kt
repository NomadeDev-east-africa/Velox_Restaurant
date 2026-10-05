package dj.nomade.velox_restaurant.data.model

import kotlinx.serialization.Serializable
import java.text.Normalizer

/**
 * Groupes d'options embarqués dans chaque document `menuItems` (champ `optionGroups`).
 *
 * Il n'existe aucune collection dédiée aux sauces ni aux suppléments : chaque plat
 * porte sa propre copie des choix qu'il propose. C'est la seule source que l'app
 * client consomme.
 *
 * ```
 * { "name": "Suppléments", "type": "multiple", "required": false,
 *   "choices": [ { "name": "Sauce roquefort", "price": 700 } ] }
 * ```
 */
@Serializable
data class OptionGroup(
    val name: String = "",
    val type: String = "single",
    val required: Boolean = false,
    val maxChoices: Int = 0,
    val choices: List<OptionChoice> = emptyList(),
)

/** `price` = supplément en FDJ ajouté au prix de base du plat (0 = inclus). */
@Serializable
data class OptionChoice(
    /**
     * Identifiant stable généré par l'admin (`ch_xxxxxxxx`), recopié par l'app client dans
     * chaque commande (`choiceId`) pour l'intégration Loyverse. Absent sur les anciens plats.
     * Doit survivre à toute réécriture de `optionGroups`.
     */
    val id: String? = null,
    val name: String = "",
    val price: Double = 0.0,
    /** Absent en base = disponible. N'est écrit que lorsqu'il vaut `false`. */
    val available: Boolean = true,
)

/**
 * Lecture tolérante du champ Firestore : une valeur absente, `null` ou mal typée
 * donne une liste vide, jamais une exception. Les entrées invalides d'un tableau
 * par ailleurs valide sont ignorées silencieusement.
 */
fun parseOptionGroups(raw: Any?): List<OptionGroup> =
    (raw as? List<*>)?.mapNotNull { it.toOptionGroup() } ?: emptyList()

private fun Any?.toOptionGroup(): OptionGroup? {
    val map = this as? Map<*, *> ?: return null
    val name = map["name"] as? String ?: return null
    return OptionGroup(
        name = name,
        type = map["type"] as? String ?: "single",
        required = map["required"] as? Boolean ?: false,
        maxChoices = (map["maxChoices"] as? Number)?.toInt() ?: 0,
        choices = (map["choices"] as? List<*>)?.mapNotNull { it.toOptionChoice() } ?: emptyList(),
    )
}

private fun Any?.toOptionChoice(): OptionChoice? {
    val map = this as? Map<*, *> ?: return null
    val name = map["name"] as? String ?: return null
    return OptionChoice(
        id = (map["id"] as? String)?.takeIf { it.isNotBlank() },
        name = name,
        price = (map["price"] as? Number)?.toDouble() ?: 0.0,
        available = map["available"] as? Boolean ?: true,
    )
}

/**
 * Sérialise les groupes pour un `.update()` Firestore. **Additif strict** : le flag
 * `available` n'est écrit que lorsqu'il vaut `false` (absent = disponible), pour ne
 * pas gonfler les documents ni casser la compatibilité avec les menus déjà importés.
 */
fun List<OptionGroup>.toFirestoreList(): List<Map<String, Any?>> = map { group ->
    buildMap {
        put("name", group.name)
        put("type", group.type)
        put("required", group.required)
        if (group.maxChoices > 0) put("maxChoices", group.maxChoices)
        put("choices", group.choices.map { choice ->
            buildMap<String, Any?> {
                choice.id?.let { put("id", it) }
                put("name", choice.name)
                put("price", choice.price)
                if (!choice.available) put("available", false)
            }
        })
    }
}

/**
 * Comparaison des noms insensible à la casse et aux accents, alignée sur le client
 * (`option_selection.dart`). Sert à agréger les choix et à cibler la mise en rupture.
 */
fun String.normalizeOptionName(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .trim()