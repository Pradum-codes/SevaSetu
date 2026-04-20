package com.example.sevasetu.network

import android.content.Context
import com.example.sevasetu.data.remote.api.AuthApi

object ApiService {
    fun authApi(context: Context): AuthApi = NetworkModule.provideAuthApi(context)
}
