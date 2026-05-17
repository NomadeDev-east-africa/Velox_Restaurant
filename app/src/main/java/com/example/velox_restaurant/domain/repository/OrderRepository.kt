package com.example.velox_restaurant.domain.repository

import com.example.velox_restaurant.data.model.Order
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
    suspend fun getOrdersForMonth(restaurantId: String, startOfMonthMs: Long): List<Order>
    suspend fun getTotalStats(restaurantId: String): Pair<Long, Double>

    // Cache Room
    suspend fun cacheActiveOrders(orders: List<Order>)
    fun streamCachedActiveOrders(restaurantId: String): Flow<List<Order>>
}
