package dj.nomade.velox_restaurant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dj.nomade.velox_restaurant.data.model.MenuItem
import dj.nomade.velox_restaurant.data.model.OptionGroup
import kotlinx.serialization.json.Json

/** Les groupes d'options sont stockés sérialisés : Room ne sait pas persister une liste. */
private val optionGroupsJson = Json { ignoreUnknownKeys = true }

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
    @ColumnInfo(defaultValue = "[]") val optionGroups: String,
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
    optionGroups = runCatching { optionGroupsJson.encodeToString(this.optionGroups) }
        .getOrDefault("[]"),
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
    // Cache écrit par une version antérieure ou JSON corrompu : liste vide, jamais de crash.
    optionGroups = runCatching {
        optionGroupsJson.decodeFromString<List<OptionGroup>>(optionGroups)
    }.getOrDefault(emptyList()),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
