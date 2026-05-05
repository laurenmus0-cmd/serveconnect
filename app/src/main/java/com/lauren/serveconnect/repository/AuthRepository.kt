package com.lauren.serveconnect.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lauren.serveconnect.models.User
import com.lauren.serveconnect.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // REGISTER new user
    fun registerUser(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        role: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // Save user info to Firestore
                val user = User(
                    uid = uid,
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    role = role,
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection(Constants.COLLECTION_USERS)
                    .document(uid)
                    .set(user)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it.message ?: "Failed to save user") }
            }
            .addOnFailureListener { onFailure(it.message ?: "Registration failed") }
    }

    // LOGIN existing user
    fun loginUser(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                firestore.collection(Constants.COLLECTION_USERS)
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val role = document.getString("role") ?: ""
                        onSuccess(role)
                    }
                    .addOnFailureListener { onFailure(it.message ?: "Failed to get user info") }
            }
            .addOnFailureListener { onFailure(it.message ?: "Login failed") }
    }

    fun logoutUser() {
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    // FETCH User Details (Real-time Flow)
    fun getUserDetailsFlow(uid: String): Flow<User?> = callbackFlow {
        val docRef = firestore.collection(Constants.COLLECTION_USERS).document(uid)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Silently handle error or log it
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                trySend(user)
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }

    fun getUserDetails(uid: String, onSuccess: (User) -> Unit, onFailure: (String) -> Unit) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                if (user != null) {
                    onSuccess(user)
                } else {
                    onFailure("User not found")
                }
            }
            .addOnFailureListener { onFailure(it.message ?: "Failed to fetch user") }
    }
}
