package com.lauren.serveconnect.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lauren.serveconnect.viewmodel.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedServicesScreen(
    currentUserId: String,
    onBackClick: () -> Unit,
    onChatClick: (com.lauren.serveconnect.model.ServicePost) -> Unit,
    onEditServiceClick: (com.lauren.serveconnect.model.ServicePost) -> Unit,
    viewModel: ServiceViewModel
) {
    val services by viewModel.services.collectAsState()
    val savedServiceIds by viewModel.savedServiceIds.collectAsState()

    val savedServices = remember(services, savedServiceIds) {
        services.filter { savedServiceIds.contains(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Services", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (savedServices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No saved services yet", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(savedServices) { service ->
                        ServiceItem(
                            service = service,
                            isOwner = service.providerId == currentUserId,
                            isSaved = true,
                            onChatClick = { onChatClick(service) },
                            onEditClick = { onEditServiceClick(service) },
                            onSaveToggle = { viewModel.toggleSaveService(currentUserId, service.id) }
                        )
                    }
                }
            }
        }
    }
}
