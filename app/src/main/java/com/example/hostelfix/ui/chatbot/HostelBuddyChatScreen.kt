package com.example.hostelfix.ui.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostelBuddyChatScreen(
    onNavigateBack: () -> Unit = {},
    onSendPrompt: ((String, onReply: (String) -> Unit) -> Unit)? = null
) {
    val starterPrompts = listOf(
        "How do I report a room issue?",
        "Complaint deletion window?",
        "Hostel rules & timings"
    )

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = "welcome",
                text = "Hey! I'm HostelBuddy. Ask me anything about filing complaints, tracking status, or hostel guidelines.",
                isUser = false
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }

    fun sendMessage(raw: String) {
        val prompt = raw.trim()
        if (prompt.isBlank() || isReplying) return

        messages.add(
            ChatMessage(
                id = "u_${System.nanoTime()}",
                text = prompt,
                isUser = true
            )
        )
        inputText = ""
        isReplying = true

        if (onSendPrompt != null) {
            onSendPrompt(prompt) { reply ->
                messages.add(
                    ChatMessage(
                        id = "b_${System.nanoTime()}",
                        text = reply,
                        isUser = false
                    )
                )
                isReplying = false
            }
        } else {
            val fallbackReply = when {
                prompt.contains("delete", ignoreCase = true) || prompt.contains("window", ignoreCase = true) ->
                    "You can delete a reported issue within 1 hour of submitting it from the Home screen."
                prompt.contains("report", ignoreCase = true) || prompt.contains("complaint", ignoreCase = true) ->
                    "Tap Quick Issue for a fast maintenance entry or File Complaint to pick Maintenance or Academical."
                else ->
                    "Your complaints sync locally to Room first and back up to the cloud automatically."
            }
            messages.add(
                ChatMessage(
                    id = "b_${System.nanoTime()}",
                    text = fallbackReply,
                    isUser = false
                )
            )
            isReplying = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "HB",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "HostelBuddy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isReplying) "Typing..." else "Campus Assistant",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        starterPrompts.forEach { chipText ->
                            SuggestionChip(
                                onClick = { sendMessage(chipText) },
                                label = {
                                    Text(
                                        text = chipText,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask HostelBuddy...") },
                            singleLine = true,
                            enabled = !isReplying,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { sendMessage(inputText) },
                            enabled = !isReplying && inputText.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Send", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (message.isUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            shadowElevation = if (message.isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}