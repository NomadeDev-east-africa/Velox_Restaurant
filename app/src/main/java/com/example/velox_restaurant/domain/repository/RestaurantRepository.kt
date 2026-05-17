package com.example.velox_restaurant.domain.repository

import android.net.Uri
import com.example.velox_restaurant.data.model.Restaurant
import kotlinx.coroutines.flow.Flow

interface RestaurantRepository {
    fun streamRestaurant(uid: String): Flow<Restaurant>
    suspend fun getRestaurant(uid: String): Restaurant
    suspend fun updateIsOpen(uid: String, isOpen: Boolean)
    suspend fun updateFcmToken(uid: String, token: String)
    suspend fun updateProfile(
        uid: String,
        name: String,
        phone: String,
        address: String,
        description: String,
    )
    suspend fun uploadProfileImage(uid: String, imageUri: Uri): String
}
