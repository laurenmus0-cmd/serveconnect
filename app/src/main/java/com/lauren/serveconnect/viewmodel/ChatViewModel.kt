package com.lauren.serveconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lauren.serveconnect.model.ChatMessage
import com.lauren.serveconnect.model.ChatSummary
import com.lauren.serveconnect.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _chatList = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chatList: StateFlow<List<ChatSummary>> = _chatList

    fun fetchChatList(currentUserId: String) {
        viewModelScope.launch {
            repository.getAllUserMessages(currentUserId).collect { allMessages ->
                // Group messages by the "other user"
                val conversations = allMessages.groupBy { 
                    if (it.senderId == currentUserId) it.receiverId else it.senderId 
                }

                val summaries = conversations.map { (otherId, msgs) ->
                    async {
                        val lastMsg = msgs.maxByOrNull { it.timestamp }
                        val name = repository.getUserName(otherId)
                        ChatSummary(
                            otherUserId = otherId,
                            otherUserName = name,
                            lastMessage = lastMsg?.message ?: "",
                            timestamp = lastMsg?.timestamp ?: 0L
                        )
                    }
                }.awaitAll().sortedByDescending { it.timestamp }
                
                _chatList.value = summaries
            }
        }
    }

    fun fetchMessages(currentUserId: String, otherUserId: String) {
        viewModelScope.launch {
            repository.getMessages(currentUserId, otherUserId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(senderId: String, receiverId: String, text: String) {
        if (text.isBlank()) return
        
        val message = ChatMessage(
            senderId = senderId,
            receiverId = receiverId,
            message = text,
            timestamp = System.currentTimeMillis()
        )
        
        repository.sendMessage(message) { /* Handle success/failure if needed */ }
    }
}
