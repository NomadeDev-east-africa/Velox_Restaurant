package com.example.velox_restaurant.domain.repository

import com.example.velox_restaurant.data.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun streamCategories(restaurantId: String): Flow<List<Category>>
    suspend fun ensureDefaultCategories(restaurantId: String)
    suspend fun addCategory(restaurantId: String, name: String): String
    suspend fun deleteCategory(restaurantId: String, categoryId: String)
}
