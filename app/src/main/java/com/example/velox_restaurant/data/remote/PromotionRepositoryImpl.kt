package com.example.velox_restaurant.data.remote

import com.example.velox_restaurant.core.utils.withRetry
import com.example.velox_restaurant.data.model.Promotion
import com.example.velox_restaurant.domain.repository.PromotionRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromotionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PromotionRepository {

    private val collection get() = firestore.collection("promotions")

    override fun streamPromotions(restaurantId: String): Flow<List<Promotion>> = callbackFlow {
        val listener = collection
            .whereEqualTo("restaurantId", restaurantId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull {
                    runCatching { it.toPromotion() }.getOrNull()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    override suspend fun addPromotion(promotion: Promotion): String {
        val data = promotion.toFirestoreMap() + mapOf("createdAt" to FieldValue.serverTimestamp())
        val ref = collection.add(data).await()
        return ref.id
    }

    override suspend fun togglePromotion(promoId: String, isActive: Boolean) {
        withRetry {
            collection.document(promoId).update("isActive", isActive).await()
        }
    }

    override suspend fun deletePromotion(promoId: String) {
        withRetry {
            collection.document(promoId).delete().await()
        }
    }

    // ── Mapper domain → Firestore ──────────────────────────────────────────

    private fun Promotion.toFirestoreMap(): Map<String, Any?> = mapOf(
        "restaurantId" to restaurantId,
        "type" to type,
        "targetId" to targetId,
        "targetName" to targetName,
        "discountPercent" to discountPercent,
        "label" to label,
        "startDate" to startDate?.let { Timestamp(it / 1000, 0) },
        "endDate" to endDate?.let { Timestamp(it / 1000, 0) },
        "isActive" to isActive,
    )
}
