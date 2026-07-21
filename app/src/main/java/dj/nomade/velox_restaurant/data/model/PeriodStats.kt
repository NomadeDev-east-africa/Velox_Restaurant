package dj.nomade.velox_restaurant.data.model

/**
 * Agrégat comptable sur un intervalle de temps.
 *
 * [revenue] est le chiffre d'affaires du restaurant, frais de livraison déduits.
 */
data class PeriodStats(
    val orderCount: Long = 0,
    val completedCount: Long = 0,
    val cancelledCount: Long = 0,
    val revenue: Double = 0.0,
    val deliveryFees: Double = 0.0,
) {
    /** Panier moyen sur les seules commandes honorées. */
    val averageBasket: Double
        get() = if (completedCount > 0) revenue / completedCount else 0.0

    /** Part des commandes annulées, en pourcentage. */
    val cancellationRate: Double
        get() = if (orderCount > 0) cancelledCount * 100.0 / orderCount else 0.0
}
