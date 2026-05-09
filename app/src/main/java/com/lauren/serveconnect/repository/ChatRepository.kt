package com.lauren.serveconnect.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lauren.serveconnect.model.ChatMessage
import com.lauren.serveconnect.model.ChatSummary
import com.lauren.serveconnect.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun getAllUserMessages(userId: String): Flow<List<ChatMessage>> = callbackFlow {
        // Listen to all messages where the user is either sender or receiver
        val senderQuery = firestore.collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("senderId", userId)
        
        val receiverQuery = firestore.collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("receiverId", userId)

        // For simplicity in a small app, we'll merge them. 
        // In a production app, we'd use a separate 'conversations' collection.
        val subscription = firestore.collection(Constants.COLLECTION_MESSAGES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                val filtered = messages.filter { it.senderId == userId || it.receiverId == userId }
                trySend(filtered)
            }
        
        awaitClose { subscription.remove() }
    }

    suspend fun getUserName(userId: String): String {
        return try {
            val doc = firestore.collection(Constants.COLLECTION_USERS).document(userId).get().await()
            doc.getString("fullName") ?: "Unknown User"
        } catch (e: Exception) {
            "Unknown User"
        }
    }

    fun getMessages(currentUserId: String, otherUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        // Using a simpler query and filtering/sorting in memory to avoid "Missing Index" crashes
        val query = firestore.collection(Constants.COLLECTION_MESSAGES)
            .whereIn("senderId", listOf(currentUserId, otherUserId))

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
            }?.filter { 
                (it.senderId == currentUserId && it.receiverId == otherUserId) || 
                (it.senderId == otherUserId && it.receiverId == currentUserId)
            }?.sortedBy { it.timestamp } ?: emptyList()

            trySend(messages)
        }
        awaitClose { subscription.remove() }
    }

    fun sendMessage(message: ChatMessage, onComplete: (Boolean) -> Unit) {
        firestore.collection(Constants.COLLECTION_MESSAGES)
            .add(message)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun markMessagesAsRead(currentUserId: String, otherUserId: String) {
        firestore.collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("senderId", otherUserId)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "read", true)
                }
                batch.commit()
            }
    }

    fun getUnreadCountFlow(userId: String): Flow<Int> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { subscription.remove() }
    }
}
