package com.BPO.plantcare.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.ChatMessage
import com.BPO.plantcare.domain.model.ChatPresence
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.model.conversationIdOf
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.ChatRepository
import com.BPO.plantcare.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
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

    /** Texto de busqueda dentro de la conversacion (null = buscador cerrado). */
    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    private var typingJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.getProfile(otherUid).onSuccess { _otherProfile.value = it }
        }
        // Marcamos como leido cada vez que cambia la lista de mensajes.
        viewModelScope.launch {
            messages.collect { list ->
                if (list.isNotEmpty()) chatRepository.markRead(otherUid)
            }
        }
    }

    private val allMessages: StateFlow<List<ChatMessage>> = currentUid
        .flatMapLatest { my ->
            if (my == null) flowOf(emptyList())
            else chatRepository.observeMessages(conversationIdOf(my, otherUid))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Mensajes filtrados por el texto de busqueda (si esta activo). */
    val messages: StateFlow<List<ChatMessage>> =
        combine(allMessages, _searchQuery) { list, q ->
            val term = q?.trim()?.lowercase()
            if (term.isNullOrEmpty()) list
            else list.filter { it.text.lowercase().contains(term) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val presence: StateFlow<ChatPresence> = currentUid
        .flatMapLatest { my ->
            if (my == null) flowOf(ChatPresence())
            else chatRepository.observePresence(conversationIdOf(my, otherUid), otherUid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChatPresence(),
        )

    private val _events = Channel<ChatEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun sendMessage(text: String, photoFile: File? = null) {
        if (text.isBlank() && photoFile == null) return
        val other = _otherProfile.value
        viewModelScope.launch {
            chatRepository.sendMessage(
                otherUid = otherUid,
                otherName = other?.displayName.orEmpty(),
                otherPhoto = other?.photoUrl,
                text = text.trim(),
                photoFile = photoFile,
            ).onFailure {
                _events.send(ChatEvent.Error(it.localizedMessage.orEmpty()))
            }
        }
    }

    /**
     * Notifica que el usuario esta escribiendo. Marca typing=true y programa
     * apagarlo tras un periodo de inactividad. Llamar en cada cambio de texto.
     */
    fun onInputChanged(text: String) {
        typingJob?.cancel()
        if (text.isBlank()) {
            viewModelScope.launch { chatRepository.setTyping(otherUid, false) }
            return
        }
        typingJob = viewModelScope.launch {
            chatRepository.setTyping(otherUid, true)
            delay(TYPING_IDLE_MS)
            chatRepository.setTyping(otherUid, false)
        }
    }

    fun react(messageId: String, emoji: String?) {
        viewModelScope.launch {
            chatRepository.reactToMessage(otherUid, messageId, emoji)
                .onFailure { _events.send(ChatEvent.Error(it.localizedMessage.orEmpty())) }
        }
    }

    fun openSearch() {
        _searchQuery.value = ""
    }

    fun closeSearch() {
        _searchQuery.value = null
    }

    fun onSearchChange(value: String) {
        _searchQuery.value = value
    }

    companion object {
        private const val TYPING_IDLE_MS = 3_500L
    }
}
