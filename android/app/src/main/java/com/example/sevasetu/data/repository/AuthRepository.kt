package com.example.sevasetu.data.repository

import com.example.sevasetu.data.remote.api.AuthApi
import com.example.sevasetu.data.remote.dto.OnboardingRegisterResponse
import com.example.sevasetu.data.remote.dto.RegisterRequest
import com.example.sevasetu.data.remote.dto.SendOtpRequest
import com.example.sevasetu.data.remote.dto.UserDto
import com.example.sevasetu.data.remote.dto.VerifyOtpRequest
import com.example.sevasetu.utils.TokenManager
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.io.IOException

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {

    suspend fun sendOtp(email: String): Result<String> {
        return runCatching {
            val response = authApi.sendOtp(SendOtpRequest(email.trim()))
            if (!response.isSuccessful) {
                val message = extractErrorMessage(response.errorBody()?.string())
                throw IllegalStateException(message ?: "Send OTP failed (${response.code()})")
            }
            response.body()?.message ?: "OTP sent"
        }.recoverCatching { throwable ->
            // Provide better error messages for network issues
            val friendlyMessage = when (throwable) {
                is SocketTimeoutException -> "Request timed out. The server took too long to respond. Please check your internet connection and try again."
                is IOException -> "Network error. Please check your internet connection and try again."
                else -> throwable.message ?: "Send OTP failed"
            }
            throw IllegalStateException(friendlyMessage)
        }
    }

    suspend fun verifyOtp(email: String, otp: String): Result<UserDto> {
        return runCatching {
            val response = authApi.verifyOtp(
                VerifyOtpRequest(
                    email = email.trim(),
                    otp = otp.trim()
                )
            )
            if (!response.isSuccessful) {
                val message = extractErrorMessage(response.errorBody()?.string())
                throw IllegalStateException(message ?: "Verify OTP failed (${response.code()})")
            }

            val body = requireNotNull(response.body()) { "verify-otp response body is null" }
            require(body.token.isNotBlank()) { "token missing in verify-otp response" }
            require(body.user.id.isNotBlank()) { "user.id missing in verify-otp response" }

            tokenManager.saveToken(body.token)
            body.user.addressDistrict?.let { tokenManager.saveUserDistrict(it) }
            body.user
        }.recoverCatching { throwable ->
            // Provide better error messages for network issues
            val friendlyMessage = when (throwable) {
                is SocketTimeoutException -> "Request timed out. Please check your internet connection and try again."
                is IOException -> "Network error. Please check your internet connection and try again."
                else -> throwable.message ?: "OTP verification failed"
            }
            throw IllegalStateException(friendlyMessage)
        }
    }

    suspend fun register(request: RegisterRequest): Result<OnboardingRegisterResponse> {
        return runCatching {
            val response = authApi.register(request)
            if (!response.isSuccessful) {
                val message = extractErrorMessage(response.errorBody()?.string())
                throw IllegalStateException(message ?: "Registration failed (${response.code()})")
            }
            val body = requireNotNull(response.body()) { "register response body is null" }
            require(body.message.isNotBlank()) { "message missing in register response" }
            require(body.user.id.isNotBlank()) { "user.id missing in register response" }
            body
        }.recoverCatching { throwable ->
            // Provide better error messages for network issues
            val friendlyMessage = when (throwable) {
                is SocketTimeoutException -> "Request timed out. Please check your internet connection and try again."
                is IOException -> "Network error. Please check your internet connection and try again."
                else -> throwable.message ?: "Registration failed"
            }
            throw IllegalStateException(friendlyMessage)
        }
    }

    fun getToken(): String? = tokenManager.getToken()

    fun saveUserDistrict(districtId: String) {
        tokenManager.saveUserDistrict(districtId)
    }

    fun getUserDistrict(): String? = tokenManager.getUserDistrict()

    fun clearSession() {
        tokenManager.clear()
    }

    private fun extractErrorMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            when {
                json.has("message") -> json.optString("message")
                json.has("error") -> json.optString("error")
                else -> null
            }
        }.getOrNull()
    }
}
