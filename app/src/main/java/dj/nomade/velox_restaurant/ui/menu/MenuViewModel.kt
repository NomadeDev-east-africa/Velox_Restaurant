package dj.nomade.velox_restaurant.ui.menu

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val items: List<MenuItem> = emptyList(),
    val activePromotions: List<Promotion> = emptyList(),
    val selectedCategory: String = MenuViewModel.ALL,
    val isLoading: Boolean = true,
    val uploadingImageFor: String? = null,
    val error: String? = null,
)

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val categoryRepository: CategoryRepository,
    private val promotionRepository: PromotionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    companion object {
        const val ALL = "Toutes"
        const val SAUCES = "Sauces"
        const val SUPPLEMENTS = "Suppléments"

        /**
         * Les deux seules catégories toujours proposées, même vides : le restaurateur
         * doit pouvoir créer sa première sauce, et il faut donc un onglet où le faire.
         * Ce sont aussi les seules où l'ajout/suppression d'entrées est autorisé.
         */
        val EDITABLE = listOf(SAUCES, SUPPLEMENTS)
    }

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private val restaurantId = authRepository.currentUserId().orEmpty()

    /** Ordre officiel des noms de catégorie, issu du référentiel global. */
    private val categoryOrder = MutableStateFlow<Map<String, Int>>(emptyMap())

    /**
     * Chips affichées : uniquement les catégories que ce restaurant utilise
     * réellement, plus Sauces/Suppléments. Aucune liste en dur.
     */
    val categories: StateFlow<List<String>> =
        combine(_uiState.map { it.items }, categoryOrder) { items, order ->
            val used = items.map { it.category }.filter { it.isNotBlank() }
            (used + EDITABLE)
                .distinct()
                .sortedWith(compareBy({ order[it] ?: Int.MAX_VALUE }, { it }))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val filteredItems: StateFlow<List<MenuItem>> = _uiState
        .map { state ->
            val cat = state.selectedCategory
            if (cat == ALL) state.items
            else state.items.filter { it.category == cat }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        viewModelScope.launch {
            categoryRepository.streamCategories()
                .collect { cats -> categoryOrder.value = cats.associate { it.name to it.order } }
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

    /** Création d'une sauce ou d'un supplément — nom + prix, rien d'autre. */
    fun addExtra(name: String, price: Double, category: String) {
        if (category !in EDITABLE) return
        if (price <= 0) {
            _uiState.update { it.copy(error = "Le prix doit être supérieur à 0 FDJ.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                menuRepository.addMenuItem(
                    MenuItem(
                        restaurantId = restaurantId,
                        name = name.trim(),
                        price = price,
                        category = category,
                        preparationTime = 0,
                    )
                )
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deleteExtra(item: MenuItem) {
        if (item.category !in EDITABLE) return
        viewModelScope.launch {
            runCatching { menuRepository.deleteMenuItem(item.id) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    /** Remplace uniquement la photo du plat : ni le nom, ni le prix ne bougent. */
    fun changeItemImage(item: MenuItem, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingImageFor = item.id) }
            runCatching {
                val url = menuRepository.uploadMenuImage(restaurantId, item.id, imageUri)
                menuRepository.updateItemImage(item.id, url)
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(uploadingImageFor = null) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
