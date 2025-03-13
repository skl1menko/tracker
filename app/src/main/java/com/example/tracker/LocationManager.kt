package com.example.tracker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class LocationManager(context: Context) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getLocation(
        onSuccess: (
            latitude: String, longitude: String
        ) -> Unit
    ) {
        fusedLocationClient
            .lastLocation
            .addOnSuccessListener { location ->
                val latitude = location.latitude.toString()
                val longitude = location.longitude.toString()

                onSuccess(latitude, longitude)
            }
    }

    fun trackLocation(): Flow<Location> {
        return callbackFlow {
            val locationCallback = locationCallback { location ->
                launch {
                    send(location)
                }
            }

            val request = LocationRequest
                .Builder(1000)
                .build()

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            awaitClose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }

        }
    }

    private fun locationCallback(
        onResult: (location: Location) -> Unit
    ): LocationCallback {

        return object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.lastOrNull()?.let { location ->
                    onResult(location)
                }
            }
        }
    }
}