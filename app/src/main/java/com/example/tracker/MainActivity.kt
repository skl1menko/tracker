package com.example.tracker

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.tracker.LocationDatabase
import com.example.tracker.LocationEntity
import com.example.tracker.ui.theme.TrackerTheme
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
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            var locationText by remember {
                mutableStateOf("")
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
