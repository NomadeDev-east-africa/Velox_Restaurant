package dj.nomade.velox_restaurant.data.remote

import dj.nomade.velox_restaurant.data.model.Category
import dj.nomade.velox_restaurant.domain.repository.CategoryRepository
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
class CategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : CategoryRepository {

    companion object {
        private val DEFAULTS = listOf(
            "Entrées", "Pizzas", "Burgers", "Pâtes", "Salades", "Desserts", "Boissons"
        )
    }

    private fun col(restaurantId: String) =
        firestore.collection("restaurants").document(restaurantId).collection("categories")

    override fun streamCategories(restaurantId: String): Flow<List<Category>> = callbackFlow {
        val listener = col(restaurantId)
            .orderBy("isDefault", Query.Direction.DESCENDING)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.map { doc ->
                    Category(
                        id = doc.id,
                        restaurantId = restaurantId,
                        name = doc.getString("name") ?: "",
                        isDefault = doc.getBoolean("isDefault") ?: false,
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    override suspend fun ensureDefaultCategories(restaurantId: String) {
        val existing = col(restaurantId).limit(1).get().await()
        if (!existing.isEmpty) return
        val batch = firestore.batch()
        DEFAULTS.forEach { name ->
            val ref = col(restaurantId).document()
            batch.set(ref, mapOf(
                "name" to name,
                "isDefault" to true,
                "restaurantId" to restaurantId,
                "createdAt" to FieldValue.serverTimestamp(),
            ))
        }
        batch.commit().await()
    }

    override suspend fun addCategory(restaurantId: String, name: String): String {
        val ref = col(restaurantId).add(mapOf(
            "name" to name,
            "isDefault" to false,
            "restaurantId" to restaurantId,
            "createdAt" to FieldValue.serverTimestamp(),
        )).await()
        return ref.id
    }

    override suspend fun deleteCategory(restaurantId: String, categoryId: String) {
        col(restaurantId).document(categoryId).delete().await()
    }
}
