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
# Compose runtime ne fait pas de reflexion : ses regles consumer (embarquees dans l'AAR)
# suffisent. Seul SnapshotStateList a besoin d'un keep explicite (etat interne accede
# par le runtime sans passer par l'API publique).
-keep class androidx.compose.runtime.snapshots.SnapshotStateList { *; }
-keepclassmembers class androidx.compose.runtime.snapshots.** { *; }
-keep class androidx.compose.ui.text.googlefonts.** { *; }

# ─── Firebase ─────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore : garder les data classes pour la sérialisation/désérialisation
-keep class dj.nomade.velox_restaurant.data.model.** { *; }
-keepclassmembers class dj.nomade.velox_restaurant.data.model.** {
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
# Room genere son code d'acces (KSP) a la compilation, pas de reflexion a l'execution ;
# ses regles consumer suffisent. On garde seulement nos classes annotees.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers @androidx.room.Entity class * { <fields>; }

# ─── Kotlinx Serialization ────────────────────────────────────────────────────
# Seules nos classes @Serializable ont besoin d'etre protegees (serializer genere
# accede aux champs par reflexion) ; le runtime kotlinx.serialization lui-meme
# n'en a pas besoin.
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# ─── Coil ─────────────────────────────────────────────────────────────────────
# Coil embarque ses propres regles consumer, pas de reflexion sur nos classes.
-dontwarn coil.**

# ─── WorkManager ──────────────────────────────────────────────────────────────
# WorkManager embarque ses propres regles consumer ; on garde juste nos Worker
# concrets, instancies par nom de classe via reflexion.
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
