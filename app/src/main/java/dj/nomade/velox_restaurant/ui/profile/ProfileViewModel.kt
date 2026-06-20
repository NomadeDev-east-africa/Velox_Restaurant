package dj.nomade.velox_restaurant.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dj.nomade.velox_restaurant.data.model.Restaurant
import dj.nomade.velox_restaurant.domain.repository.AuthRepository
import dj.nomade.velox_restaurant.domain.repository.OrderRepository
import dj.nomade.velox_restaurant.domain.repository.RestaurantRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val restaurant: Restaurant? = null,
    val totalOrders: Long = 0L,
    val totalRevenue: Double = 0.0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val loggedOut: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val crashlytics: FirebaseCrashlytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val uid = authRepository.currentUserId().orEmpty()

    init {
        viewModelScope.launch {
            restaurantRepository.streamRestaurant(uid)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { restaurant ->
                    _uiState.update { it.copy(isLoading = false, restaurant = restaurant) }
                }
        }
        loadTotalStats()
    }

    fun loadTotalStats() {
        viewModelScope.launch {
            runCatching { orderRepository.getTotalStats(uid) }
                .onSuccess { (orders, revenue) ->
                    _uiState.update { it.copy(totalOrders = orders, totalRevenue = revenue) }
                }
        }
    }

    fun toggleIsOpen() {
        val current = _uiState.value.restaurant?.isOpen ?: return
        viewModelScope.launch {
            runCatching { restaurantRepository.updateIsOpen(uid, !current) }
                .onFailure { e ->
                    crashlytics.recordException(e)
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun updateProfile(name: String, phone: String, address: String, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                restaurantRepository.updateProfile(uid, name, phone, address, description)
            }.onFailure { e ->
                crashlytics.log("updateProfile failed — uid=$uid")
                crashlytics.recordException(e)
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun uploadProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching { restaurantRepository.uploadProfileImage(uid, imageUri) }
                .onFailure { e ->
                    crashlytics.log("uploadProfileImage failed — uid=$uid")
                    crashlytics.recordException(e)
                    _uiState.update { it.copy(error = e.message) }
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
