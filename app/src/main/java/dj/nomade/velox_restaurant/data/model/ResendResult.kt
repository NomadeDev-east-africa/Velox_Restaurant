package dj.nomade.velox_restaurant.data.model

/** Réponse de la Cloud Function `resendOrderToDrivers`. */
data class ResendResult(
    val success: Boolean,
    val driversNotified: Int,
    val message: String,
)
