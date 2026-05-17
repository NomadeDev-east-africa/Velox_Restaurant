package com.example.velox_restaurant.core.di

import com.example.velox_restaurant.data.remote.AuthRepositoryImpl
import com.example.velox_restaurant.data.remote.CategoryRepositoryImpl
import com.example.velox_restaurant.data.remote.MenuRepositoryImpl
import com.example.velox_restaurant.data.remote.OrderRepositoryImpl
import com.example.velox_restaurant.data.remote.PromotionRepositoryImpl
import com.example.velox_restaurant.data.remote.RestaurantRepositoryImpl
import com.example.velox_restaurant.domain.repository.AuthRepository
import com.example.velox_restaurant.domain.repository.CategoryRepository
import com.example.velox_restaurant.domain.repository.MenuRepository
import com.example.velox_restaurant.domain.repository.OrderRepository
import com.example.velox_restaurant.domain.repository.PromotionRepository
import com.example.velox_restaurant.domain.repository.RestaurantRepository
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
