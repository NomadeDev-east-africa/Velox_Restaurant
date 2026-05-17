package com.example.velox_restaurant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Restaurant(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val isOpen: Boolean = false,
    val isActive: Boolean = true,
    val rating: Double = 0.0,
    val totalOrders: Long = 0L,
    val totalRevenue: Double = 0.0,
    val fcmToken: String = "",
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)
