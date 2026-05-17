package com.example.velox_restaurant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.velox_restaurant.data.local.dao.MenuItemDao
import com.example.velox_restaurant.data.local.dao.OrderDao
import com.example.velox_restaurant.data.local.entity.MenuItemEntity
import com.example.velox_restaurant.data.local.entity.OrderEntity

@Database(
    entities = [OrderEntity::class, MenuItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RestaurantDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun menuItemDao(): MenuItemDao
}
