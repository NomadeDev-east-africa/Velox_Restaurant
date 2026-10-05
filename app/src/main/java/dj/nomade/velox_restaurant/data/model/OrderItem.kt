package dj.nomade.velox_restaurant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
    val name: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "",
    val preparationTime: Int = 0,
    val sauces: List<String> = emptyList(),
    val extras: List<String> = emptyList(),
    /**
     * Options choisies avec leur groupe d'origine (« Viande », « Boisson »…), dans
     * l'ordre de la commande. Vide pour les lignes mises en cache avant ce champ :
     * l'affichage retombe alors sur [sauces] / [extras].
     */
    val options: List<OrderItemOption> = emptyList(),
) {
    /**
     * Options regroupées pour l'affichage : une étiquette par groupe.
     * Sans nom de groupe (commandes antérieures au champ `group` de l'app client),
     * on garde l'ancien regroupement SAUCE / SUPPLÉMENT.
     */
    fun optionTags(): List<OptionTag> {
        val source = options.ifEmpty {
            sauces.map { OrderItemOption(it, group = null, isSauce = true) } +
                extras.map { OrderItemOption(it, group = null, isSauce = false) }
        }
        val byKey = LinkedHashMap<Pair<String?, OptionTagKind>, MutableList<String>>()
        source.forEach { option ->
            val group = option.group?.trim()?.takeIf { it.isNotEmpty() }
            val kind = when {
                group == null -> if (option.isSauce) OptionTagKind.SAUCE else OptionTagKind.EXTRA
                group.normalizeOptionName().contains("sauce") -> OptionTagKind.SAUCE
                group.normalizeOptionName().let { it.contains("suppl") || it.contains("extra") } ->
                    OptionTagKind.EXTRA
                else -> OptionTagKind.CHOICE
            }
            byKey.getOrPut(group to kind) { mutableListOf() }.add(option.name)
        }
        return byKey.map { (key, names) -> OptionTag(key.first, key.second, names) }
    }
}

/** Une option choisie par le client, telle qu'écrite dans `orders.items[].sauces|extras[]`. */
@Serializable
data class OrderItemOption(
    val name: String,
    /** Nom du groupe côté menu (« Viande », « Suppléments »…). Absent sur les anciennes commandes. */
    val group: String? = null,
    /** Rangée par l'app client dans `sauces` (sinon `extras`). */
    val isSauce: Boolean = false,
)

enum class OptionTagKind { SAUCE, EXTRA, CHOICE }

/** Étiquette affichée sous un article : [label] `null` = libellé par défaut du [kind]. */
data class OptionTag(val label: String?, val kind: OptionTagKind, val values: List<String>)
