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
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("email") val email: String,
    @SerializedName("phoneNumber") val phoneNumber: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("pinCode") val pinCode: String?,
    @SerializedName("idType") val idType: String?,
    @SerializedName("idNumber") val idNumber: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class RegisterRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("address") val address: String,
    @SerializedName("city") val city: String,
    @SerializedName("pinCode") val pinCode: String,
    @SerializedName("idType") val idType: String,
    @SerializedName("idNumber") val idNumber: String
)
