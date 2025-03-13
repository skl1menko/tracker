package com.example.tracker

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.tracker.ui.theme.TrackerTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val locationManager by lazy {
        LocationManager(applicationContext)
    }

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(this, permissions, 100)
        enableEdgeToEdge()

        val database = LocationDatabase.getDatabase(applicationContext)

        setContent {
            TrackerTheme {
                val scope = rememberCoroutineScope()
                var locations by remember { mutableStateOf<List<LocationEntity>>(emptyList()) }

                LaunchedEffect(Unit) {
                    scope.launch {
                        database.locationDao().getAllLocations().collectLatest {
                            locations = it
                        }
                    }
                }

                Screen(
                    startTracking = {
                        Intent(applicationContext, LocationTrackerService::class.java).also {
                            it.action = LocationTrackerService.Action.START.name
                            startService(it)
                        }
                    },
                    stopTracking = {
                        Intent(applicationContext, LocationTrackerService::class.java).also {
                            it.action = LocationTrackerService.Action.STOP.name
                            startService(it)
                        }
                    },
                    clearDatabase = {
                        scope.launch {
                            database.locationDao().clearLocations()
                            locations = emptyList() // Очищаем список на UI
                        }
                    },
                    locations = locations
                )
            }
        }
    }

    @Composable
    fun Screen(
        startTracking: () -> Unit,
        stopTracking: () -> Unit,
        clearDatabase: () -> Unit,
        locations: List<LocationEntity>
    ) {

        var mapReady by remember { mutableStateOf(false) }
        val cameraPositionState = rememberCameraPositionState()
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            var locationText by remember {
                mutableStateOf("")
            }
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                cameraPositionState = cameraPositionState,
                onMapLoaded = { mapReady = true }
            ) {
                if (mapReady && locations.isNotEmpty()) {
                    val startLocation = LatLng(locations.first().latitude, locations.first().longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(startLocation, 15f)

                    Polyline(
                        points = locations.map { LatLng(it.latitude, it.longitude) },
                        color = Color.Blue,
                        width = 5f
                    )

                    Marker(
                        state = MarkerState(position = startLocation),
                        title = "Start"
                    )

                    Marker(
                        state = MarkerState(position = LatLng(locations.last().latitude, locations.last().longitude)),
                        title = "End"
                    )
                }
            }

            Text(text = locationText)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    locationManager.getLocation { latitude, longitude ->
                        locationText = "Location: ..$latitude / ..$longitude"
                    }
                }
            ) {
                Text(text = "Get Location")
            }

            Spacer(modifier = Modifier.height(50.dp))

            Button(onClick = startTracking) {
                Text(text = "Start Tracking")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = stopTracking) {
                Text(text = "Stop Tracking")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = clearDatabase, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)) {
                Text(text = "Clear Database")
            }

            Spacer(modifier = Modifier.height(32.dp))



            LazyColumn {
                items(locations) { location ->
                    Text(
                        text = "Lat: ${location.latitude}, Lng: ${location.longitude}",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
