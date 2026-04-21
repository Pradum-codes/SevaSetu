package com.example.sevasetu.ui.common

import com.example.sevasetu.data.remote.dto.UserDto

data class AuthUiState(
    val isLoading: Boolean = false,
    val email: String = "",
    val otp: String = "",
    val otpSent: Boolean = false,
    val isAuthenticated: Boolean = false,
    val registrationCompleted: Boolean = false,
    val user: UserDto? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)
