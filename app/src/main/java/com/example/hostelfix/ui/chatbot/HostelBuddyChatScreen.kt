package com.example.hostelfix.ui.chatbot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatbotViewModel : ViewModel() {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "AIzaSyCGAIRSdazNXi9fVrOqEqYBXJCBZVkmoIM"
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Hi! I'm HostelBuddy. How can I help you with hostel queries today?", false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = ChatMessage(prompt, true)
        _messages.value += userMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(
                    "You are HostelBuddy, a helpful assistant for hostel students. Answer concisely about hostel rules, timings, and maintenance: $prompt"
                )
                val botReplyText = response.text ?: "I couldn't generate a response right now."
                _messages.value += ChatMessage(botReplyText, false)
            } catch (e: Exception) {
                _messages.value += ChatMessage(
                                    "Error: ${e.localizedMessage ?: "Failed to reach AI service"}",
                                    false
                                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostelBuddyChatScreen(viewModel: ChatbotViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("HostelBuddy AI Assistant") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (message.isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = message.text,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about Wi-Fi, curfews, rules...") },
                    singleLine = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val toSend = inputQuery
                        inputQuery = ""
                        viewModel.sendMessage(toSend)
                    },
                    enabled = !isLoading && inputQuery.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        }
    }
}
