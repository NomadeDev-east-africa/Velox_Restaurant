package com.example.velox_restaurant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.velox_restaurant.data.model.Order
import com.example.velox_restaurant.data.model.OrderItem
import com.example.velox_restaurant.data.model.OrderStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "active_orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val restaurantId: String,
    val restaurantName: String,
    val customerName: String,
    val customerPhone: String,
    val itemsJson: String,
    val deliveryFee: Double,
    val total: Double,
    val status: String,
    val paymentMethod: String,
    val deliveryAddress: String,
    val deliveryLat: Double?,
    val deliveryLng: Double?,
    val addressDetails: String,
    val deliveryDriverId: String?,
    val deliveryDriverName: String?,
    val estimatedPreparationTime: Int?,
    val acceptedAt: Long?,
    val readyAt: Long?,
    val createdAt: Long?,
    val updatedAt: Long?,
)

fun Order.toEntity(): OrderEntity = OrderEntity(
    id = id,
    userId = userId,
    restaurantId = restaurantId,
    restaurantName = restaurantName,
    customerName = customerName,
    customerPhone = customerPhone,
    itemsJson = Json.encodeToString(items),
    deliveryFee = deliveryFee,
    total = total,
    status = status.value,
    paymentMethod = paymentMethod,
    deliveryAddress = deliveryAddress,
    deliveryLat = deliveryLat,
    deliveryLng = deliveryLng,
    addressDetails = addressDetails,
    deliveryDriverId = deliveryDriverId,
    deliveryDriverName = deliveryDriverName,
    estimatedPreparationTime = estimatedPreparationTime,
    acceptedAt = acceptedAt,
    readyAt = readyAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun OrderEntity.toDomain(): Order {
    val items = runCatching {
        Json.decodeFromString<List<OrderItem>>(itemsJson)
    }.getOrDefault(emptyList())
    return Order(
        id = id,
        userId = userId,
        restaurantId = restaurantId,
        restaurantName = restaurantName,
        customerName = customerName,
        customerPhone = customerPhone,
        items = items,
        deliveryFee = deliveryFee,
        total = total,
        status = OrderStatus.fromString(status),
        paymentMethod = paymentMethod,
        deliveryAddress = deliveryAddress,
        deliveryLat = deliveryLat,
        deliveryLng = deliveryLng,
        addressDetails = addressDetails,
        deliveryDriverId = deliveryDriverId,
        deliveryDriverName = deliveryDriverName,
        estimatedPreparationTime = estimatedPreparationTime,
        acceptedAt = acceptedAt,
        readyAt = readyAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
