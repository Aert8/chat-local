package com.example.modelolocal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.modelolocal.ChatMessage
import com.example.modelolocal.ui.theme.ChatColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) +
                slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = tween(200)
                )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Header: label de rol + timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUser) "tú" else "LFM",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUser) ChatColors.RoleLabelUser
                            else ChatColors.RoleLabelAssistant
                )
                if (message.timestamp > 0) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ChatColors.MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Contenido del mensaje
            if (isUser) {
                // Usuario: contenedor sutil diferenciado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ChatColors.UserMessageBackground)
                        .padding(14.dp)
                ) {
                    Text(
                        text = message.content,
                        color = ChatColors.UserMessageText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Asistente: sin burbuja, markdown fluido a ancho completo
                if (message.isStreaming && message.content.isEmpty()) {
                    TypingIndicator()
                } else {
                    MarkdownText(
                        content = message.content,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (message.isStreaming) {
                        Spacer(modifier = Modifier.height(6.dp))
                        StreamingCursor()
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = ChatColors.DividerColor,
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
fun StreamingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming_cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )
    Box(
        modifier = Modifier
            .size(width = 8.dp, height = 14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(ChatColors.Accent.copy(alpha = alpha))
    )
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ChatColors.Accent.copy(alpha = alpha))
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
