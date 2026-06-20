package dj.nomade.velox_restaurant.data.model

enum class OrderStatus(val value: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    PREPARING("preparing"),
    READY("ready"),
    DELIVERING("delivering"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        fun fromString(value: String): OrderStatus =
            entries.firstOrNull { it.value == value } ?: PENDING

        val active = setOf(PENDING, ACCEPTED, PREPARING, READY)
    }
}
