package com.cretemobility.app.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cretemobility.app.domain.model.*
import com.cretemobility.app.domain.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    val favoriteRoutes = favoritesRepository.getFavoriteRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteStops = favoritesRepository.getFavoriteStops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeRoute(id: Long) {
        viewModelScope.launch { favoritesRepository.removeFavoriteRoute(id) }
    }

    fun removeStop(id: Long) {
        viewModelScope.launch { favoritesRepository.removeFavoriteStop(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlanner: (String, String) -> Unit,
    onNavigateToDepartures: (String) -> Unit
) {
    val routes by viewModel.favoriteRoutes.collectAsState()
    val stops by viewModel.favoriteStops.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Routes (${routes.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Stops (${stops.size})") })
            }

            when (selectedTab) {
                0 -> FavoriteRoutesList(
                    routes = routes,
                    onRemove = viewModel::removeRoute,
                    onUse = { route ->
                        onNavigateToPlanner(
                            route.origin.toJson(),
                            route.destination.toJson()
                        )
                    }
                )
                1 -> FavoriteStopsList(
                    stops = stops,
                    onRemove = viewModel::removeStop,
                    onViewDepartures = { stop ->
                        onNavigateToDepartures(stop.stop.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun FavoriteRoutesList(
    routes: List<FavoriteRoute>,
    onRemove: (Long) -> Unit,
    onUse: (FavoriteRoute) -> Unit
) {
    if (routes.isEmpty()) {
        EmptyFavoritesView(
            icon = "🗺️",
            message = "No saved routes yet",
            hint = "Plan a journey and save it for quick access"
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(routes, key = { it.id }) { route ->
            FavoriteRouteCard(
                route = route,
                onUse = { onUse(route) },
                onRemove = { onRemove(route.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteRouteCard(route: FavoriteRoute, onUse: () -> Unit, onRemove: () -> Unit) {
    Card(
        onClick = onUse,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Directions, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(route.name.ifEmpty { "${route.origin.name} → ${route.destination.name}" },
                    fontWeight = FontWeight.SemiBold)
                Text(
                    "Used ${route.usageCount}x",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, "Remove", tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun FavoriteStopsList(
    stops: List<FavoriteStop>,
    onRemove: (Long) -> Unit,
    onViewDepartures: (FavoriteStop) -> Unit
) {
    if (stops.isEmpty()) {
        EmptyFavoritesView(
            icon = "🚏",
            message = "No saved stops yet",
            hint = "Tap the star on any stop to save it"
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stops, key = { it.id }) { fav ->
            FavoriteStopCard(
                favoriteStop = fav,
                onViewDepartures = { onViewDepartures(fav) },
                onRemove = { onRemove(fav.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteStopCard(
    favoriteStop: FavoriteStop,
    onViewDepartures: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        onClick = onViewDepartures,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.DirectionsBus, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(favoriteStop.customName ?: favoriteStop.stop.name,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    favoriteStop.stop.region?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Crete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, "Remove", tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyFavoritesView(icon: String, message: String, hint: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(icon, style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(hint, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun Location.toJson(): String = android.util.Base64.encodeToString(
    com.google.gson.Gson().toJson(this).toByteArray(),
    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
)
