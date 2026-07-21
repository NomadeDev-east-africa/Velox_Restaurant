package dj.nomade.velox_restaurant.domain.repository

import android.net.Uri
import dj.nomade.velox_restaurant.data.model.MenuItem
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun streamMenuItems(restaurantId: String): Flow<List<MenuItem>>
    suspend fun addMenuItem(item: MenuItem): String
    suspend fun updateMenuItem(item: MenuItem)
    suspend fun deleteMenuItem(itemId: String)
    suspend fun toggleItemAvailability(itemId: String, isAvailable: Boolean)
    suspend fun uploadMenuImage(restaurantId: String, itemId: String, imageUri: Uri): String

    /** Ne touche que `imageUrl` — le reste du plat (nom, prix, description) est préservé. */
    suspend fun updateItemImage(itemId: String, imageUrl: String)

    // Cache Room
    suspend fun cacheMenuItems(items: List<MenuItem>)
    fun streamCachedMenuItems(restaurantId: String): Flow<List<MenuItem>>
}
