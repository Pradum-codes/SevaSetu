package com.example.sevasetu.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SendOtpRequest(
    @SerializedName("email") val email: String
)

data class SendOtpResponse(
    @SerializedName("message") val message: String
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("createdAt") val createdAt: String?
)
