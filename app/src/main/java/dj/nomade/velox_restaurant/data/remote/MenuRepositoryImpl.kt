package dj.nomade.velox_restaurant.data.remote

import android.content.Context
import android.net.Uri
import dj.nomade.velox_restaurant.core.utils.compressImageUri
import dj.nomade.velox_restaurant.core.utils.isValidImageUri
import dj.nomade.velox_restaurant.core.utils.withRetry
import dj.nomade.velox_restaurant.data.local.dao.MenuItemDao
import dj.nomade.velox_restaurant.data.local.entity.toDomain
import dj.nomade.velox_restaurant.data.local.entity.toEntity
import dj.nomade.velox_restaurant.data.model.MenuItem
import dj.nomade.velox_restaurant.domain.repository.MenuRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val menuItemDao: MenuItemDao,
) : MenuRepository {

    // Collection Firestore en camelCase (anciennement "menu_items").
    // Les règles ne protègent que /menuItems ; toute autre casse → PERMISSION_DENIED.
    private val collection get() = firestore.collection("menuItems")

    override fun streamMenuItems(restaurantId: String): Flow<List<MenuItem>> = callbackFlow {
        val listener = collection
            .whereEqualTo("restaurantId", restaurantId)
            .orderBy("category")
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull {
                    runCatching { it.toMenuItem() }.getOrNull()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    override suspend fun addMenuItem(item: MenuItem): String {
        val data = item.toFirestoreMap() + mapOf("createdAt" to FieldValue.serverTimestamp())
        // L'ID du document fait foi (relu via doc.id dans toMenuItem()).
        // NE PAS écrire un champ "id" : les règles menuItems/update ne
        // l'autorisent pas (onlyFields) → PERMISSION_DENIED sur la création.
        val ref = collection.add(data).await()
        return ref.id
    }

    override suspend fun updateMenuItem(item: MenuItem) {
        withRetry {
            collection.document(item.id).update(
                item.toFirestoreMap() + mapOf("updatedAt" to FieldValue.serverTimestamp())
            ).await()
        }
    }

    override suspend fun deleteMenuItem(itemId: String) {
        withRetry {
            collection.document(itemId).delete().await()
        }
    }

    override suspend fun toggleItemAvailability(itemId: String, isAvailable: Boolean) {
        withRetry {
            collection.document(itemId).update(
                mapOf(
                    "isAvailable" to isAvailable,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
    }

    override suspend fun updateItemImage(itemId: String, imageUrl: String) {
        withRetry {
            collection.document(itemId).update(
                mapOf(
                    "imageUrl" to imageUrl,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
    }

    override suspend fun uploadMenuImage(restaurantId: String, itemId: String, imageUri: Uri): String {
        require(context.isValidImageUri(imageUri)) { "Image invalide ou corrompue" }
        val timestamp = System.currentTimeMillis()
        val ref = storage.reference
            .child("restaurants/$restaurantId/menu/${itemId}_$timestamp.jpg")
        val bytes = context.compressImageUri(imageUri, maxDimension = 800, quality = 85)
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    // ── Cache Room ────────────────────────────────────────────────────────

    override suspend fun cacheMenuItems(items: List<MenuItem>) {
        if (items.isEmpty()) return
        menuItemDao.clearMenuItems(items.first().restaurantId)
        menuItemDao.insertMenuItems(items.map { it.toEntity() })
    }

    override fun streamCachedMenuItems(restaurantId: String): Flow<List<MenuItem>> =
        menuItemDao.streamMenuItems(restaurantId).map { entities ->
            entities.map { it.toDomain() }
        }.distinctUntilChanged()

    // ── Mapper domain → Firestore ──────────────────────────────────────────

    private fun MenuItem.toFirestoreMap(): Map<String, Any?> = mapOf(
        "restaurantId" to restaurantId,
        "name" to name,
        "description" to description,
        "price" to price,
        "imageUrl" to imageUrl,
        "category" to category,
        "isAvailable" to isAvailable,
        "preparationTime" to preparationTime,
    )
}
