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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.lauren.serveconnect.ui.chat.ChatScreen
import com.lauren.serveconnect.ui.home.HomeScreen
import com.lauren.serveconnect.ui.login.LoginScreen
import com.lauren.serveconnect.ui.profile.ProfileScreen
import com.lauren.serveconnect.ui.provider.ServiceProviderScreen
import com.lauren.serveconnect.ui.signup.SignUpScreen
import com.lauren.serveconnect.ui.splash.SplashScreen
import com.lauren.serveconnect.ui.theme.ServeConnectTheme
import com.lauren.serveconnect.viewmodel.AuthViewModel
import com.lauren.serveconnect.viewmodel.ChatViewModel
import com.lauren.serveconnect.viewmodel.ServiceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ServeConnectTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val auth = FirebaseAuth.getInstance()
                
                val authViewModel: AuthViewModel = viewModel()
                val serviceViewModel: ServiceViewModel = viewModel()
                val chatViewModel: ChatViewModel = viewModel()

                val userDetails by authViewModel.userDetails.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = "splash") {
                            composable("splash") {
                                SplashScreen(onTimeout = {
                                    if (auth.currentUser != null) {
                                        authViewModel.fetchUserDetails()
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
                                                authViewModel.fetchUserDetails()
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
                                                    "fullName" to name,
                                                    "email" to email,
                                                    "phone" to phone,
                                                    "role" to role,
                                                    "uid" to userId,
                                                    "createdAt" to System.currentTimeMillis()
                                                )
                                                
                                                if (userId != null) {
                                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                    db.collection("users").document(userId)
                                                        .set(userMap)
                                                        .addOnSuccessListener {
                                                            authViewModel.fetchUserDetails()
                                                            navController.navigate("home") {
                                                                popUpTo("signup") { inclusive = true }
                                                            }
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
                                        navController.navigate("post_service")
                                    },
                                    onChatClick = { service ->
                                        navController.navigate("chat/${service.providerId}/${service.providerName}")
                                    },
                                    onProfileClick = {
                                        navController.navigate("profile")
                                    },
                                    onMessagesClick = {
                                        navController.navigate("chat_list")
                                    },
                                    viewModel = serviceViewModel
                                )
                            }
                            composable("chat_list") {
                                com.lauren.serveconnect.ui.chat.ChatListScreen(
                                    currentUserId = auth.currentUser?.uid ?: "",
                                    onChatClick = { id, name ->
                                        navController.navigate("chat/$id/$name")
                                    },
                                    onBackClick = { navController.popBackStack() },
                                    viewModel = chatViewModel
                                )
                            }
                            composable("post_service") {
                                ServiceProviderScreen(
                                    providerId = auth.currentUser?.uid ?: "",
                                    providerName = userDetails?.fullName ?: "Anonymous",
                                    onBackClick = { navController.popBackStack() },
                                    onPostService = { servicePost ->
                                        serviceViewModel.postService(servicePost) { success, error ->
                                            if (success) {
                                                Toast.makeText(context, "Job has been posted successfully!", Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            } else {
                                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                            composable("profile") {
                                ProfileScreen(
                                    viewModel = authViewModel,
                                    onBackClick = { navController.popBackStack() },
                                    onLogoutClick = {
                                        authViewModel.logoutUser()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(
                                route = "chat/{otherUserId}/{otherUserName}",
                                arguments = listOf(
                                    navArgument("otherUserId") { type = NavType.StringType },
                                    navArgument("otherUserName") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
                                val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
                                ChatScreen(
                                    currentUserId = auth.currentUser?.uid ?: "",
                                    otherUserId = otherUserId,
                                    otherUserName = otherUserName,
                                    onBackClick = { navController.popBackStack() },
                                    viewModel = chatViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
