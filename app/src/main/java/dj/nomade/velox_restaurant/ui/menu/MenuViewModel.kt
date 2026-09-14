package dj.nomade.velox_restaurant.ui.menu

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dj.nomade.velox_restaurant.data.model.MenuItem
import dj.nomade.velox_restaurant.data.model.OptionChoice
import dj.nomade.velox_restaurant.data.model.Promotion
import dj.nomade.velox_restaurant.data.model.normalizeOptionName
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
    /** Clé (nom normalisé) du choix dont la rupture est en cours d'écriture. */
    val togglingChoice: String? = null,
    val error: String? = null,
)

/**
 * Un choix d'option agrégé sur tout le menu du restaurant. Chaque plat porte sa
 * propre copie des mêmes choix : « Sauce roquefort » existe 15 fois chez Davido,
 * on n'en affiche qu'une ligne portant le nombre de plats concernés.
 */
data class ExtraOption(
    val group: String,
    val name: String,
    val price: Double,
    val itemCount: Int,
    /** `false` dès qu'au moins un plat marque ce choix indisponible. */
    val available: Boolean,
    /** `false` si l'état diffère d'un plat à l'autre. */
    val isUniform: Boolean,
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
        const val LEGUMES = "Légumes"

        /**
         * Sections d'options, distinctes des catégories de plats : elles ne listent
         * pas des documents `menuItems` mais les choix embarqués dans leur champ
         * `optionGroups`. Une section n'apparaît que si le menu la contient vraiment.
         */
        val OPTION_SECTIONS = listOf(SAUCES, SUPPLEMENTS, LEGUMES)

        /**
         * Le rangement se fait sur le nom du **groupe**, jamais sur celui du choix :
         * « Sauce roquefort » appartient au groupe « Suppléments » et doit donc
         * s'afficher dans Suppléments.
         *
         * L'ordre des tests reproduit `isSauceGroup()` de l'app client
         * (`option_selection.dart`) : un groupe contenant « sauce » part en sauces.
         */
        fun sectionOf(groupName: String): String? {
            val n = groupName.normalizeOptionName()
            return when {
                n.contains("sauce") -> SAUCES
                n.contains("supplement") || n.contains("extra") -> SUPPLEMENTS
                n.contains("legume") -> LEGUMES
                else -> null
            }
        }
    }

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private val restaurantId = authRepository.currentUserId().orEmpty()

    /** Ordre officiel des noms de catégorie, issu du référentiel global. */
    private val categoryOrder = MutableStateFlow<Map<String, Int>>(emptyMap())

    /**
     * Chips affichées : les catégories que ce restaurant utilise réellement, suivies
     * des sections d'options qu'il propose réellement. Aucune liste en dur.
     */
    val categories: StateFlow<List<String>> =
        combine(_uiState.map { it.items }, categoryOrder) { items, order ->
            val used = items.map { it.category }
                .filter { it.isNotBlank() }
                .distinct()
                .sortedWith(compareBy({ order[it] ?: Int.MAX_VALUE }, { it }))
            val sections = OPTION_SECTIONS.filter { section ->
                items.any { item -> item.optionGroups.any { sectionOf(it.name) == section } }
            }
            used + sections
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val filteredItems: StateFlow<List<MenuItem>> = _uiState
        .map { state ->
            val cat = state.selectedCategory
            when {
                cat == ALL -> state.items
                cat in OPTION_SECTIONS -> emptyList()
                else -> state.items.filter { it.category == cat }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Choix de la section sélectionnée, dédupliqués sur (groupe, nom, prix). */
    val sectionOptions: StateFlow<List<ExtraOption>> = _uiState
        .map { state ->
            val section = state.selectedCategory
            if (section !in OPTION_SECTIONS) return@map emptyList()
            state.items
                .flatMap { item ->
                    item.optionGroups
                        .filter { sectionOf(it.name) == section }
                        .flatMap { group -> group.choices.map { group.name to it } }
                }
                .groupBy { (group, choice) ->
                    Triple(group, choice.name.normalizeOptionName(), choice.price)
                }
                .map { (_, entries) -> entries.toExtraOption() }
                .sortedWith(compareBy({ it.group }, { it.name }))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private fun List<Pair<String, OptionChoice>>.toExtraOption(): ExtraOption {
        val (group, first) = first()
        return ExtraOption(
            group = group,
            name = first.name,
            price = first.price,
            itemCount = size,
            available = all { it.second.available },
            isUniform = all { it.second.available == first.available },
        )
    }

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

    /**
     * Met une sauce/supplément en rupture (ou la remet en stock) sur tout le menu.
     * L'écran se rafraîchit tout seul : le snapshot Firestore relit `available`.
     */
    fun setExtraAvailability(option: ExtraOption, available: Boolean) {
        val key = option.name.normalizeOptionName()
        viewModelScope.launch {
            _uiState.update { it.copy(togglingChoice = key) }
            runCatching { menuRepository.setChoiceAvailability(restaurantId, option.name, available) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(togglingChoice = null) }
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
