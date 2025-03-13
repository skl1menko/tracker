package com.example.tracker

import kotlinx.coroutines.flow.Flow

class LocationRepository(private val dao: LocationDao) {

    val allLocations: Flow<List<LocationEntity>> = dao.getAllLocations()

    suspend fun insertLocation(location: LocationEntity) {
        dao.insertLocation(location)
    }

    suspend fun clearLocations() {
        dao.clearLocations()
    }
}
