package dj.nomade.velox_restaurant.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dj.nomade.velox_restaurant.data.local.RestaurantDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "restaurant_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RestaurantDatabase =
        Room.databaseBuilder(
            context,
            RestaurantDatabase::class.java,
            "restaurant_db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()

    @Provides
    fun provideOrderDao(db: RestaurantDatabase) = db.orderDao()

    @Provides
    fun provideMenuItemDao(db: RestaurantDatabase) = db.menuItemDao()
}
