package com.lauren.serveconnect.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lauren.serveconnect.models.User
import com.lauren.serveconnect.utils.Constants

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
        onSuccess: (String) -> Unit,   // returns role so we know which dashboard to open
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // Get user role from Firestore
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

    // LOGOUT
    fun logoutUser() {
        auth.signOut()
    }

    // CHECK if user is already logged in
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // GET current user ID
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // FETCH User Details
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