package dj.nomade.velox_restaurant.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable data object Login : AppDestination
    @Serializable data object Home  : AppDestination
}
