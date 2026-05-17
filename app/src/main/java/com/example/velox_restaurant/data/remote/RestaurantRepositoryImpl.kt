package com.example.velox_restaurant.data.remote

import android.content.Context
import android.net.Uri
import com.example.velox_restaurant.core.utils.compressImageUri
import com.example.velox_restaurant.core.utils.isValidImageUri
import com.example.velox_restaurant.core.utils.withRetry
import com.example.velox_restaurant.data.model.Restaurant
import com.example.velox_restaurant.domain.repository.RestaurantRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestaurantRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : RestaurantRepository {

    private val collection get() = firestore.collection("restaurants")

    override fun streamRestaurant(uid: String): Flow<Restaurant> = callbackFlow {
        val listener = collection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                snapshot?.takeIf { it.exists() }
                    ?.runCatching { toRestaurant() }
                    ?.getOrNull()
                    ?.let { trySend(it) }
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    override suspend fun getRestaurant(uid: String): Restaurant =
        collection.document(uid).get().await().toRestaurant()

    override suspend fun updateIsOpen(uid: String, isOpen: Boolean) {
        withRetry {
            collection.document(uid).update(
                mapOf(
                    "isOpen" to isOpen,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
    }

    override suspend fun updateFcmToken(uid: String, token: String) {
        withRetry {
            collection.document(uid).update(
                mapOf(
                    "fcmToken" to token,
                    "fcmTokenUpdatedAt" to Timestamp.now(),
                )
            ).await()
        }
    }

    override suspend fun updateProfile(
        uid: String,
        name: String,
        phone: String,
        address: String,
        description: String,
    ) {
        withRetry {
            collection.document(uid).update(
                mapOf(
                    "name" to name,
                    "phone" to phone,
                    "address" to address,
                    "description" to description,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
    }

    override suspend fun uploadProfileImage(uid: String, imageUri: Uri): String {
        require(context.isValidImageUri(imageUri)) { "Image invalide ou corrompue" }
        val timestamp = System.currentTimeMillis()
        val ref = storage.reference
            .child("restaurants/$uid/profile/restaurant_$timestamp.jpg")
        val bytes = context.compressImageUri(imageUri, maxDimension = 1080, quality = 85)
        ref.putBytes(bytes).await()
        val url = ref.downloadUrl.await().toString()
        withRetry {
            collection.document(uid).update(
                mapOf(
                    "imageUrl" to url,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
        return url
    }
}
