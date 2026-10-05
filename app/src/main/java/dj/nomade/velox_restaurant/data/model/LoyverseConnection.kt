package dj.nomade.velox_restaurant.data.model

/**
 * État de la liaison du restaurant avec sa caisse Loyverse, tel que renvoyé par la
 * Cloud Function `loyverseStatus`. Les jetons OAuth restent côté serveur : l'app ne
 * voit jamais que ce résumé.
 */
data class LoyverseConnection(
    val status: LoyverseStatus,
    /** Nom du commerce côté Loyverse, connu une fois connecté. */
    val businessName: String? = null,
)

enum class LoyverseStatus {
    NOT_CONNECTED,
    CONNECTED,
    /** Le jeton n'a pas pu être rafraîchi (accès retiré côté Loyverse…) : reconnecter. */
    RECONNECT_REQUIRED;

    companion object {
        fun fromString(value: String?): LoyverseStatus = when (value) {
            "connected" -> CONNECTED
            "reconnect_required" -> RECONNECT_REQUIRED
            else -> NOT_CONNECTED
        }
    }
}
