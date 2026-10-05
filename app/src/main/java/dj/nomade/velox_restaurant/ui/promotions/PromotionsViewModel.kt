package dj.nomade.velox_restaurant.ui.promotions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dj.nomade.velox_restaurant.data.model.MenuItem
import dj.nomade.velox_restaurant.data.model.Promotion
import dj.nomade.velox_restaurant.domain.repository.AuthRepository
import dj.nomade.velox_restaurant.domain.repository.MenuRepository
import dj.nomade.velox_restaurant.domain.repository.PromotionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PromotionsUiState(
    val promotions: List<Promotion> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PromotionsViewModel @Inject constructor(
    private val promotionRepository: PromotionRepository,
    private val menuRepository: MenuRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromotionsUiState())
    val uiState: StateFlow<PromotionsUiState> = _uiState.asStateFlow()

    private val restaurantId = authRepository.currentUserId().orEmpty()

    init {
        viewModelScope.launch {
            promotionRepository.streamPromotions(restaurantId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, promotions = list) }
                }
        }
        viewModelScope.launch {
            menuRepository.streamMenuItems(restaurantId)
                .catch { }
                .collect { items -> _uiState.update { it.copy(menuItems = items) } }
        }
    }

    /** Une promotion cible toujours un plat précis (`type = "item"`). */
    fun addPromotion(
        targetName: String,
        discountPercent: Int,
        label: String,
        endDate: Long?,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val promo = Promotion(
                    restaurantId = restaurantId,
                    type = "item",
                    targetId = targetName.lowercase().replace(" ", "_"),
                    targetName = targetName,
                    discountPercent = discountPercent,
                    label = label,
                    startDate = System.currentTimeMillis(),
                    endDate = endDate,
                    isActive = true,
                )
                promotionRepository.addPromotion(promo)
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun togglePromotion(promoId: String, currentIsActive: Boolean) {
        viewModelScope.launch {
            runCatching { promotionRepository.togglePromotion(promoId, !currentIsActive) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deletePromotion(promoId: String) {
        viewModelScope.launch {
            runCatching { promotionRepository.deletePromotion(promoId) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
