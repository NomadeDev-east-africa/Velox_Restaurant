package com.example.velox_restaurant.data.remote

import com.example.velox_restaurant.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val crashlytics: FirebaseCrashlytics,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<String> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: error("UID null après connexion")

        // Règle critique : isActive doit être true
        val doc = firestore.collection("restaurants").document(uid).get().await()
        if (doc.getBoolean("isActive") != true) {
            auth.signOut()
            error("Compte désactivé. Contactez le support Nomade253.")
        }

        // Lie les futurs crash reports à ce restaurant
        crashlytics.setUserId(uid)
        crashlytics.setCustomKey("restaurant_email", email)

        // Sauvegarde du token FCM actuel — onNewToken ne se déclenche que si le token change
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            firestore.collection("restaurants").document(uid).update(
                mapOf(
                    "fcmToken" to token,
                    "fcmTokenUpdatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }

        uid
    }

    override suspend fun logout() {
        crashlytics.setUserId("")
        crashlytics.setCustomKey("restaurant_email", "")
        auth.signOut()
    }

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun isLoggedIn(): Boolean = auth.currentUser != null
}
