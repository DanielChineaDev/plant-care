package com.BPO.plantcare.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.ChatMessage
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.model.conversationIdOf
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.ChatRepository
import com.BPO.plantcare.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ChatEvent {
    data class Error(val message: String) : ChatEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val otherUid: String =
        checkNotNull(savedStateHandle.get<String>(NavArgs.OTHER_UID))

    val currentUid: StateFlow<String?> = authRepository.authState
        .map { (it as? AuthState.SignedIn)?.profile?.uid }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _otherProfile = MutableStateFlow<UserProfile?>(null)
    val otherProfile: StateFlow<UserProfile?> = _otherProfile.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getProfile(otherUid).onSuccess { _otherProfile.value = it }
        }
    }

    val messages: StateFlow<List<ChatMessage>> = currentUid
        .flatMapLatest { my ->
            if (my == null) flowOf(emptyList())
            else chatRepository.observeMessages(conversationIdOf(my, otherUid))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _events = Channel<ChatEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val other = _otherProfile.value
        viewModelScope.launch {
            chatRepository.sendMessage(
                otherUid = otherUid,
                otherName = other?.displayName.orEmpty(),
                otherPhoto = other?.photoUrl,
                text = text.trim(),
            ).onFailure {
                _events.send(ChatEvent.Error(it.localizedMessage.orEmpty()))
            }
        }
    }
}
