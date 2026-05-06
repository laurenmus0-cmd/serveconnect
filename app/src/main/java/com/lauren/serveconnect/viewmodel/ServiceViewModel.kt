package com.lauren.serveconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lauren.serveconnect.model.ServicePost
import com.lauren.serveconnect.repository.ServiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceViewModel : ViewModel() {
    private val repository = ServiceRepository()

    private val _services = MutableStateFlow<List<ServicePost>>(emptyList())
    val services: StateFlow<List<ServicePost>> = _services

    init {
        fetchServices()
    }

    private fun fetchServices() {
        viewModelScope.launch {
            repository.getServices().collect {
                _services.value = it
            }
        }
    }

    fun postService(service: ServicePost, onComplete: (Boolean, String?) -> Unit) {
        repository.postService(service, 
            onSuccess = { onComplete(true, null) },
            onFailure = { onComplete(false, it.message) }
        )
    }

    fun updateService(service: ServicePost, onComplete: (Boolean, String?) -> Unit) {
        repository.updateService(service,
            onSuccess = { onComplete(true, null) },
            onFailure = { onComplete(false, it.message) }
        )
    }
}
