package dj.nomade.velox_restaurant.ui.orders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dj.nomade.velox_restaurant.R
import dj.nomade.velox_restaurant.data.model.Order
import dj.nomade.velox_restaurant.data.model.OrderStatus
import dj.nomade.velox_restaurant.domain.repository.AuthRepository
import dj.nomade.velox_restaurant.domain.repository.OrderRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersUiState(
    val pendingOrders: List<Order> = emptyList(),
    val activeOrders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    /** Id de la commande dont la relance vers les livreurs est en cours d'envoi. */
    val resendingOrderId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val crashlytics: FirebaseCrashlytics,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val restaurantId = authRepository.currentUserId().orEmpty()

    /** Dernier instantané brut reçu, pour ré-appliquer le filtre d'ancienneté sans attendre une nouvelle émission Firestore. */
    private var lastOrders: List<Order> = emptyList()

    init {
        loadCacheThenStream()
        // Une commande "en cours" (ex. ready sans livreur) peut rester bloquée
        // des jours sans qu'aucune écriture Firestore ne survienne entre-temps :
        // sans ce tick, le filtre de 24h ne se ré-évaluerait jamais tout seul.
        viewModelScope.launch {
            while (true) {
                delay(15 * 60 * 1_000L)
                splitAndUpdate(lastOrders, isLoading = _uiState.value.isLoading)
            }
        }
    }

    private fun loadCacheThenStream() {
        // 1. Cache Room — annulé dès que Firestore émet pour la première fois
        val cacheJob = viewModelScope.launch {
            orderRepository.streamCachedActiveOrders(restaurantId).collect { cached ->
                splitAndUpdate(cached)
            }
        }
        // 2. Stream Firestore temps réel — prend le relais et annule le cache job
        viewModelScope.launch {
            orderRepository.streamActiveOrders(restaurantId)
                .catch { e ->
                    crashlytics.recordException(e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { orders ->
                    cacheJob.cancel()
                    orderRepository.cacheActiveOrders(orders)
                    splitAndUpdate(orders, isLoading = false)
                }
        }
    }

    private fun splitAndUpdate(orders: List<Order>, isLoading: Boolean = false) {
        lastOrders = orders
        // Masquage d'affichage uniquement : le statut Firestore de la commande ne
        // bouge pas, elle disparaît juste de l'écran "En cours" au bout de 24h pour
        // ne pas laisser une commande bloquée (ex. ready sans livreur) polluer la
        // liste indéfiniment. Les commandes "Nouvelles" (jamais acceptées) restent
        // toujours visibles : les cacher serait dangereux, un client attend encore.
        val staleCutoffMs = System.currentTimeMillis() - STALE_ACTIVE_ORDER_MAX_AGE_MS
        _uiState.update {
            it.copy(
                isLoading = isLoading,
                pendingOrders = orders.filter { o -> o.status == OrderStatus.PENDING },
                activeOrders = orders.filter { o ->
                    o.status != OrderStatus.PENDING &&
                        (o.createdAt == null || o.createdAt >= staleCutoffMs)
                },
                error = null,
            )
        }
    }

    companion object {
        private const val STALE_ACTIVE_ORDER_MAX_AGE_MS = 24L * 60 * 60 * 1_000L
    }

    fun acceptOrder(orderId: String, prepTimeMinutes: Int = 20) {
        viewModelScope.launch {
            runCatching { orderRepository.acceptOrder(orderId, prepTimeMinutes) }
                .onFailure { e ->
                    crashlytics.log("acceptOrder failed — orderId=$orderId")
                    crashlytics.recordException(e)
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun startPreparation(orderId: String) {
        viewModelScope.launch {
            runCatching { orderRepository.startPreparation(orderId) }
                .onFailure { e ->
                    crashlytics.log("startPreparation failed — orderId=$orderId")
                    crashlytics.recordException(e)
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun markReady(orderId: String) {
        viewModelScope.launch {
            // 1. Passage du statut à "ready" (écriture Firestore).
            runCatching { orderRepository.markReady(orderId) }
                .onFailure { e ->
                    crashlytics.log("markReady failed — orderId=$orderId")
                    crashlytics.recordException(e)
                    _uiState.update { it.copy(error = e.message) }
                    return@launch
                }

            // 2. Déclenchement des notifications (Cloud Function). Le statut est
            //    déjà "ready" ; on distingue donc l'échec de notification pour
            //    que le restaurateur sache que le client n'a PAS été prévenu.
            orderRepository.sendReadyNotifications(orderId)
                .onFailure { e ->
                    crashlytics.log("sendReadyNotifications failed — orderId=$orderId")
                    crashlytics.recordException(e)
                    _uiState.update {
                        it.copy(error = context.getString(R.string.error_mark_ready_notification_failed))
                    }
                }
        }
    }

    /**
     * Relance l'offre vers les livreurs actuellement en ligne, sans repasser par
     * [markReady] : le statut, les stats restaurant et la notif client ne bougent pas.
     */
    fun resendToDrivers(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(resendingOrderId = orderId) }
            orderRepository.resendToDrivers(orderId)
                .onSuccess { result -> _uiState.update { it.copy(error = result.message) } }
                .onFailure { e ->
                    crashlytics.log("resendToDrivers failed — orderId=$orderId")
                    crashlytics.recordException(e)
                    _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_resend_failed)) }
                }
            _uiState.update { it.copy(resendingOrderId = null) }
        }
    }

    fun cancelOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            runCatching { orderRepository.cancelOrder(orderId, reason) }
                .onFailure { e ->
                    crashlytics.log("cancelOrder failed — orderId=$orderId")
                    crashlytics.recordException(e)
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
