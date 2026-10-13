package com.example.modelolocal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.modelolocal.data.AppDatabase
import com.example.modelolocal.data.ChatRepository
import java.io.File

// Modelo de datos para la UI
data class ChatMessage(
    val role: String,           // "user" o "assistant"
    val content: String,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

// Estado del modelo
sealed class ModelState {
    object Loading : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val bridge = LlamaBridge()
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "chat-db"
    ).build()
    private val repository = ChatRepository(db.chatDao())

    // Estado del modelo
    private val _modelState = MutableStateFlow<ModelState>(ModelState.Loading)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    // Mensajes del chat actual
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Estado de generación
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Chat actual
    private val _currentChatId = MutableStateFlow<Long?>(null)
    val currentChatId: StateFlow<Long?> = _currentChatId.asStateFlow()

    // Lista de chats anteriores
    val chatHistory = repository.getAllChats()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Carga automática del modelo al iniciar
        loadModel()
    }

    private fun loadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _modelState.value = ModelState.Loading
            val modelFile = File(getApplication<Application>().filesDir, "modelo.gguf")
            if (!modelFile.isFile || !modelFile.canRead()) {
                _modelState.value = ModelState.Error(
                    "No se encontró un modelo legible en: ${modelFile.absolutePath}"
                )
                return@launch
            }
            val availableProcessors = Runtime.getRuntime().availableProcessors()
            val generationThreads = (availableProcessors - 2).coerceIn(2, 6)
            val batchThreads = availableProcessors.coerceIn(2, 8)
            val success = bridge.loadModel(
                modelFile.absolutePath,
                contextSize = 2048,
                threads = generationThreads,
                batchThreads = batchThreads
            )
            if (success) {
                _modelState.value = ModelState.Ready
            } else {
                _modelState.value = ModelState.Error("No se pudo cargar el modelo desde: ${modelFile.absolutePath}")
            }
        }
    }

    fun sendPrompt(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isEmpty() || _modelState.value !is ModelState.Ready || _isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            var responseBuilder = StringBuilder()
            var assistantTimestamp = System.currentTimeMillis()

            try {
                val userTimestamp = System.currentTimeMillis()
                _messages.value = _messages.value + ChatMessage(
                    role = "user",
                    content = trimmedPrompt,
                    timestamp = userTimestamp
                )

                if (_currentChatId.value == null) {
                    val title = trimmedPrompt.take(40) + if (trimmedPrompt.length > 40) "..." else ""
                    _currentChatId.value = withContext(Dispatchers.IO) {
                        repository.createChat(title)
                    }
                }

                val chatId = _currentChatId.value
                    ?: error("No se pudo crear el chat actual")
                withContext(Dispatchers.IO) {
                    repository.addMessage(chatId, "user", trimmedPrompt)
                }

                assistantTimestamp = System.currentTimeMillis()
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "",
                    isStreaming = true,
                    timestamp = assistantTimestamp
                )

                bridge.onTokenCallback = { token ->
                    synchronized(responseBuilder) {
                        responseBuilder.append(token)
                        updateStreamingMessage(responseBuilder.toString(), assistantTimestamp)
                    }
                }

                val result = withContext(Dispatchers.Default) {
                    bridge.generateCompletion(trimmedPrompt, maxTokens = 512)
                }

                val finalContent = responseBuilder.toString().ifEmpty { result }
                updateStreamingMessage(finalContent, assistantTimestamp, isStreaming = false)

                withContext(Dispatchers.IO) {
                    repository.addMessage(chatId, "assistant", finalContent)
                }
            } catch (error: Exception) {
                val message = error.message ?: "Error inesperado durante la generación"
                updateStreamingMessage("Error: $message", assistantTimestamp, isStreaming = false)
            } finally {
                bridge.onTokenCallback = null
                _isGenerating.value = false
            }
        }
    }

    private fun updateStreamingMessage(
        content: String,
        timestamp: Long,
        isStreaming: Boolean = true
    ) {
        val updatedMessages = _messages.value.toMutableList()
        val lastIndex = updatedMessages.indexOfLast { it.timestamp == timestamp }
        if (lastIndex >= 0) {
            updatedMessages[lastIndex] = ChatMessage(
                role = "assistant",
                content = content,
                isStreaming = isStreaming,
                timestamp = timestamp
            )
            _messages.value = updatedMessages
        }
    }

    fun newChat() {
        if (_isGenerating.value) return
        _currentChatId.value = null
        _messages.value = emptyList()
        // Limpiar contexto nativo sin recargar modelo
        viewModelScope.launch(Dispatchers.IO) {
            bridge.resetContext()
        }
    }

    fun loadChat(chatId: Long) {
        if (_isGenerating.value) return
        viewModelScope.launch {
            _currentChatId.value = chatId
            // Cargar mensajes desde la DB
            val dbMessages = withContext(Dispatchers.IO) {
                repository.getMessagesForChatOnce(chatId)
            }
            _messages.value = dbMessages.map { msg ->
                ChatMessage(
                    role = msg.role,
                    content = msg.content,
                    isStreaming = false,
                    timestamp = msg.timestamp
                )
            }
            withContext(Dispatchers.IO) {
                if (!bridge.restoreContext(buildTranscript(dbMessages))) {
                    bridge.resetContext()
                }
            }
        }
    }

    private fun buildTranscript(messages: List<com.example.modelolocal.data.MessageEntity>): String {
        return buildString {
            append("<|im_start|>system\n")
            append("Responde de forma concisa y útil en español. Usa markdown cuando sea apropiado para formatear tu respuesta.")
            append("<|im_end|>\n")
            messages.forEach { message ->
                append("<|im_start|>${message.role}\n")
                append(message.content)
                append("<|im_end|>\n")
            }
            if (messages.lastOrNull()?.role == "user") {
                append("<|im_start|>assistant\n")
            }
        }
    }

    fun deleteChat(chatId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChat(chatId)
            // Si se elimina el chat actual, limpiar la UI
            if (_currentChatId.value == chatId) {
                _currentChatId.value = null
                _messages.value = emptyList()
                bridge.resetContext()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bridge.unloadModel()
    }
}