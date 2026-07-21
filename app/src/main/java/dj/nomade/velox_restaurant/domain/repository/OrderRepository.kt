package dj.nomade.velox_restaurant.domain.repository

import dj.nomade.velox_restaurant.data.model.Order
import dj.nomade.velox_restaurant.data.model.PeriodStats
import kotlinx.coroutines.flow.Flow

interface OrderRepository {

    // Streams temps réel
    fun streamActiveOrders(restaurantId: String): Flow<List<Order>>
    fun streamOrderHistory(restaurantId: String): Flow<List<Order>>

    // Changements de statut (retry x3 géré dans l'implémentation)
    suspend fun acceptOrder(orderId: String, estimatedPreparationTime: Int)
    suspend fun startPreparation(orderId: String)
    suspend fun markReady(orderId: String)
    suspend fun cancelOrder(orderId: String, reason: String)

    // Cloud Function : notifie livreurs disponibles + client
    suspend fun sendReadyNotifications(orderId: String): Result<Unit>

    // Stats
    suspend fun getOrdersForDay(restaurantId: String, startOfDayMs: Long): List<Order>
    suspend fun getOrdersInRange(restaurantId: String, startMs: Long, endMs: Long): List<Order>
    suspend fun getTotalStats(restaurantId: String): Pair<Long, Double>

    /**
     * Agrégat comptable sur `[startMs, endMs[`, calculé côté serveur.
     * Une poignée de lectures facturées quel que soit le volume de commandes.
     */
    suspend fun getPeriodStats(restaurantId: String, startMs: Long, endMs: Long): PeriodStats

    // Cache Room
    suspend fun cacheActiveOrders(orders: List<Order>)
    fun streamCachedActiveOrders(restaurantId: String): Flow<List<Order>>
}
