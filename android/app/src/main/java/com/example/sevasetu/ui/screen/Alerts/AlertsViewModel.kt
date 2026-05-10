package com.example.sevasetu.ui.screen.Alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sevasetu.data.remote.dto.NotificationDto
import com.example.sevasetu.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val notifications: List<NotificationDto> = emptyList(),
    val unreadCount: Int = 0,
    val errorMessage: String? = null
)

class AlertsViewModel(
    private val repository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    fun loadNotifications(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    errorMessage = null
                )
            }

            repository.getMyNotifications(page = 1, limit = 50)
                .onSuccess { response ->
                    val notifications = response.notifications
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = notifications,
                            unreadCount = notifications.count { n -> !n.read }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "Failed to load notifications"
                        )
                    }
                }
        }
    }

    fun markAsRead(notificationId: String?) {
        if (notificationId.isNullOrBlank()) return

        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
                .onSuccess {
                    _uiState.update { state ->
                        val updated = state.notifications.map {
                            if (it.id == notificationId) it.copy(read = true) else it
                        }
                        state.copy(
                            notifications = updated,
                            unreadCount = updated.count { n -> !n.read }
                        )
                    }
                }
                .onFailure { }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { it.copy(read = true) },
                            unreadCount = 0
                        )
                    }
                }
                .onFailure { }
        }
    }
}

class AlertsViewModelFactory(
    private val repository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertsViewModel::class.java)) {
            return AlertsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
