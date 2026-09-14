package dj.nomade.velox_restaurant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dj.nomade.velox_restaurant.data.local.dao.MenuItemDao
import dj.nomade.velox_restaurant.data.local.dao.OrderDao
import dj.nomade.velox_restaurant.data.local.entity.MenuItemEntity
import dj.nomade.velox_restaurant.data.local.entity.OrderEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [OrderEntity::class, MenuItemEntity::class],
    // v2 : ajout de subtotal/discount sur active_orders (CA hors livraison).
    // v3 : ajout de optionGroups sur menu_items (sauces & suppléments).
    version = 3,
    exportSchema = false,
)
abstract class RestaurantDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun menuItemDao(): MenuItemDao
}

/**
 * Migration explicite : le cache sert de mode dégradé hors ligne pour les commandes,
 * le vider en production est inacceptable. `'[]'` = aucun groupe d'options connu,
 * la valeur sera remplacée au premier snapshot Firestore.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE menu_items ADD COLUMN optionGroups TEXT NOT NULL DEFAULT '[]'")
    }
}
