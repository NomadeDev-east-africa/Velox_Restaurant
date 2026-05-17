package com.example.velox_restaurant.data.remote

import com.example.velox_restaurant.core.utils.withRetry
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.example.velox_restaurant.data.local.dao.OrderDao
import com.example.velox_restaurant.data.local.entity.toDomain
import com.example.velox_restaurant.data.local.entity.toEntity
import com.example.velox_restaurant.data.model.Order
import com.example.velox_restaurant.data.model.OrderStatus
import com.example.velox_restaurant.domain.repository.OrderRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val orderDao: OrderDao,
    private val crashlytics: FirebaseCrashlytics,
) : OrderRepository {

    private val orders get() = firestore.collection("orders")

    // ── Streams ──────────────────────────────────────────────────────────────

    override fun streamActiveOrders(restaurantId: String): Flow<List<Order>> = callbackFlow {
        val activeStatuses = OrderStatus.active.map { it.value }
        val listener = orders
            .whereEqualTo("restaurantId", restaurantId)
            .whereIn("status", activeStatuses)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull {
                    runCatching { it.toOrder() }.getOrNull()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    override fun streamOrderHistory(restaurantId: String): Flow<List<Order>> = callbackFlow {
        val listener = orders
            .whereEqualTo("restaurantId", restaurantId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull {
                    runCatching { it.toOrder() }.getOrNull()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    // ── Changements de statut ─────────────────────────────────────────────

    override suspend fun acceptOrder(orderId: String, estimatedPreparationTime: Int) {
        withRetry {
            orders.document(orderId).update(
                mapOf(
                    "status" to OrderStatus.ACCEPTED.value,
                    "acceptedAt" to FieldValue.serverTimestamp(),
                    "estimatedPreparationTime" to estimatedPreparationTime,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
    }

    override suspend fun startPreparation(orderId: String) {
        withRetry {
            orders.document(orderId).update(
                mapOf(
                    "status" to OrderStatus.PREPARING.value,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
    }

    override suspend fun markReady(orderId: String) {
        withRetry {
            firestore.runTransaction { transaction ->
                val orderDoc = transaction.get(orders.document(orderId))
                val total = orderDoc.getDouble("total") ?: 0.0
                val restaurantId = orderDoc.getString("restaurantId") ?: ""

                transaction.update(
                    orders.document(orderId),
                    mapOf(
                        "status" to OrderStatus.READY.value,
                        "readyAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                if (restaurantId.isNotEmpty()) {
                    transaction.update(
                        firestore.collection("restaurants").document(restaurantId),
                        mapOf(
                            "totalOrders" to FieldValue.increment(1L),
                            "totalRevenue" to FieldValue.increment(total),
                        )
                    )
                }
            }.await()
        }
    }

    override suspend fun cancelOrder(orderId: String, reason: String) {
        withRetry {
            orders.document(orderId).update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.value,
                    "cancelledAt" to FieldValue.serverTimestamp(),
                    "cancellationReason" to reason,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
        cancelDeliveryNotifications(orderId)
    }

    // ── Cloud Function + livreurNotifications ────────────────────────────

    override suspend fun sendReadyNotifications(orderId: String): Result<Unit> = runCatching {
        withRetry {
            // 1. Récupérer la commande pour les données de notification
            val orderDoc = orders.document(orderId).get().await()
            val order = orderDoc.toOrder()

            // 2. Livreurs disponibles
            val livreurs = firestore.collection("livreurs")
                .whereEqualTo("isOnline", true)
                .whereEqualTo("isAvailable", true)
                .get().await()

            // 3. Batch write dans livreurNotifications (nom exact, sans underscore)
            if (livreurs.documents.isNotEmpty()) {
                val batch = firestore.batch()
                livreurs.documents.forEach { livreurDoc ->
                    val ref = firestore.collection("livreurNotifications").document()
                    batch.set(
                        ref, mapOf(
                            "userId" to livreurDoc.id,
                            "livreurName" to (livreurDoc.getString("name") ?: ""),
                            "type" to "food_delivery",
                            "orderId" to orderId,
                            "title" to "Nouvelle livraison disponible",
                            "body" to "Commande prête chez ${order.restaurantName}",
                            "data" to mapOf(
                                "order_id" to orderId,
                                "order_type" to "food",
                                "restaurant_name" to order.restaurantName,
                                "pickup_address" to order.deliveryAddress,
                                "delivery_address" to order.deliveryAddress,
                                "customer_name" to order.customerName,
                                "customer_phone" to order.customerPhone,
                                "total" to order.total.toInt(),
                            ),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "read" to false,
                            "accepted" to false,
                        )
                    )
                }
                batch.commit().await()
            }

            // 4. Appel Cloud Function (FCM vers client + livreurs)
            functions
                .getHttpsCallable("sendOrderReadyNotifications")
                .call(mapOf("orderId" to orderId))
                .await()
        }
    }

    private suspend fun cancelDeliveryNotifications(orderId: String) {
        try {
            val docs = firestore.collection("livreurNotifications")
                .whereEqualTo("orderId", orderId)
                .whereEqualTo("accepted", false)
                .get().await()
            if (docs.documents.isNotEmpty()) {
                val batch = firestore.batch()
                docs.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        } catch (e: Exception) {
            crashlytics.recordException(e)
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────

    override suspend fun getOrdersForDay(restaurantId: String, startOfDayMs: Long): List<Order> {
        val start = Timestamp(startOfDayMs / 1000, 0)
        val end = Timestamp(startOfDayMs / 1000 + TimeUnit.DAYS.toSeconds(1), 0)
        return orders
            .whereEqualTo("restaurantId", restaurantId)
            .whereGreaterThanOrEqualTo("createdAt", start)
            .whereLessThan("createdAt", end)
            .get().await()
            .documents.mapNotNull { runCatching { it.toOrder() }.getOrNull() }
    }

    override suspend fun getOrdersForMonth(restaurantId: String, startOfMonthMs: Long): List<Order> {
        val start = Timestamp(startOfMonthMs / 1000, 0)
        return orders
            .whereEqualTo("restaurantId", restaurantId)
            .whereGreaterThanOrEqualTo("createdAt", start)
            .get().await()
            .documents.mapNotNull { runCatching { it.toOrder() }.getOrNull() }
    }

    override suspend fun getTotalStats(restaurantId: String): Pair<Long, Double> {
        val query = orders
            .whereEqualTo("restaurantId", restaurantId)
            .whereEqualTo("status", OrderStatus.COMPLETED.value)
        return try {
            val countSnapshot = query.count().get(AggregateSource.SERVER).await()
            val totalCount = countSnapshot.count
            val sumSnapshot = query
                .aggregate(AggregateField.sum("total"))
                .get(AggregateSource.SERVER).await()
            val rawRevenue = sumSnapshot.get(AggregateField.sum("total"))
            val totalRevenue = (rawRevenue as? Number)?.toDouble() ?: 0.0
            totalCount to totalRevenue
        } catch (_: Exception) {
            // Fallback : calcul local si les aggregate queries échouent
            val docs = query.get().await()
                .documents.mapNotNull { runCatching { it.toOrder() }.getOrNull() }
            docs.size.toLong() to docs.sumOf { it.total }
        }
    }

    // ── Cache Room ────────────────────────────────────────────────────────

    override suspend fun cacheActiveOrders(orders: List<Order>) {
        orderDao.clearActiveOrders(orders.firstOrNull()?.restaurantId ?: return)
        orderDao.insertOrders(orders.map { it.toEntity() })
    }

    override fun streamCachedActiveOrders(restaurantId: String): Flow<List<Order>> =
        orderDao.streamActiveOrders(restaurantId).map { entities ->
            entities.map { it.toDomain() }
        }.distinctUntilChanged()
}
