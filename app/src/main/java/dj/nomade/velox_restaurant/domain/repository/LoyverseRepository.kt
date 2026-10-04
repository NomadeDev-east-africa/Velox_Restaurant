package dj.nomade.velox_restaurant.domain.repository

import dj.nomade.velox_restaurant.data.model.LoyverseConnection

/** Liaison avec la caisse Loyverse — tout passe par les Cloud Functions `loyverse*`. */
interface LoyverseRepository {
    suspend fun getStatus(): Result<LoyverseConnection>

    /** URL d'autorisation Loyverse à ouvrir dans le navigateur (valable 10 min). */
    suspend fun startConnection(): Result<String>

    suspend fun disconnect(): Result<Unit>
}
