package com.example.velox_restaurant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Promotion(
    val id: String = "",
    val restaurantId: String = "",
    val type: String = "",         // "item" | "category"
    val targetId: String = "",
    val targetName: String = "",
    val discountPercent: Int = 0,  // 1-100
    val label: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long? = null,
) {
    val isExpired: Boolean get() {
        val end = endDate ?: return false
        return end < System.currentTimeMillis()
    }
}
