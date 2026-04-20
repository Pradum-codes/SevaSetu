package com.example.sevasetu.utils

enum class LaunchDestination {
    LOGIN,
    HOME
}

class SessionManager(
    private val tokenManager: TokenManager
) {
    fun resolveLaunchDestination(): LaunchDestination {
        return if (tokenManager.getToken().isNullOrBlank()) {
            LaunchDestination.LOGIN
        } else {
            LaunchDestination.HOME
        }
    }
}
