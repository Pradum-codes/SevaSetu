package com.example.sevasetu.data.repository

import com.example.sevasetu.data.remote.api.AuthApi
import com.example.sevasetu.data.remote.dto.SendOtpRequest
import com.example.sevasetu.data.remote.dto.UserDto
import com.example.sevasetu.data.remote.dto.VerifyOtpRequest
import com.example.sevasetu.utils.TokenManager
import org.json.JSONObject

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
            body.user
        }
    }

    fun getToken(): String? = tokenManager.getToken()

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
