package com.example.hostelfix.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hostelfix.data.FirebaseProfileSync
import com.example.hostelfix.data.GoogleAuthHelper
import com.example.hostelfix.data.HostelDao
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(
    username: String,
    dao: HostelDao,
    profileSync: FirebaseProfileSync = remember(dao) { FirebaseProfileSync(dao) },
    webClientId: String = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com",
    onNavigateBack: () -> Unit,
    onSaved: (updatedName: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val normalizedUser = remember(username) { username.trim().lowercase() }

    var name by remember { mutableStateOf("") }
    var hostelNumber by remember { mutableStateOf("") }
    var roomNumber by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var linkedGoogleEmail by remember { mutableStateOf<String?>(null) }

    var isLinkingGoogle by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val busy = isSaving || isLinkingGoogle

    LaunchedEffect(normalizedUser) {
        if (normalizedUser.isBlank()) return@LaunchedEffect
        val existing = dao.getUserByUsername(normalizedUser) ?: dao.getUserByUsername(username.trim())
        if (existing != null) {
            name = existing.name
            hostelNumber = existing.hostelNumber
            roomNumber = existing.roomNumber
            phoneNumber = existing.phoneNumber
            department = existing.department
            linkedGoogleEmail = existing.linkedGoogleEmail
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Text(
                        text = "Profile Details",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack, enabled = !busy) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Account",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = linkedGoogleEmail ?: "Not linked",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            isLinkingGoogle = true
                            statusMessage = null
                            isError = false

                            scope.launch {
                                val picked = GoogleAuthHelper.pickGoogleAccount(context, webClientId)
                                picked.onSuccess { account ->
                                    val pickedEmail = account.email.trim().lowercase()
                                    linkedGoogleEmail = pickedEmail
                                    if (name.isBlank() && !account.displayName.isNullOrBlank()) {
                                        name = account.displayName.trim()
                                    }

                                    val targetUsername = dao.getUserByUsername(normalizedUser)?.username
                                        ?: username.trim()
                                    val cleanName = name.trim().ifBlank { targetUsername.substringBefore("@") }

                                    dao.updateUserDetails(
                                        username = targetUsername,
                                        name = cleanName,
                                        hostelNumber = hostelNumber.trim(),
                                        roomNumber = roomNumber.trim(),
                                        phoneNumber = phoneNumber.trim(),
                                        department = department.trim(),
                                        linkedGoogleEmail = pickedEmail
                                    )

                                    dao.getUserByUsername(targetUsername)?.let { updated ->
                                        try {
                                            withTimeoutOrNull(5000L) {
                                                profileSync.backupProfile(updated)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("FirebaseProfileSync", "Link sync failed", e)
                                        }
                                    }

                                    statusMessage = "Linked $pickedEmail"
                                    isError = false
                                }.onFailure { err ->
                                    statusMessage = err.message ?: "Could not link Google account."
                                    isError = true
                                }
                                isLinkingGoogle = false
                            }
                        }
                    ) {
                        Text(
                            text = when {
                                isLinkingGoogle -> "Linking..."
                                linkedGoogleEmail == null -> "Link Google"
                                else -> "Change"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hostelNumber,
                        onValueChange = { hostelNumber = it },
                        label = { Text("Hostel Number") },
                        singleLine = true,
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("Room Number") },
                        singleLine = true,
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        singleLine = true,
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (statusMessage != null) {
                Surface(
                    color = if (isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (username.isBlank() || busy) return@Button
                    isSaving = true
                    statusMessage = null
                    isError = false

                    scope.launch {
                        val targetUsername = dao.getUserByUsername(normalizedUser)?.username
                            ?: username.trim()
                        val cleanName = name.trim().ifBlank { targetUsername.substringBefore("@") }
                        val cleanHostel = hostelNumber.trim()
                        val cleanRoom = roomNumber.trim()
                        val cleanPhone = phoneNumber.trim()
                        val cleanDept = department.trim()

                        dao.updateUserDetails(
                            username = targetUsername,
                            name = cleanName,
                            hostelNumber = cleanHostel,
                            roomNumber = cleanRoom,
                            phoneNumber = cleanPhone,
                            department = cleanDept,
                            linkedGoogleEmail = linkedGoogleEmail
                        )

                        val updatedUser = dao.getUserByUsername(targetUsername)
                        if (updatedUser != null) {
                            try {
                                withTimeoutOrNull(5000L) {
                                    profileSync.backupProfile(updatedUser)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("FirebaseProfileSync", "Profile backup failed", e)
                            }
                        }

                        isSaving = false
                        onSaved(cleanName)
                    }
                },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSaving) "Saving..." else "Save Profile",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}