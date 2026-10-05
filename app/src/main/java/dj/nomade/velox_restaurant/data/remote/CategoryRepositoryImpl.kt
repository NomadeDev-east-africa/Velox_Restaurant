package dj.nomade.velox_restaurant.data.remote

import dj.nomade.velox_restaurant.data.model.Category
import dj.nomade.velox_restaurant.domain.repository.CategoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : CategoryRepository {

    // Référentiel GLOBAL, à la racine — les documents n'ont pas de restaurantId.
    // L'ancienne sous-collection restaurants/{uid}/categories n'a jamais existé en
    // base : elle était remplie par un seeding de catégories en dur, ce qui faisait
    // afficher « Pizzas / Burgers / Pâtes » à des restaurants qui n'en vendent pas.
    private val collection get() = firestore.collection("menuCategories")

    override fun streamCategories(): Flow<List<Category>> = callbackFlow {
        val listener = collection
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.map { doc ->
                    Category(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        order = doc.getLong("order")?.toInt() ?: Int.MAX_VALUE,
                        imageUrl = doc.getString("imageUrl"),
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }
        // Ce référentiel ne sert qu'à ordonner les chips : s'il est illisible
        // (règles Firestore, hors ligne), on retombe sur l'ordre alphabétique
        // plutôt que de casser l'écran Menu.
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
}
