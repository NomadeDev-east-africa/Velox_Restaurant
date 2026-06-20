package dj.nomade.velox_restaurant.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String>
    suspend fun logout()
    fun currentUserId(): String?
    fun isLoggedIn(): Boolean
}
