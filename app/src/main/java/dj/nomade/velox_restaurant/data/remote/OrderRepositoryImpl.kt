package dj.nomade.velox_restaurant.data.remote

import dj.nomade.velox_restaurant.core.utils.withRetry
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dj.nomade.velox_restaurant.data.local.dao.OrderDao
import dj.nomade.velox_restaurant.data.local.entity.toDomain
import dj.nomade.velox_restaurant.data.local.entity.toEntity
import dj.nomade.velox_restaurant.data.model.Order
import dj.nomade.velox_restaurant.data.model.OrderStatus
import dj.nomade.velox_restaurant.domain.repository.OrderRepository
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
