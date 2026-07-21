package dj.nomade.velox_restaurant.domain.repository

import dj.nomade.velox_restaurant.data.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    /** Référentiel global `menuCategories`, trié par `order`. Lecture seule. */
    fun streamCategories(): Flow<List<Category>>
}
