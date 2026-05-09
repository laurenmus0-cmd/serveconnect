package com.lauren.serveconnect.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lauren.serveconnect.model.SavedService
import com.lauren.serveconnect.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SavedServiceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val savedCollection = firestore.collection(Constants.COLLECTION_SAVED_SERVICES)

    fun getSavedServices(userId: String): Flow<List<String>> = callbackFlow {
        val subscription = savedCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val serviceIds = snapshot?.documents?.mapNotNull { it.getString("serviceId") } ?: emptyList()
                trySend(serviceIds)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun toggleSaveService(userId: String, serviceId: String) {
        val existing = savedCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("serviceId", serviceId)
            .get()
            .await()

        if (existing.isEmpty) {
            val savedService = SavedService(
                userId = userId,
                serviceId = serviceId,
                timestamp = System.currentTimeMillis()
            )
            savedCollection.add(savedService).await()
        } else {
            for (doc in existing.documents) {
                doc.reference.delete().await()
            }
        }
    }
}
