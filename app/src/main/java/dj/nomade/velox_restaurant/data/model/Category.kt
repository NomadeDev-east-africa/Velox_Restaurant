package dj.nomade.velox_restaurant.data.model

data class Category(
    val id: String = "",
    val restaurantId: String = "",
    val name: String = "",
    val isDefault: Boolean = false,
)
