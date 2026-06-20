package dj.nomade.velox_restaurant.domain.repository

import dj.nomade.velox_restaurant.data.model.Promotion
import kotlinx.coroutines.flow.Flow

interface PromotionRepository {
    fun streamPromotions(restaurantId: String): Flow<List<Promotion>>
    suspend fun addPromotion(promotion: Promotion): String
    suspend fun togglePromotion(promoId: String, isActive: Boolean)
    suspend fun deletePromotion(promoId: String)
}
