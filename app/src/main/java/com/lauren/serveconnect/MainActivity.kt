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
import androidx.compose.runtime.LaunchedEffect
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
import com.lauren.serveconnect.viewmodel.AuthState
import com.lauren.serveconnect.viewmodel.AuthViewModel
import com.lauren.serveconnect.viewmodel.ChatViewModel
import com.lauren.serveconnect.viewmodel.ServiceViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

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
                                        authViewModel.loginUser(email, password)
                                    },
                                    onSignUpClick = {
                                        navController.navigate("signup")
                                    }
                                )

                                val authState by authViewModel.authState.collectAsState()
                                LaunchedEffect(authState) {
                                    if (authState is AuthState.Success) {
                                        authViewModel.fetchUserDetails()
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else if (authState is AuthState.Error) {
                                        Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            composable("signup") {
                                SignUpScreen(
                                    onSignUpClick = { name, email, phone, password, role ->
                                        authViewModel.registerUser(email, password, name, phone, role)
                                    },
                                    onLoginClick = {
                                        navController.popBackStack()
                                    }
                                )
                                
                                // Observe auth state to navigate on success
                                val authState by authViewModel.authState.collectAsState()
                                LaunchedEffect(authState) {
                                    if (authState is AuthState.Success) {
                                        navController.navigate("home") {
                                            popUpTo("signup") { inclusive = true }
                                        }
                                    } else if (authState is AuthState.Error) {
                                        Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            composable("home") {
                                HomeScreen(
                                    currentUserId = auth.currentUser?.uid ?: "",
                                    onPostServiceClick = {
                                        navController.navigate("post_service")
                                    },
                                    onEditServiceClick = { service ->
                                        navController.navigate("post_service?serviceId=${service.id}")
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
                            composable(
                                "post_service?serviceId={serviceId}",
                                arguments = listOf(navArgument("serviceId") { 
                                    nullable = true
                                    defaultValue = null 
                                })
                            ) { backStackEntry ->
                                val serviceId = backStackEntry.arguments?.getString("serviceId")
                                val services by serviceViewModel.services.collectAsState()
                                val serviceToEdit = services.find { it.id == serviceId }

                                ServiceProviderScreen(
                                    serviceToEdit = serviceToEdit,
                                    providerId = auth.currentUser?.uid ?: "",
                                    providerName = userDetails?.fullName ?: "Anonymous",
                                    onBackClick = { navController.popBackStack() },
                                    onPostService = { servicePost ->
                                        val callback: (Boolean, String?) -> Unit = { success, error ->
                                            if (success) {
                                                val msg = if (serviceId != null) "Updated successfully!" else "Posted successfully!"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            } else {
                                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        if (serviceId != null) {
                                            serviceViewModel.updateService(servicePost, callback)
                                        } else {
                                            serviceViewModel.postService(servicePost, callback)
                                        }
                                    }
                                )
                            }
                            composable("profile") {
                                ProfileScreen(
                                    currentUserId = auth.currentUser?.uid ?: "",
                                    authViewModel = authViewModel,
                                    serviceViewModel = serviceViewModel,
                                    onBackClick = { navController.popBackStack() },
                                    onEditServiceClick = { service ->
                                        navController.navigate("post_service?serviceId=${service.id}")
                                    },
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
