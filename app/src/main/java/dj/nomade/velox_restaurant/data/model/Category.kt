package dj.nomade.velox_restaurant.data.model

/**
 * Entrée du référentiel **global** `menuCategories` (collection racine).
 *
 * Ces documents ne portent pas de `restaurantId` : ils sont partagés par tous les
 * restaurants et servent uniquement à connaître l'ordre d'affichage officiel d'un
 * nom de catégorie. Les catégories réellement affichées pour un restaurant sont
 * déduites des `category` de ses propres plats — voir `MenuViewModel.categories`.
 */
data class Category(
    val id: String = "",
    val name: String = "",
    val order: Int = Int.MAX_VALUE,
    val imageUrl: String? = null,
)
