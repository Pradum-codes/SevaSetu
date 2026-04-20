package com.example.sevasetu.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sevasetu.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, infoMessage = null) }
    }

    fun onOtpChanged(value: String) {
        _uiState.update { it.copy(otp = value.take(6), errorMessage = null, infoMessage = null) }
    }

    fun sendOtp() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter email") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

            repository.sendOtp(email)
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            otpSent = true,
                            infoMessage = message
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Failed to send OTP"
                        )
                    }
                }
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter email") }
            return
        }
        if (state.otp.length != 6) {
            _uiState.update { it.copy(errorMessage = "Please enter 6-digit OTP") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

            repository.verifyOtp(state.email, state.otp)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = user,
                            infoMessage = "Logged in"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            errorMessage = throwable.message ?: "OTP verification failed"
                        )
                    }
                }
        }
    }

    fun restoreSession(): Boolean {
        return !repository.getToken().isNullOrBlank()
    }

    fun logout() {
        repository.clearSession()
        _uiState.value = AuthUiState()
    }
}
