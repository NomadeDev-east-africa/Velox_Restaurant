package dj.nomade.velox_restaurant

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class RestaurantApplication : Application(), ImageLoaderFactory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.30)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        .crossfade(true)
        .build()

    override fun onCreate() {
        super.onCreate()
        // App Check — silences "No AppCheckProvider installed" warning.
        // PlayIntegrity vérifie l'intégrité de l'appareil sans bloquer l'accès
        // tant que l'enforcement n'est PAS activé dans Firebase Console.
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        // Pré-chauffe les singletons Firebase sur un thread IO pour réduire
        // le jank au premier frame (évite l'initialisation sur le main thread).
        appScope.launch {
            FirebaseFirestore.getInstance()
            FirebaseAuth.getInstance()
        }
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ORDERS_CHANNEL_ID,
            "Nouvelles commandes",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications pour les nouvelles commandes client"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ORDERS_CHANNEL_ID = "orders_channel"
    }
}