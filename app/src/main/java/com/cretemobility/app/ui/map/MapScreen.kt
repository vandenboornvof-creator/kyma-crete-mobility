package com.cretemobility.app.ui.map

import android.content.Context
import android.graphics.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cretemobility.app.domain.model.*
import com.cretemobility.app.domain.repository.LocationRepository
import com.cretemobility.app.domain.repository.StopsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import javax.inject.Inject

// ─── Map ViewModel ────────────────────────────────────────────────────────────

data class MapState(
    val stops: List<TransitStop> = emptyList(),
    val currentLocation: LatLng? = null,
    val selectedStop: TransitStop? = null,
    val routePolyline: List<LatLng> = emptyList(),
    val isLoading: Boolean = false,
    val initialLat: Double = 35.3387,
    val initialLng: Double = 25.1442
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val stopsRepository: StopsRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    init {
        loadStops()
        observeLocation()
    }

    private fun loadStops() {
        stopsRepository.getAllStops()
            .onEach { stops ->
                _state.update { it.copy(stops = stops) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeLocation() {
        locationRepository.observeCurrentLocation()
            .onEach { loc ->
                _state.update { it.copy(currentLocation = loc) }
            }
            .launchIn(viewModelScope)
    }

    fun setInitialPosition(lat: Double, lng: Double) {
        _state.update { it.copy(initialLat = lat, initialLng = lng) }
    }

    fun selectStop(stop: TransitStop?) {
        _state.update { it.copy(selectedStop = stop) }
    }

    fun loadStopsInRegion(region: CreteRegion) {
        stopsRepository.getStopsByRegion(region)
            .onEach { stops ->
                _state.update { it.copy(stops = stops) }
            }
            .launchIn(viewModelScope)
    }
}

// ─── Map Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    initialLat: Double,
    initialLng: Double,
    stopId: String? = null,
    viewModel: MapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDepartures: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(initialLat, initialLng) {
        viewModel.setInitialPosition(initialLat, initialLng)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // OSMDroid Map
        OsmMapView(
            context = context,
            initialLat = initialLat,
            initialLng = initialLng,
            stops = state.stops,
            currentLocation = state.currentLocation,
            routePolyline = state.routePolyline,
            onStopClick = { stop -> viewModel.selectStop(stop) },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar overlay
        TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            title = { Text("Map", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        )

        // Selected stop bottom sheet
        state.selectedStop?.let { stop ->
            StopBottomCard(
                stop = stop,
                modifier = Modifier.align(Alignment.BottomCenter),
                onDepartures = { onNavigateToDepartures(stop.id) },
                onDismiss = { viewModel.selectStop(null) }
            )
        }

        // FAB: center on user location
        FloatingActionButton(
            onClick = { /* center map on user location */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = if (state.selectedStop != null) 130.dp else 0.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.MyLocation, "My Location", tint = Color.White)
        }
    }
}

@Composable
private fun OsmMapView(
    context: Context,
    initialLat: Double,
    initialLng: Double,
    stops: List<TransitStop>,
    currentLocation: LatLng?,
    routePolyline: List<LatLng>,
    onStopClick: (TransitStop) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().apply {
                userAgentValue = "CreteMobility/1.0"
                osmdroidBasePath = ctx.getExternalFilesDir(null)
                osmdroidTileCache = java.io.File(ctx.getExternalFilesDir(null), "tiles")
            }

            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(14.0)
                controller.setCenter(GeoPoint(initialLat, initialLng))
                minZoomLevel = 8.0
                maxZoomLevel = 19.0

                // My location overlay
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                locationOverlay.enableMyLocation()
                overlays.add(locationOverlay)

                // Compass
                val compassOverlay = CompassOverlay(ctx, this)
                compassOverlay.enableCompass()
                overlays.add(compassOverlay)
            }
        },
        update = { mapView ->
            // Clear existing markers (except system overlays)
            mapView.overlays.removeAll { it is Marker }

            // Add stop markers
            stops.forEach { stop ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(stop.location.latitude, stop.location.longitude)
                    title = stop.name
                    snippet = stop.type.name
                    icon = createBusStopIcon(context, stop.type)
                    setOnMarkerClickListener { _, _ ->
                        onStopClick(stop)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }

            // Add route polyline
            if (routePolyline.isNotEmpty()) {
                val polyline = Polyline(mapView).apply {
                    setPoints(routePolyline.map { GeoPoint(it.latitude, it.longitude) })
                    outlinePaint.color = android.graphics.Color.parseColor("#1565C0")
                    outlinePaint.strokeWidth = 8f
                }
                mapView.overlays.add(polyline)
            }

            mapView.invalidate()
        },
        modifier = modifier
    )
}

private fun createBusStopIcon(context: Context, stopType: StopType): android.graphics.drawable.Drawable {
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = when (stopType) {
        StopType.FERRY_PORT -> android.graphics.Color.parseColor("#006064")
        StopType.AIRPORT -> android.graphics.Color.parseColor("#4A148C")
        else -> android.graphics.Color.parseColor("#1565C0")
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

    paint.color = android.graphics.Color.WHITE
    paint.strokeWidth = 3f
    paint.style = Paint.Style.STROKE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val text = when (stopType) {
        StopType.FERRY_PORT -> "F"
        StopType.AIRPORT -> "✈"
        else -> "B"
    }
    canvas.drawText(text, size / 2f, size / 2f + 10f, textPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

@Composable
private fun StopBottomCard(
    stop: TransitStop,
    onDepartures: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stop.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stop.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close")
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onDepartures,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Departures")
            }
        }
    }
}
