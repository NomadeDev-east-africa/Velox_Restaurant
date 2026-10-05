package dj.nomade.velox_restaurant

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import dj.nomade.velox_restaurant.core.locale.AppLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class RestaurantApplication : Application(), ImageLoaderFactory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrapContext(base))
    }

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
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // "orders_channel" est l'ancien channel, créé sans son personnalisé. Le son d'un
        // NotificationChannel est immuable une fois créé sur l'appareil : le modifier ici
        // n'a aucun effet pour les utilisateurs qui ont déjà l'app. D'où le nouvel ID.
        manager.deleteNotificationChannel(LEGACY_ORDERS_CHANNEL_ID)

        val newOrderSound = Uri.parse("android.resource://$packageName/${R.raw.new_order}")
        val channel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            getString(R.string.notification_channel_orders_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_orders_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            enableLights(true)
            setSound(
                newOrderSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val LEGACY_ORDERS_CHANNEL_ID = "orders_channel"
    }
}