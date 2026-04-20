package com.example.sevasetu.data.remote.api

import com.example.sevasetu.data.remote.dto.AuthResponse
import com.example.sevasetu.data.remote.dto.SendOtpRequest
import com.example.sevasetu.data.remote.dto.SendOtpResponse
import com.example.sevasetu.data.remote.dto.VerifyOtpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<SendOtpResponse>

    @POST("/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>
}
