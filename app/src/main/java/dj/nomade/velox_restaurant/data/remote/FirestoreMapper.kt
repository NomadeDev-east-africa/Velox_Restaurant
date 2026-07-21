package dj.nomade.velox_restaurant.data.remote

import dj.nomade.velox_restaurant.data.model.MenuItem
import dj.nomade.velox_restaurant.data.model.Order
import dj.nomade.velox_restaurant.data.model.OrderItem
import dj.nomade.velox_restaurant.data.model.OrderStatus
import dj.nomade.velox_restaurant.data.model.Promotion
import dj.nomade.velox_restaurant.data.model.Restaurant
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint

fun DocumentSnapshot.toRestaurant(): Restaurant = Restaurant(
    id = id,
    name = getString("name") ?: "",
    email = getString("email") ?: "",
    phone = getString("phone") ?: "",
    address = getString("address") ?: "",
    imageUrl = getString("imageUrl") ?: "",
    description = getString("description") ?: "",
    isOpen = getBoolean("isOpen") ?: false,
    isActive = getBoolean("isActive") ?: true,
    rating = getDouble("rating") ?: 0.0,
    totalOrders = getLong("totalOrders") ?: 0L,
    totalRevenue = getDouble("totalRevenue") ?: 0.0,
    fcmToken = getString("fcmToken") ?: "",
    createdAt = getTimestamp("createdAt")?.toEpochMillis(),
    updatedAt = getTimestamp("updatedAt")?.toEpochMillis(),
)

fun DocumentSnapshot.toOrder(): Order {
    val rawItems = get("items")
    val items = when (rawItems) {
        is List<*> -> rawItems.mapNotNull { it.toOrderItem() }
        else -> emptyList()
    }
    val geoPoint = get("deliveryLocation") as? GeoPoint
    return Order(
        id = id,
        userId = getString("userId") ?: "",
        restaurantId = getString("restaurantId") ?: "",
        restaurantName = getString("restaurantName") ?: "",
        restaurantImageUrl = getString("restaurantImageUrl") ?: "",
        customerName = getString("customerName") ?: "",
        customerPhone = getString("customerPhone") ?: "",
        items = items,
        // Les commandes anciennes n'ont pas de `subtotal` : on le reconstitue
        // depuis total - deliveryFee pour ne pas fausser l'historique comptable.
        subtotal = getDouble("subtotal")
            ?: ((getDouble("total") ?: 0.0) - (getDouble("deliveryFee") ?: 0.0)),
        discount = getDouble("discount") ?: 0.0,
        deliveryFee = getDouble("deliveryFee") ?: 0.0,
        total = getDouble("total") ?: 0.0,
        status = OrderStatus.fromString(getString("status") ?: ""),
        paymentMethod = getString("paymentMethod") ?: "",
        deliveryAddress = getString("deliveryAddress") ?: "",
        deliveryLat = geoPoint?.latitude,
        deliveryLng = geoPoint?.longitude,
        addressDetails = getString("addressDetails") ?: "",
        deliveryDriverId = getString("deliveryDriverId"),
        deliveryDriverName = getString("deliveryDriverName"),
        estimatedPreparationTime = getLong("estimatedPreparationTime")?.toInt(),
        acceptedAt = getTimestamp("acceptedAt")?.toEpochMillis(),
        readyAt = getTimestamp("readyAt")?.toEpochMillis(),
        pickedUpAt = getTimestamp("pickedUpAt")?.toEpochMillis(),
        deliveredAt = getTimestamp("deliveredAt")?.toEpochMillis(),
        cancelledAt = getTimestamp("cancelledAt")?.toEpochMillis(),
        cancellationReason = getString("cancellationReason"),
        createdAt = getTimestamp("createdAt")?.toEpochMillis(),
        updatedAt = getTimestamp("updatedAt")?.toEpochMillis(),
    )
}

fun DocumentSnapshot.toMenuItem(): MenuItem = MenuItem(
    id = id,
    restaurantId = getString("restaurantId") ?: "",
    name = getString("name") ?: "",
    description = getString("description") ?: "",
    price = getDouble("price") ?: 0.0,
    imageUrl = getString("imageUrl"),
    category = getString("category") ?: "",
    isAvailable = getBoolean("isAvailable") ?: true,
    preparationTime = getLong("preparationTime")?.toInt() ?: 15,
    createdAt = getTimestamp("createdAt")?.toEpochMillis(),
    updatedAt = getTimestamp("updatedAt")?.toEpochMillis(),
)

fun DocumentSnapshot.toPromotion(): Promotion = Promotion(
    id = id,
    restaurantId = getString("restaurantId") ?: "",
    type = getString("type") ?: "",
    targetId = getString("targetId") ?: "",
    targetName = getString("targetName") ?: "",
    discountPercent = getLong("discountPercent")?.toInt() ?: 0,
    label = getString("label") ?: "",
    startDate = getTimestamp("startDate")?.toEpochMillis(),
    endDate = getTimestamp("endDate")?.toEpochMillis(),
    isActive = getBoolean("isActive") ?: true,
    createdAt = getTimestamp("createdAt")?.toEpochMillis(),
)

@Suppress("UNCHECKED_CAST")
private fun Any?.toOrderItem(): OrderItem? {
    val map = this as? Map<String, Any> ?: return null

    fun extractStringList(key: String): List<String> {
        val raw = map[key] as? List<*> ?: return emptyList()
        return raw.mapNotNull { entry ->
            when (entry) {
                is String -> entry
                is Map<*, *> -> entry["name"] as? String
                else -> null
            }
        }
    }

    return OrderItem(
        name = map["name"] as? String ?: return null,
        quantity = (map["quantity"] as? Long)?.toInt() ?: 1,
        price = (map["price"] as? Double) ?: ((map["price"] as? Long)?.toDouble()) ?: 0.0,
        imageUrl = map["imageUrl"] as? String ?: "",
        category = map["category"] as? String ?: "",
        preparationTime = (map["preparationTime"] as? Long)?.toInt() ?: 0,
        sauces = extractStringList("sauces"),
        extras = extractStringList("extras"),
    )
}

private fun Timestamp.toEpochMillis(): Long = seconds * 1000 + nanoseconds / 1_000_000
