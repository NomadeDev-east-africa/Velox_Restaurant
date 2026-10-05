package dj.nomade.velox_restaurant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String = "",
    val userId: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantImageUrl: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    // items est List<Map> dans Firestore — désérialisé manuellement dans le repository
    val items: List<OrderItem> = emptyList(),
    /** Montant des plats seuls — c'est ce que le restaurant encaisse. */
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    /** Revient au livreur / à la plateforme, jamais au restaurant. */
    val deliveryFee: Double = 0.0,
    /** `subtotal + deliveryFee` — montant payé par le client, PAS le CA du restaurant. */
    val total: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentMethod: String = "",
    val deliveryAddress: String = "",
    val deliveryLat: Double? = null,
    val deliveryLng: Double? = null,
    val addressDetails: String = "",
    val deliveryDriverId: String? = null,
    val deliveryDriverName: String? = null,
    val estimatedPreparationTime: Int? = null,
    val acceptedAt: Long? = null,
    val readyAt: Long? = null,
    val pickedUpAt: Long? = null,
    val deliveredAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    /** Dernière relance de notification vers les livreurs (bouton "Relancer"). */
    val lastResendAt: Long? = null,
    val resendCount: Int = 0,
) {
    val isActive: Boolean get() = status in OrderStatus.active
    val itemCount: Int get() = items.sumOf { it.quantity }

    /**
     * Chiffre d'affaires réel du restaurant, frais de livraison exclus.
     * À utiliser partout pour la comptabilité — jamais [total], qui inclut
     * les [deliveryFee] reversés au livreur.
     */
    val restaurantRevenue: Double get() = subtotal
}
