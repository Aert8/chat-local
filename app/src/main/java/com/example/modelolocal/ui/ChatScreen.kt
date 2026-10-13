package com.example.modelolocal.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.modelolocal.ChatMessage
import com.example.modelolocal.ChatViewModel
import com.example.modelolocal.ModelState
import com.example.modelolocal.ui.theme.ChatColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {
    val modelState by viewModel.modelState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val currentChatId by viewModel.currentChatId.collectAsState()
    val currentChatTitle = chatHistory
        .firstOrNull { it.id == currentChatId }
        ?.title
        ?.takeIf { it.isNotBlank() }
        ?: "LFM Local"

    var promptInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Auto-scroll al último mensaje
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HistoryDrawerContent(
                chats = chatHistory,
                currentChatId = currentChatId,
                onNewChat = {
                    viewModel.newChat()
                    scope.launch { drawerState.close() }
                },
                onSelectChat = { chatId ->
                    viewModel.loadChat(chatId)
                    scope.launch { drawerState.close() }
                },
                onDeleteChat = { chatId ->
                    viewModel.deleteChat(chatId)
                }
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentChatTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = when (modelState) {
                                    is ModelState.Loading -> "Cargando modelo..."
                                    is ModelState.Ready -> "Listo"
                                    is ModelState.Error -> "Error"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (modelState) {
                                    is ModelState.Loading -> ChatColors.StatusLoading
                                    is ModelState.Ready -> ChatColors.StatusReady
                                    is ModelState.Error -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Historial",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Área de mensajes
                Box(modifier = Modifier.weight(1f)) {
                    if (messages.isEmpty() && modelState is ModelState.Ready) {
                        // Estado vacío
                        EmptyStateView()
                    } else if (modelState is ModelState.Loading) {
                        LoadingModelView()
                    } else if (modelState is ModelState.Error) {
                        ErrorView(message = (modelState as ModelState.Error).message)
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(messages, key = { "${it.role}_${it.timestamp}" }) { msg ->
                                MessageBubble(message = msg)
                            }
                        }
                    }
                }

                // Barra de entrada
                InputBar(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    onSend = {
                        if (promptInput.isNotBlank()) {
                            viewModel.sendPrompt(promptInput.trim())
                            promptInput = ""
                        }
                    },
                    enabled = modelState is ModelState.Ready && !isGenerating
                )
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Escribe tu mensaje...",
                        color = ChatColors.MutedText
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ChatColors.InputBackground,
                    unfocusedContainerColor = ChatColors.InputBackground,
                    disabledContainerColor = ChatColors.InputBackground.copy(alpha = 0.5f),
                    focusedBorderColor = ChatColors.Accent.copy(alpha = 0.4f),
                    unfocusedBorderColor = ChatColors.InputBorder,
                    cursorColor = ChatColors.Accent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = enabled,
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ChatColors.Accent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = ChatColors.Accent.copy(alpha = 0.3f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                ),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar"
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Que chow",
                style = MaterialTheme.typography.headlineLarge
            )
            //Spacer(modifier = Modifier.height(16.dp))
            //Text(
            //    text = "¿En qué puedo ayudarte?",
            //    style = MaterialTheme.typography.titleLarge,
            //    color = MaterialTheme.colorScheme.onBackground,
            //    fontWeight = FontWeight.SemiBold
            //)
            //Spacer(modifier = Modifier.height(8.dp))
            //Text(
            //    text = "Escribe tu pregunta para empezar una conversación",
            //    style = MaterialTheme.typography.bodyMedium,
            //    color = ChatColors.MutedText
            //)
        }
    }
}

@Composable
private fun LoadingModelView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Pulse animation
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ChatColors.Accent.copy(alpha = alpha * 0.3f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = ChatColors.Accent,
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Cargando modelo LFM...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Esto puede tardar unos segundos",
                style = MaterialTheme.typography.bodyMedium,
                color = ChatColors.MutedText
            )
        }
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error al cargar el modelo",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ChatColors.MutedText
            )
        }
    }
}
