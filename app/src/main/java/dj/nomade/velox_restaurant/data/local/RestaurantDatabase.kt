package dj.nomade.velox_restaurant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dj.nomade.velox_restaurant.data.local.dao.MenuItemDao
import dj.nomade.velox_restaurant.data.local.dao.OrderDao
import dj.nomade.velox_restaurant.data.local.entity.MenuItemEntity
import dj.nomade.velox_restaurant.data.local.entity.OrderEntity

@Database(
    entities = [OrderEntity::class, MenuItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RestaurantDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun menuItemDao(): MenuItemDao
}
