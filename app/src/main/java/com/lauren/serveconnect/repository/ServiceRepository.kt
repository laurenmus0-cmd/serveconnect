package com.lauren.serveconnect.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lauren.serveconnect.model.ServicePost
import com.lauren.serveconnect.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ServiceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val servicesCollection = firestore.collection(Constants.COLLECTION_SERVICES)

    fun getServices(): Flow<List<ServicePost>> = callbackFlow {
        val subscription = servicesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val services = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ServicePost::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(services)
        }
        awaitClose { subscription.remove() }
    }

    fun postService(service: ServicePost, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        servicesCollection.add(service)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateService(service: ServicePost, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (service.id.isEmpty()) {
            onFailure(Exception("Service ID is empty"))
            return
        }
        servicesCollection.document(service.id).set(service)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
