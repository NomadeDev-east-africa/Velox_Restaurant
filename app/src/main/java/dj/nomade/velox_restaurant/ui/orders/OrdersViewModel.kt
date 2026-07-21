package dj.nomade.velox_restaurant.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dj.nomade.velox_restaurant.data.model.Order
import dj.nomade.velox_restaurant.data.model.OrderStatus
import dj.nomade.velox_restaurant.domain.repository.AuthRepository
import dj.nomade.velox_restaurant.domain.repository.OrderRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val error: String? = null,
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val crashlytics: FirebaseCrashlytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val restaurantId = authRepository.currentUserId().orEmpty()

    init {
        loadCacheThenStream()
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
        _uiState.update {
            it.copy(
                isLoading = isLoading,
                pendingOrders = orders.filter { o -> o.status == OrderStatus.PENDING },
                activeOrders = orders.filter { o -> o.status != OrderStatus.PENDING },
                error = null,
            )
        }
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
                        it.copy(error = "Commande marquée prête, mais l'envoi de la notification a échoué. Réessayez.")
                    }
                }
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
