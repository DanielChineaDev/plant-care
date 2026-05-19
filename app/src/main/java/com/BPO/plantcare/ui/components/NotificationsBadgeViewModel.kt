package com.BPO.plantcare.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * VM minusculo que expone el conteo de notificaciones no leidas para
 * el badge del icono campana. Vive a nivel de NavBackStackEntry de cada
 * pantalla top-level. La fuente de verdad es Firestore (siempre).
 */
@HiltViewModel
class NotificationsBadgeViewModel @Inject constructor(
    repository: NotificationRepository,
) : ViewModel() {

    val unreadCount: StateFlow<Int> = repository.observeUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )
}
