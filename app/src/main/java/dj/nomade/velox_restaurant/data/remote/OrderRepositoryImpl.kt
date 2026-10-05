package dj.nomade.velox_restaurant.data.remote

import dj.nomade.velox_restaurant.core.utils.withRetry
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dj.nomade.velox_restaurant.data.local.dao.OrderDao
import dj.nomade.velox_restaurant.data.local.entity.toDomain
import dj.nomade.velox_restaurant.data.local.entity.toEntity
import dj.nomade.velox_restaurant.data.model.Order
import dj.nomade.velox_restaurant.data.model.OrderStatus
import dj.nomade.velox_restaurant.data.model.PeriodStats
import dj.nomade.velox_restaurant.data.model.ResendResult
import dj.nomade.velox_restaurant.domain.repository.OrderRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
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
            // Note : les stats restaurant (totalOrders/totalRevenue) sont
            // calculées à la demande via getTotalStats() (aggregate queries),
            // pas dénormalisées ici. Les règles Firestore interdisent au
            // restaurant d'écrire ces champs sur son propre doc (anti-triche),
            // donc toute écriture sur restaurants/{id} ferait échouer la
            // transaction et bloquerait le passage à "ready".
            orders.document(orderId).update(
                mapOf(
                    "status" to OrderStatus.READY.value,
                    "readyAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
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
            // Toute la logique de notification est gérée côté serveur par la
            // Cloud Function sendOrderReadyNotifications (Admin SDK) :
            //   - FCM + création des livreurNotifications pour chaque livreur dispo
            //   - FCM au client ("commande prête")
            //   - incrément des stats restaurant (totalOrders/totalRevenue)
            //
            // L'app NE DOIT PAS créer de livreurNotifications elle-même : les
            // règles Firestore l'interdisent (livreurNotifications →
            // allow create: if false). L'ancienne écriture batch levait donc
            // PERMISSION_DENIED et empêchait l'appel de la fonction ci-dessous,
            // d'où l'absence totale de notification (client ET livreur).
            functions
                .getHttpsCallable("sendOrderReadyNotifications")
                .call(mapOf("orderId" to orderId))
                .await()
        }
    }

    override suspend fun resendToDrivers(orderId: String): Result<ResendResult> = runCatching {
        val result = functions
            .getHttpsCallable("resendOrderToDrivers")
            .call(mapOf("orderId" to orderId))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?> ?: emptyMap()
        ResendResult(
            success = data["success"] as? Boolean ?: false,
            driversNotified = (data["driversNotified"] as? Number)?.toInt() ?: 0,
            message = data["message"] as? String ?: "",
        )
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

    override suspend fun getOrdersInRange(
        restaurantId: String,
        startMs: Long,
        endMs: Long,
    ): List<Order> {
        val start = Timestamp(startMs / 1000, 0)
        val end = Timestamp(endMs / 1000, 0)
        return orders
            .whereEqualTo("restaurantId", restaurantId)
            .whereGreaterThanOrEqualTo("createdAt", start)
            .whereLessThan("createdAt", end)
            .get().await()
            .documents.mapNotNull { runCatching { it.toOrder() }.getOrNull() }
    }

    override suspend fun getPeriodStats(
        restaurantId: String,
        startMs: Long,
        endMs: Long,
    ): PeriodStats {
        val base = orders
            .whereEqualTo("restaurantId", restaurantId)
            .whereGreaterThanOrEqualTo("createdAt", Timestamp(startMs / 1000, 0))
            .whereLessThan("createdAt", Timestamp(endMs / 1000, 0))

        return try {
            coroutineScope {
                val allCount = async {
                    base.count().get(AggregateSource.SERVER).await().count
                }
                val cancelledCount = async {
                    base.whereEqualTo("status", OrderStatus.CANCELLED.value)
                        .count().get(AggregateSource.SERVER).await().count
                }
                // On somme `total` et `deliveryFee` plutôt que `subtotal` : les
                // agrégats serveur ignorent les documents où le champ est absent,
                // or les commandes anciennes n'ont pas de `subtotal`. total et
                // deliveryFee, eux, sont présents partout — leur différence donne
                // exactement le CA hors livraison.
                val completed = async {
                    base.whereEqualTo("status", OrderStatus.COMPLETED.value)
                        .aggregate(
                            AggregateField.count(),
                            AggregateField.sum("total"),
                            AggregateField.sum("deliveryFee"),
                        )
                        .get(AggregateSource.SERVER).await()
                }
                val done = completed.await()
                val grossTotal = (done.get(AggregateField.sum("total")) as? Number)?.toDouble() ?: 0.0
                val fees = (done.get(AggregateField.sum("deliveryFee")) as? Number)?.toDouble() ?: 0.0
                PeriodStats(
                    orderCount = allCount.await(),
                    completedCount = done.get(AggregateField.count()),
                    cancelledCount = cancelledCount.await(),
                    revenue = grossTotal - fees,
                    deliveryFees = fees,
                )
            }
        } catch (e: Exception) {
            // Couvre notamment l'absence de l'index composite
            // (restaurantId, status, createdAt) : on recalcule localement.
            crashlytics.log("getPeriodStats: repli local — ${e.message}")
            val docs = base.get().await()
                .documents.mapNotNull { runCatching { it.toOrder() }.getOrNull() }
            val done = docs.filter { it.status == OrderStatus.COMPLETED }
            PeriodStats(
                orderCount = docs.size.toLong(),
                completedCount = done.size.toLong(),
                cancelledCount = docs.count { it.status == OrderStatus.CANCELLED }.toLong(),
                revenue = done.sumOf { it.restaurantRevenue },
                deliveryFees = done.sumOf { it.deliveryFee },
            )
        }
    }

    override suspend fun getTotalStats(restaurantId: String): Pair<Long, Double> {
        val query = orders
            .whereEqualTo("restaurantId", restaurantId)
            .whereEqualTo("status", OrderStatus.COMPLETED.value)
        return try {
            val snapshot = query
                .aggregate(
                    AggregateField.count(),
                    AggregateField.sum("total"),
                    AggregateField.sum("deliveryFee"),
                )
                .get(AggregateSource.SERVER).await()
            val grossTotal = (snapshot.get(AggregateField.sum("total")) as? Number)?.toDouble() ?: 0.0
            val fees = (snapshot.get(AggregateField.sum("deliveryFee")) as? Number)?.toDouble() ?: 0.0
            // CA du restaurant = encaissé client - frais de livraison du livreur.
            snapshot.get(AggregateField.count()) to (grossTotal - fees)
        } catch (_: Exception) {
            // Fallback : calcul local si les aggregate queries échouent
            val docs = query.get().await()
                .documents.mapNotNull { runCatching { it.toOrder() }.getOrNull() }
            docs.size.toLong() to docs.sumOf { it.restaurantRevenue }
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
