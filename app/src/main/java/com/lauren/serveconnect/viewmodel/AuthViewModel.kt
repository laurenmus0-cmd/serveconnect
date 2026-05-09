package com.lauren.serveconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lauren.serveconnect.models.User
import com.lauren.serveconnect.repository.AuthRepository
import com.lauren.serveconnect.utils.AccountManager
import com.lauren.serveconnect.utils.RememberedAccount
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()
    private var userObservationJob: Job? = null
    
    private lateinit var accountManager: AccountManager

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _userDetails = MutableStateFlow<User?>(null)
    val userDetails: StateFlow<User?> = _userDetails
    
    private val _savedAccounts = MutableStateFlow<List<RememberedAccount>>(emptyList())
    val savedAccounts = _savedAccounts.asStateFlow()

    fun initAccountManager(context: android.content.Context) {
        accountManager = AccountManager(context)
        _savedAccounts.value = accountManager.getAccounts()
    }

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
                val uid = repository.getCurrentUserId()
                accountManager.saveAccount(RememberedAccount(email, fullName, role, uid, password))
                _savedAccounts.value = accountManager.getAccounts()
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
                val uid = repository.getCurrentUserId()
                repository.getUserDetails(uid, { user ->
                     accountManager.saveAccount(RememberedAccount(email, user.fullName, role, uid, password))
                     _savedAccounts.value = accountManager.getAccounts()
                }, {})
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

    fun getCurrentUserId() = repository.getCurrentUserId()

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

    fun switchAccount(account: RememberedAccount) {
        logoutUser()
        loginUser(account.email, account.pass)
    }

    fun updateProfile(fullName: String, email: String, phone: String, location: String, profileImageUrl: String, onComplete: (Boolean, String?) -> Unit) {
        val uid = repository.getCurrentUserId()
        if (uid.isEmpty()) {
            onComplete(false, "User not logged in")
            return
        }

        val updates = mapOf(
            "fullName" to fullName,
            "email" to email,
            "phone" to phone,
            "location" to location,
            "profileImageUrl" to profileImageUrl
        )

        repository.updateUserDetails(uid, updates, onComplete)
    }
}

// States for the UI to react to
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
