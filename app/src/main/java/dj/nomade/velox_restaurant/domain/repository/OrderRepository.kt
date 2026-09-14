package dj.nomade.velox_restaurant.domain.repository

import dj.nomade.velox_restaurant.data.model.Order
import dj.nomade.velox_restaurant.data.model.PeriodStats
import dj.nomade.velox_restaurant.data.model.ResendResult
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

    /**
     * Cloud Function dédiée `resendOrderToDrivers` : relance l'offre vers les
     * livreurs actuellement en ligne, sans rejouer [sendReadyNotifications]
     * (qui rechangerait le statut et gonflerait les stats restaurant).
     */
    suspend fun resendToDrivers(orderId: String): Result<ResendResult>

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
