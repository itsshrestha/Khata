package com.khata.app.data.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AuthState {
    data object SignedOut : AuthState
    data object Loading : AuthState
    data class Authenticated(val userId: String, val email: String?) : AuthState
}

class AuthManager(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {

    val authState: StateFlow<AuthState> = client.auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    if (user != null) {
                        AuthState.Authenticated(userId = user.id, email = user.email)
                    } else {
                        AuthState.SignedOut
                    }
                }
                SessionStatus.Initializing -> AuthState.Loading
                else -> AuthState.SignedOut
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Loading,
        )

    val currentUserId: String?
        get() = (authState.value as? AuthState.Authenticated)?.userId

    val currentUserEmail: String?
        get() = (authState.value as? AuthState.Authenticated)?.email

    suspend fun signUp(emailVal: String, passwordVal: String): Result<Unit> = runCatching {
        client.auth.signUpWith(Email) {
            email = emailVal
            password = passwordVal
        }
    }

    suspend fun signIn(emailVal: String, passwordVal: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) {
            email = emailVal
            password = passwordVal
        }
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        client.auth.signOut()
    }
}
