package dj.nomade.velox_restaurant.core.di

import dj.nomade.velox_restaurant.data.remote.AuthRepositoryImpl
import dj.nomade.velox_restaurant.data.remote.CategoryRepositoryImpl
import dj.nomade.velox_restaurant.data.remote.MenuRepositoryImpl
import dj.nomade.velox_restaurant.data.remote.OrderRepositoryImpl
import dj.nomade.velox_restaurant.data.remote.PromotionRepositoryImpl
import dj.nomade.velox_restaurant.data.remote.RestaurantRepositoryImpl
import dj.nomade.velox_restaurant.domain.repository.AuthRepository
import dj.nomade.velox_restaurant.domain.repository.CategoryRepository
import dj.nomade.velox_restaurant.domain.repository.MenuRepository
import dj.nomade.velox_restaurant.domain.repository.OrderRepository
import dj.nomade.velox_restaurant.domain.repository.PromotionRepository
import dj.nomade.velox_restaurant.domain.repository.RestaurantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindRestaurantRepository(impl: RestaurantRepositoryImpl): RestaurantRepository

    @Binds @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds @Singleton
    abstract fun bindMenuRepository(impl: MenuRepositoryImpl): MenuRepository

    @Binds @Singleton
    abstract fun bindPromotionRepository(impl: PromotionRepositoryImpl): PromotionRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
}
