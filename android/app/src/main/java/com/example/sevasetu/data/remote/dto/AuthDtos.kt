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

data class RegistrationStatusDto(
    @SerializedName("onboardingCompleted") val onboardingCompleted: Boolean,
    @SerializedName("profileCompleted") val profileCompleted: Boolean,
    @SerializedName("locationCaptured") val locationCaptured: Boolean
)

data class OnboardingUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("idType") val idType: String,
    @SerializedName("idNumber") val idNumber: String,
    @SerializedName("addressText") val addressText: String,
    @SerializedName("addressCityOrPanchayat") val addressCityOrPanchayat: String,
    @SerializedName("addressDistrict") val addressDistrict: String?,
    @SerializedName("pinCode") val pinCode: String
)

data class OnboardingRegisterResponse(
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: OnboardingUserDto,
    @SerializedName("registrationStatus") val registrationStatus: RegistrationStatusDto
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("email") val email: String,
    @SerializedName("phoneNumber") val phoneNumber: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("addressDistrict") val addressDistrict: String?,
    @SerializedName("pinCode") val pinCode: String?,
    @SerializedName("idType") val idType: String?,
    @SerializedName("idNumber") val idNumber: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("idType") val idType: String,
    @SerializedName("idNumber") val idNumber: String,
    @SerializedName("jurisdictionId") val jurisdictionId: String,
    @SerializedName("addressDistrict") val addressDistrict: String,
    @SerializedName("addressAreaType") val addressAreaType: String,
    @SerializedName("addressCity") val addressCity: String? = null,
    @SerializedName("addressWard") val addressWard: String? = null,
    @SerializedName("addressBlock") val addressBlock: String? = null,
    @SerializedName("addressPanchayat") val addressPanchayat: String? = null,
    @SerializedName("addressLocality") val addressLocality: String? = null,
    @SerializedName("addressLandmark") val addressLandmark: String? = null,
    @SerializedName("addressText") val addressText: String,
    @SerializedName("pinCode") val pinCode: String
)
