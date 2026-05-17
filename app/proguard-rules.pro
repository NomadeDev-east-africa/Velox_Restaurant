# ─── Attributs de débogage ────────────────────────────────────────────────────
-keepattributes *Annotation*, Signature, Exception, InnerClasses, EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Kotlin ───────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Lazy { <fields>; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keep class kotlinx.coroutines.android.** { *; }

# ─── Compose / SnapshotStateList ──────────────────────────────────────────────
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.runtime.snapshots.SnapshotStateList { *; }
-keepclassmembers class androidx.compose.runtime.snapshots.** { *; }
-keep class androidx.compose.ui.text.googlefonts.** { *; }

# ─── Firebase ─────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore : garder les data classes pour la sérialisation/désérialisation
-keep class com.example.velox_restaurant.data.model.** { *; }
-keepclassmembers class com.example.velox_restaurant.data.model.** {
    <init>(...);
    <fields>;
}

# ─── Hilt / Dagger ────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.Module class * { *; }
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
}

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers @androidx.room.Entity class * { <fields>; }

# ─── Kotlinx Serialization ────────────────────────────────────────────────────
-keep class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# ─── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ─── WorkManager ──────────────────────────────────────────────────────────────
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
