package dj.nomade.velox_restaurant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dj.nomade.velox_restaurant.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query("""
        SELECT * FROM active_orders
        WHERE restaurantId = :restaurantId
        AND status IN ('pending','accepted','preparing','ready')
        ORDER BY createdAt DESC
    """)
    fun streamActiveOrders(restaurantId: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Query("""
        DELETE FROM active_orders
        WHERE restaurantId = :restaurantId
        AND status IN ('pending','accepted','preparing','ready')
    """)
    suspend fun clearActiveOrders(restaurantId: String)
}
