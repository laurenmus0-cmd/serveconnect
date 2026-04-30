package com.lauren.serveconnect

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lauren.serveconnect.ui.home.HomeScreen
import com.lauren.serveconnect.ui.login.LoginScreen
import com.lauren.serveconnect.ui.signup.SignUpScreen
import com.lauren.serveconnect.ui.splash.SplashScreen
import com.lauren.serveconnect.ui.theme.ServeConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ServeConnectTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val auth = FirebaseAuth.getInstance()
                val db = FirebaseFirestore.getInstance()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = "splash") {
                            composable("splash") {
                                SplashScreen(onTimeout = {
                                    if (auth.currentUser != null) {
                                        navController.navigate("home") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("login") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                })
                            }
                            composable("login") {
                                LoginScreen(
                                    onLoginClick = { email, password ->
                                        auth.signInWithEmailAndPassword(email, password)
                                            .addOnSuccessListener {
                                                navController.navigate("home") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    },
                                    onSignUpClick = {
                                        navController.navigate("signup")
                                    }
                                )
                            }
                            composable("signup") {
                                SignUpScreen(
                                    onSignUpClick = { name, email, phone, password, role ->
                                        auth.createUserWithEmailAndPassword(email, password)
                                            .addOnSuccessListener { result ->
                                                val userId = result.user?.uid
                                                val userMap = hashMapOf(
                                                    "name" to name,
                                                    "email" to email,
                                                    "phone" to phone,
                                                    "role" to role,
                                                    "uid" to userId
                                                )
                                                
                                                if (userId != null) {
                                                    db.collection("users").document(userId)
                                                        .set(userMap)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(context, "Signed up successfully!", Toast.LENGTH_LONG).show()
                                                            navController.navigate("home") {
                                                                popUpTo("signup") { inclusive = true }
                                                            }
                                                        }
                                                        .addOnFailureListener {
                                                            Toast.makeText(context, "Failed to save user info: ${it.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Sign up failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    },
                                    onLoginClick = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                            composable("home") {
                                HomeScreen(
                                    onPostServiceClick = {
                                        // TODO: Navigate to Post Service screen
                                    },
                                    onChatClick = { service ->
                                        // TODO: Navigate to Chat screen
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
