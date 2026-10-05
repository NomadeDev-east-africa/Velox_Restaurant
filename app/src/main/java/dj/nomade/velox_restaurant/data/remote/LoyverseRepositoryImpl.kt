package dj.nomade.velox_restaurant.data.remote

import com.google.firebase.functions.FirebaseFunctions
import dj.nomade.velox_restaurant.data.model.LoyverseConnection
import dj.nomade.velox_restaurant.data.model.LoyverseStatus
import dj.nomade.velox_restaurant.domain.repository.LoyverseRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LoyverseRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
) : LoyverseRepository {

    override suspend fun getStatus(): Result<LoyverseConnection> = runCatching {
        val data = call("loyverseStatus")
        LoyverseConnection(
            status = LoyverseStatus.fromString(data["status"] as? String),
            businessName = data["businessName"] as? String,
        )
    }

    override suspend fun startConnection(): Result<String> = runCatching {
        call("loyverseStartOAuth")["url"] as? String
            ?: error("URL d'autorisation Loyverse absente")
    }

    override suspend fun disconnect(): Result<Unit> = runCatching {
        call("loyverseDisconnect")
        Unit
    }

    private suspend fun call(name: String): Map<String, Any?> {
        val result = functions.getHttpsCallable(name).call().await()
        @Suppress("UNCHECKED_CAST")
        return result.getData() as? Map<String, Any?> ?: emptyMap()
    }
}
