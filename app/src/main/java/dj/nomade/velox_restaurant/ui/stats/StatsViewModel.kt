package dj.nomade.velox_restaurant.ui.stats

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dj.nomade.velox_restaurant.R
import dj.nomade.velox_restaurant.data.model.Order
import dj.nomade.velox_restaurant.data.model.OrderStatus
import dj.nomade.velox_restaurant.data.model.PeriodStats
import dj.nomade.velox_restaurant.domain.repository.AuthRepository
import dj.nomade.velox_restaurant.domain.repository.OrderRepository
import dj.nomade.velox_restaurant.domain.repository.RestaurantRepository
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

enum class StatsPeriod(@StringRes val labelRes: Int) {
    DAY(R.string.stats_period_day),
    WEEK(R.string.stats_period_week),
    MONTH(R.string.stats_period_month),
    YEAR(R.string.stats_period_year),
}

data class DayRevenue(val label: String, val amount: Double)

data class TopItem(val name: String, val count: Int)

/** Une ligne du tableau comptable annuel. */
data class MonthRow(
    val label: String,
    val monthIndex: Int,
    val stats: PeriodStats,
)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.DAY,
    val periodStats: PeriodStats = PeriodStats(),
    val monthlyRows: List<MonthRow> = emptyList(),
    val year: Int = 0,
    val totalOrders: Long = 0,
    val totalRevenue: Double = 0.0,
    val avgRating: Double = 0.0,
    val weeklyRevenue: List<DayRevenue> = emptyList(),
    val topItems: List<TopItem> = emptyList(),
    val isLoading: Boolean = true,
    val isPeriodLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val restaurantRepository: RestaurantRepository,
    private val authRepository: AuthRepository,
    private val crashlytics: FirebaseCrashlytics,
    @ApplicationContext private val context: Context,
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

    fun selectPeriod(period: StatsPeriod) {
        if (period == _uiState.value.period) return
        _uiState.update { it.copy(period = period) }
        loadPeriod(period)
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Graphique 7 jours + top 5 du mois — une requête de plage chacun,
            // au lieu des 7 requêtes journalières parallèles d'avant.
            runCatching {
                coroutineScope {
                    val weekly = async { buildWeeklyData() }
                    val top = async {
                        val (start, end) = rangeOf(StatsPeriod.MONTH)
                        val monthOrders = orderRepository.getOrdersInRange(restaurantId, start, end)
                        withContext(Dispatchers.Default) { computeTopItems(monthOrders) }
                    }
                    _uiState.update {
                        it.copy(weeklyRevenue = weekly.await(), topItems = top.await())
                    }
                }
            }.onFailure { e ->
                crashlytics.recordException(e)
                _uiState.update { it.copy(error = e.message) }
            }

            _uiState.update { it.copy(isLoading = false) }
            loadPeriod(_uiState.value.period)
        }
    }

    /**
     * Cumuls « depuis l'ouverture ». Rafraîchis à chaque chargement de période et
     * à chaque retour sur l'écran : le ViewModel survit aux changements d'onglet,
     * donc un chargement unique au démarrage laisserait des chiffres figés dès
     * qu'une commande est honorée pendant la session.
     */
    fun refreshTotals() {
        viewModelScope.launch {
            // Échec isolé : ne doit pas casser le reste de l'écran.
            runCatching {
                val (totalOrders, totalRevenue) = orderRepository.getTotalStats(restaurantId)
                _uiState.update { it.copy(totalOrders = totalOrders, totalRevenue = totalRevenue) }
            }.onFailure { crashlytics.recordException(it) }
        }
    }

    private fun loadPeriod(period: StatsPeriod) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPeriodLoading = true) }
            runCatching {
                val (start, end) = rangeOf(period)
                val stats = orderRepository.getPeriodStats(restaurantId, start, end)
                val rows = if (period == StatsPeriod.YEAR) buildMonthlyRows() else emptyList()
                _uiState.update {
                    it.copy(
                        periodStats = stats,
                        monthlyRows = rows,
                        year = Calendar.getInstance().get(Calendar.YEAR),
                    )
                }
            }.onFailure { e ->
                crashlytics.recordException(e)
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isPeriodLoading = false) }
            refreshTotals()
        }
    }

    /** Les 12 mois de l'année en cours, en parallèle. */
    private suspend fun buildMonthlyRows(): List<MonthRow> = coroutineScope {
        val labels = context.resources.getStringArray(R.array.stats_months)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        (0..currentMonth).map { month ->
            val start = calendarOf(year, month).timeInMillis
            val end = calendarOf(
                if (month == 11) year + 1 else year,
                if (month == 11) 0 else month + 1,
            ).timeInMillis
            async {
                MonthRow(
                    label = labels[month],
                    monthIndex = month,
                    stats = orderRepository.getPeriodStats(restaurantId, start, end),
                )
            }
        }.map { it.await() }
    }

    private suspend fun buildWeeklyData(): List<DayRevenue> {
        val days = context.resources.getStringArray(R.array.stats_weekdays_short)
        val startOfToday = startOfDayCalendar()
        val weekStart = (startOfToday.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val endMs = (startOfToday.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val orders = orderRepository.getOrdersInRange(
            restaurantId, weekStart.timeInMillis, endMs,
        )
        return withContext(Dispatchers.Default) {
            val completed = orders.filter { it.status == OrderStatus.COMPLETED }
            (0..6).map { offset ->
                val dayCal = (weekStart.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, offset)
                }
                val dayStart = dayCal.timeInMillis
                val dayEnd = (dayCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis
                DayRevenue(
                    label = days[dayCal.get(Calendar.DAY_OF_WEEK) - 1],
                    amount = completed
                        .filter { it.createdAt != null && it.createdAt in dayStart until dayEnd }
                        .sumOf { it.restaurantRevenue },
                )
            }
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

    // ── Bornes de période ─────────────────────────────────────────────────

    /** Retourne `[début, fin[` en millisecondes pour la période demandée. */
    private fun rangeOf(period: StatsPeriod): Pair<Long, Long> {
        val today = startOfDayCalendar()
        return when (period) {
            StatsPeriod.DAY -> {
                val end = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                today.timeInMillis to end.timeInMillis
            }
            StatsPeriod.WEEK -> {
                val start = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
                val end = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                start.timeInMillis to end.timeInMillis
            }
            StatsPeriod.MONTH -> {
                val cal = Calendar.getInstance()
                val start = calendarOf(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                start.timeInMillis to end.timeInMillis
            }
            StatsPeriod.YEAR -> {
                val year = Calendar.getInstance().get(Calendar.YEAR)
                calendarOf(year, 0).timeInMillis to calendarOf(year + 1, 0).timeInMillis
            }
        }
    }

    private fun startOfDayCalendar(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun calendarOf(year: Int, month: Int): Calendar = Calendar.getInstance().apply {
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
