package com.example.tracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LocationRepository
    val locations: LiveData<List<LocationEntity>>

    init {
        val dao = LocationDatabase.getDatabase(application).locationDao()
        repository = LocationRepository(dao)
        locations = repository.allLocations.asLiveData()
    }

    fun insertLocation(location: LocationEntity) = viewModelScope.launch {
        repository.insertLocation(location)
    }

    fun clearLocations() = viewModelScope.launch {
        repository.clearLocations()
    }
}
