package com.example.velox_restaurant.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.velox_restaurant.domain.repository.AuthRepository
import com.example.velox_restaurant.ui.auth.LoginScreen
import com.example.velox_restaurant.ui.home.HomeScreen

@Composable
fun AppNavHost(
    authRepository: AuthRepository,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination: AppDestination =
        if (authRepository.isLoggedIn()) AppDestination.Home else AppDestination.Login

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<AppDestination.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppDestination.Home) {
                        popUpTo<AppDestination.Login> { inclusive = true }
                    }
                }
            )
        }

        composable<AppDestination.Home> {
            HomeScreen(
                onLogout = {
                    navController.navigate(AppDestination.Login) {
                        popUpTo<AppDestination.Home> { inclusive = true }
                    }
                }
            )
        }
    }
}
