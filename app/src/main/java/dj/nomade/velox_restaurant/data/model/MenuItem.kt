package dj.nomade.velox_restaurant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MenuItem(
    val id: String = "",
    val restaurantId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String? = null,
    val category: String = "",
    val isAvailable: Boolean = true,
    val preparationTime: Int = 15,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)
