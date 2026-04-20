package com.example.sevasetu.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object UnauthorizedEventBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events

    fun notifyUnauthorized() {
        _events.tryEmit(Unit)
    }
}
