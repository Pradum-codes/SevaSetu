package com.example.sevasetu.data.repository

import android.content.Context
import com.example.sevasetu.network.ApiService
import com.example.sevasetu.utils.TokenManager

object AuthContainer {
    fun provideAuthRepository(context: Context): AuthRepository {
        val appContext = context.applicationContext
        return AuthRepository(
            authApi = ApiService.authApi(appContext),
            tokenManager = TokenManager(appContext)
        )
    }
}
