package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.BikeCareRepository
import com.example.data.location.DeviceLocationService
import com.example.data.location.NearbyPlacesService

class BikeCareViewModelFactory(
    private val repository: BikeCareRepository,
    private val locationService: DeviceLocationService,
    private val nearbyPlacesService: NearbyPlacesService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BikeCareViewModel::class.java)) {
            return BikeCareViewModel(repository, locationService, nearbyPlacesService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
