package com.example.velox_restaurant.ui.home

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.velox_restaurant.core.utils.rememberNetworkState
import com.example.velox_restaurant.ui.menu.MenuScreen
import com.example.velox_restaurant.ui.orders.OrdersScreen
import com.example.velox_restaurant.ui.orders.OrdersViewModel
import com.example.velox_restaurant.ui.profile.ProfileScreen
import com.example.velox_restaurant.ui.promotions.PromotionsScreen
import com.example.velox_restaurant.ui.stats.StatsScreen
import com.example.velox_restaurant.ui.theme.InterFontFamily
import com.example.velox_restaurant.ui.theme.NeonGreen
import com.example.velox_restaurant.ui.theme.OnNeonGreen
import com.example.velox_restaurant.ui.theme.SurfaceL1

@Composable
private fun TabContent(visible: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (visible) 1f else 0f)
            .then(if (!visible) Modifier.alpha(0f) else Modifier),
    ) {
        content()
    }
}

private data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val navItems = listOf(
    NavItem("ORDERS",   Icons.Filled.Receipt,     Icons.Outlined.Receipt),
    NavItem("MENU",     Icons.Filled.Restaurant,  Icons.Outlined.Restaurant),
    NavItem("PROMOS",   Icons.Filled.LocalOffer,  Icons.Outlined.LocalOffer),
    NavItem("STATS",    Icons.Filled.BarChart,    Icons.Outlined.BarChart),
    NavItem("PROFILE",  Icons.Filled.Person,      Icons.Outlined.Person),
)

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val ordersViewModel: OrdersViewModel = hiltViewModel()
    val ordersState by ordersViewModel.uiState.collectAsStateWithLifecycle()
    val pendingCount = ordersState.pendingOrders.size
    val isOnline by rememberNetworkState()

    // Composition paresseuse : un onglet n'est composé qu'à la première visite,
    // puis reste dans l'arbre pour préserver son état (ViewModel, scroll…)
    val composed = remember { mutableStateListOf(true, false, false, false, false) }
    LaunchedEffect(selectedTab) { composed[selectedTab] = true }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── Bandeau hors-ligne ───────────────────────────────────────────
        androidx.compose.animation.AnimatedVisibility(
            visible = !isOnline,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE65100))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Hors ligne — données en cache",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // ── Contenu écrans ───────────────────────────────────────────────
        // Composition paresseuse : composé à la 1re visite, puis gardé vivant.
        // Au démarrage seul l'onglet 0 est composé → 0 freeze au lancement.
        Box(Modifier.weight(1f)) {
            if (composed[0]) TabContent(selectedTab == 0) { OrdersScreen(viewModel = ordersViewModel) }
            if (composed[1]) TabContent(selectedTab == 1) { MenuScreen() }
            if (composed[2]) TabContent(selectedTab == 2) { PromotionsScreen() }
            if (composed[3]) TabContent(selectedTab == 3) { StatsScreen() }
            if (composed[4]) TabContent(selectedTab == 4) { ProfileScreen(onLogout = onLogout) }
        }

        // ── Bottom Navigation ────────────────────────────────────────────
        NavigationBar(
            containerColor = SurfaceL1,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            navItems.forEachIndexed { index, item ->
                val selected = selectedTab == index
                NavigationBarItem(
                    selected = selected,
                    onClick = { selectedTab = index },
                    icon = {
                        BadgedBox(
                            badge = {
                                // Badge commandes en attente sur l'onglet Orders
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = index == 0 && pendingCount > 0,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    Badge(
                                        containerColor = NeonGreen,
                                        contentColor = OnNeonGreen,
                                    ) {
                                        Text(
                                            pendingCount.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                    }
                                }
                            }
                        ) {
                            Box(
                                modifier = if (selected) Modifier
                                    .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                else Modifier,
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = if (selected) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontFamily = InterFontFamily,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            color = if (selected) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}
