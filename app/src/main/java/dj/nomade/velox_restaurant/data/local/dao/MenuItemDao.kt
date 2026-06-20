package dj.nomade.velox_restaurant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dj.nomade.velox_restaurant.data.local.entity.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {

    @Query("""
        SELECT * FROM menu_items
        WHERE restaurantId = :restaurantId
        ORDER BY category ASC, name ASC
    """)
    fun streamMenuItems(restaurantId: String): Flow<List<MenuItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItemEntity>)

    @Query("DELETE FROM menu_items WHERE restaurantId = :restaurantId")
    suspend fun clearMenuItems(restaurantId: String)
}
