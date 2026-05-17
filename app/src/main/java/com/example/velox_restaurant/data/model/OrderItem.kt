package com.example.velox_restaurant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
    val name: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "",
    val preparationTime: Int = 0,
    val sauces: List<String> = emptyList(),
    val extras: List<String> = emptyList(),
)
