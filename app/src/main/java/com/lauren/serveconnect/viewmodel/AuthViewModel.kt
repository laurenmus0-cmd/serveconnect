package com.lauren.serveconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lauren.serveconnect.models.User
import com.lauren.serveconnect.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()
    private var userObservationJob: Job? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _userDetails = MutableStateFlow<User?>(null)
    val userDetails: StateFlow<User?> = _userDetails

    init {
        // Try to observe if already logged in on init
        fetchUserDetails()
    }

    fun registerUser(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        role: String
    ) {
        _authState.value = AuthState.Loading
        repository.registerUser(
            email, password, fullName, phone, role,
            onSuccess = { 
                _authState.value = AuthState.Success(role)
                fetchUserDetails() 
            },
            onFailure = { _authState.value = AuthState.Error(it) }
        )
    }

    fun loginUser(email: String, password: String) {
        _authState.value = AuthState.Loading
        repository.loginUser(
            email, password,
            onSuccess = { role -> 
                _authState.value = AuthState.Success(role)
                fetchUserDetails()
            },
            onFailure = { _authState.value = AuthState.Error(it) }
        )
    }

    fun logoutUser() {
        userObservationJob?.cancel()
        userObservationJob = null
        repository.logoutUser()
        _userDetails.value = null
        _authState.value = AuthState.Idle
    }

    fun isUserLoggedIn() = repository.isUserLoggedIn()

    fun fetchUserDetails() {
        val uid = repository.getCurrentUserId()
        if (uid.isNotEmpty() && userObservationJob == null) {
            userObservationJob = viewModelScope.launch {
                repository.getUserDetailsFlow(uid).collectLatest { user ->
                    _userDetails.value = user
                }
            }
        }
    }
}

// States for the UI to react to
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
