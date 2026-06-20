package dj.nomade.velox_restaurant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dj.nomade.velox_restaurant.data.model.MenuItem

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String?,
    val category: String,
    val isAvailable: Boolean,
    val preparationTime: Int,
    val createdAt: Long?,
    val updatedAt: Long?,
)

fun MenuItem.toEntity(): MenuItemEntity = MenuItemEntity(
    id = id,
    restaurantId = restaurantId,
    name = name,
    description = description,
    price = price,
    imageUrl = imageUrl,
    category = category,
    isAvailable = isAvailable,
    preparationTime = preparationTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun MenuItemEntity.toDomain(): MenuItem = MenuItem(
    id = id,
    restaurantId = restaurantId,
    name = name,
    description = description,
    price = price,
    imageUrl = imageUrl,
    category = category,
    isAvailable = isAvailable,
    preparationTime = preparationTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
