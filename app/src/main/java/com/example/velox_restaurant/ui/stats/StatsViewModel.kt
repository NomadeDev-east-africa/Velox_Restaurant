package com.example.velox_restaurant.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velox_restaurant.data.model.Order
import com.example.velox_restaurant.data.model.OrderStatus
import com.example.velox_restaurant.domain.repository.AuthRepository
import com.example.velox_restaurant.domain.repository.OrderRepository
import com.example.velox_restaurant.domain.repository.RestaurantRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DayRevenue(val label: String, val amount: Double)

data class TopItem(val name: String, val count: Int)

data class StatsUiState(
    val todayOrders: Int = 0,
    val todayRevenue: Double = 0.0,
    val totalOrders: Long = 0,
    val totalRevenue: Double = 0.0,
    val avgRating: Double = 0.0,
    val weeklyRevenue: List<DayRevenue> = emptyList(),
    val topItems: List<TopItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val restaurantRepository: RestaurantRepository,
    private val authRepository: AuthRepository,
    private val crashlytics: FirebaseCrashlytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val restaurantId = authRepository.currentUserId().orEmpty()

    init {
        viewModelScope.launch {
            restaurantRepository.streamRestaurant(restaurantId)
                .catch { }
                .collect { restaurant ->
                    _uiState.update { it.copy(avgRating = restaurant.rating) }
                }
        }
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Graphique, top 5, stats du jour — bloc principal
            runCatching {
                val startOfDay = startOfDay()
                val todayOrders = orderRepository.getOrdersForDay(restaurantId, startOfDay)
                val weeklyData = buildWeeklyData()
                val startOfMonth = startOfMonth()
                val monthOrders = orderRepository.getOrdersForMonth(restaurantId, startOfMonth)
                // Filtrage et agrégation déplacés sur Default pour ne pas bloquer le main thread
                val (completed, topItems) = withContext(Dispatchers.Default) {
                    val done = todayOrders.filter { it.status == OrderStatus.COMPLETED }
                    done to computeTopItems(monthOrders)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        todayOrders = todayOrders.size,
                        todayRevenue = completed.sumOf { o -> o.total },
                        weeklyRevenue = weeklyData,
                        topItems = topItems,
                    )
                }
            }.onFailure { e ->
                crashlytics.recordException(e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }

            // Totaux all-time — échec isolé, ne casse pas le reste
            runCatching {
                val (totalOrders, totalRevenue) = orderRepository.getTotalStats(restaurantId)
                _uiState.update { it.copy(totalOrders = totalOrders, totalRevenue = totalRevenue) }
            }
        }
    }

    private suspend fun buildWeeklyData(): List<DayRevenue> {
        val days = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return coroutineScope {
            (6 downTo 0).map { daysBack ->
                val dayCal = (cal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -daysBack)
                }
                val startMs = dayCal.timeInMillis
                val dayLabel = days[dayCal.get(Calendar.DAY_OF_WEEK) - 1]
                async {
                    val orders = orderRepository.getOrdersForDay(restaurantId, startMs)
                    val revenue = orders.filter { it.status == OrderStatus.COMPLETED }.sumOf { it.total }
                    DayRevenue(dayLabel, revenue)
                }
            }.map { it.await() }
        }
    }

    private fun computeTopItems(orders: List<Order>): List<TopItem> {
        val countMap = mutableMapOf<String, Int>()
        orders.filter { it.status == OrderStatus.COMPLETED }.forEach { order ->
            order.items.forEach { item ->
                countMap[item.name] = (countMap[item.name] ?: 0) + item.quantity
            }
        }
        return countMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { TopItem(it.key, it.value) }
    }

    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
