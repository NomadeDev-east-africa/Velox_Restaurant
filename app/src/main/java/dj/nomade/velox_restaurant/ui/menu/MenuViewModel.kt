package dj.nomade.velox_restaurant.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dj.nomade.velox_restaurant.data.model.Category
import dj.nomade.velox_restaurant.data.model.MenuItem
import dj.nomade.velox_restaurant.data.model.Promotion
import dj.nomade.velox_restaurant.domain.repository.AuthRepository
import dj.nomade.velox_restaurant.domain.repository.CategoryRepository
import dj.nomade.velox_restaurant.domain.repository.MenuRepository
import dj.nomade.velox_restaurant.domain.repository.PromotionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val items: List<MenuItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val activePromotions: List<Promotion> = emptyList(),
    val selectedCategory: String = "Toutes",
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val categoryRepository: CategoryRepository,
    private val promotionRepository: PromotionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private val restaurantId = authRepository.currentUserId().orEmpty()

    val filteredItems: StateFlow<List<MenuItem>> = _uiState
        .map { state ->
            val cat = state.selectedCategory
            if (cat == "Toutes") state.items
            else state.items.filter { it.category == cat }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        viewModelScope.launch {
            runCatching { categoryRepository.ensureDefaultCategories(restaurantId) }
        }
        viewModelScope.launch {
            categoryRepository.streamCategories(restaurantId)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { cats -> _uiState.update { it.copy(categories = cats) } }
        }
        viewModelScope.launch {
            menuRepository.streamCachedMenuItems(restaurantId).collect { cached ->
                if (_uiState.value.isLoading) _uiState.update { it.copy(items = cached) }
            }
        }
        viewModelScope.launch {
            menuRepository.streamMenuItems(restaurantId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { items ->
                    menuRepository.cacheMenuItems(items)
                    _uiState.update { it.copy(isLoading = false, items = items) }
                }
        }
        viewModelScope.launch {
            promotionRepository.streamPromotions(restaurantId)
                .catch { }
                .collect { promos ->
                    _uiState.update {
                        it.copy(activePromotions = promos.filter { p -> p.isActive && !p.isExpired })
                    }
                }
        }
    }

    fun selectCategory(category: String) = _uiState.update { it.copy(selectedCategory = category) }

    fun toggleAvailability(item: MenuItem) {
        viewModelScope.launch {
            runCatching { menuRepository.toggleItemAvailability(item.id, !item.isAvailable) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
