package com.khata.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.data.repository.TransactionRepository
import com.khata.app.data.sync.AuthManager
import com.khata.app.data.sync.AuthState
import com.khata.app.data.sync.SyncManager
import com.khata.app.data.sync.SyncState
import com.khata.app.domain.model.Balance
import com.khata.app.domain.model.TransactionWithCustomer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.khata.app.data.security.SecurityPreferences

data class SettingsUiState(
    val isLoading: Boolean = true,
    val shopName: String = "My Shop Khata",
    val activeCustomerCount: Int = 0,
    val totalOutstanding: Long = 0,
    val totalTransactionsCount: Int = 0,
    val databaseVersion: String = "v2.0 (Room SQLite + Supabase Cloud)",
    val syncState: SyncState = SyncState.Synced,
    val authState: AuthState = AuthState.SignedOut,
    val lastSyncTimestamp: Long? = null,
    val authError: String? = null,
    val isAuthActionLoading: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val hasError: Boolean = false,
)

class SettingsViewModel(
    customerRepository: CustomerRepository,
    transactionRepository: TransactionRepository,
    private val authManager: AuthManager,
    private val syncManager: SyncManager,
    private val securityPreferences: SecurityPreferences,
) : ViewModel() {

    private val _authError = MutableStateFlow<String?>(null)
    private val _isAuthActionLoading = MutableStateFlow(false)
    private val _appLockState = MutableStateFlow(securityPreferences.isAppLockEnabled())
    private val _shopNameState = MutableStateFlow(securityPreferences.getShopName())

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SettingsUiState> = combine(
        customerRepository.observeActiveCustomerCount(),
        transactionRepository.observeOverallBalance(),
        transactionRepository.observeTransactions(),
        syncManager.syncState,
        authManager.authState,
        syncManager.lastSyncTimestamp,
        _authError,
        _isAuthActionLoading,
        _appLockState,
        _shopNameState,
    ) { args: Array<Any?> ->
        val customerCount = args[0] as Int
        val balance = args[1] as Balance
        val transactions = args[2] as List<TransactionWithCustomer>
        val syncState = args[3] as SyncState
        val authState = args[4] as AuthState
        val lastSync = args[5] as Long?
        val authErr = args[6] as String?
        val isAuthLoading = args[7] as Boolean
        val appLockEnabled = args[8] as Boolean
        val shopName = args[9] as String

        SettingsUiState(
            isLoading = false,
            shopName = shopName,
            activeCustomerCount = customerCount,
            totalOutstanding = balance.outstanding,
            totalTransactionsCount = transactions.size,
            syncState = syncState,
            authState = authState,
            lastSyncTimestamp = lastSync,
            authError = authErr,
            isAuthActionLoading = isAuthLoading,
            isAppLockEnabled = appLockEnabled,
        )
    }
        .catch { emit(SettingsUiState(isLoading = false, hasError = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(),
        )

    fun updateShopName(newName: String): Boolean {
        val success = securityPreferences.setShopName(newName)
        if (success) {
            _shopNameState.value = newName.trim()
        }
        return success
    }

    fun setAppLockEnabled(enabled: Boolean) {
        securityPreferences.setAppLockEnabled(enabled)
        _appLockState.value = enabled
    }

    fun verifyPin(pin: String): Boolean {
        return securityPreferences.verifyCode(pin)
    }

    fun changePin(newPin: String): Boolean {
        val success = securityPreferences.setSecurityCode(newPin)
        return success
    }

    fun triggerSyncNow() {
        syncManager.triggerSync()
    }

    fun signIn(emailVal: String, passwordVal: String) {
        _isAuthActionLoading.value = true
        _authError.value = null
        viewModelScope.launch {
            val result = authManager.signIn(emailVal, passwordVal)
            _isAuthActionLoading.value = false
            if (result.isFailure) {
                _authError.value = result.exceptionOrNull()?.localizedMessage ?: "Sign in failed"
            } else {
                syncManager.triggerSync()
            }
        }
    }

    fun signUp(emailVal: String, passwordVal: String) {
        _isAuthActionLoading.value = true
        _authError.value = null
        viewModelScope.launch {
            val result = authManager.signUp(emailVal, passwordVal)
            _isAuthActionLoading.value = false
            if (result.isFailure) {
                _authError.value = result.exceptionOrNull()?.localizedMessage ?: "Sign up failed"
            } else {
                syncManager.triggerSync()
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            syncManager.triggerSync()
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
