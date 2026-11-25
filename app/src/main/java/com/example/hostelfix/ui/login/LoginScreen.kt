package com.example.hostelfix.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.hostelfix.data.FirebaseProfileSync
import com.example.hostelfix.data.GoogleAuthHelper
import com.example.hostelfix.data.HostelDao
import com.example.hostelfix.data.PasswordHasher
import com.example.hostelfix.data.UserEntity
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun LoginScreen(
    dao: HostelDao,
    profileSync: FirebaseProfileSync = remember(dao) { FirebaseProfileSync(dao) },
    webClientId: String = "2475045038-ginqkrmuta8k629jid8tklepaoufre56.apps.googleusercontent.com",
    onLoginSuccess: (UserEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var hostelNumber by remember { mutableStateOf("") }
    var roomNumber by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "HF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "HostelFix",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isRegisterMode) {
                            "Register with your campus email"
                        } else {
                            "Campus maintenance & issue desk"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isRegisterMode,
                            onClick = {
                                if (!isLoading) {
                                    isRegisterMode = false
                                    errorMessage = null
                                }
                            },
                            label = { Text("Login") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isRegisterMode,
                            onClick = {
                                if (!isLoading) {
                                    isRegisterMode = true
                                    errorMessage = null
                                }
                            },
                            label = { Text("Create Account") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text("Student Email / Username") },
                        singleLine = true,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isRegisterMode) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = hostelNumber,
                                onValueChange = { hostelNumber = it },
                                label = { Text("Hostel") },
                                singleLine = true,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = roomNumber,
                                onValueChange = { roomNumber = it },
                                label = { Text("Room") },
                                singleLine = true,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Department") },
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cleanUser = username.trim()
                            if (cleanUser.isBlank() || password.isBlank()) {
                                errorMessage = "Enter both username and password."
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null

                            scope.launch {
                                try {
                                    if (isRegisterMode) {
                                        val existing = dao.getUserByUsername(cleanUser)
                                        if (existing != null) {
                                            errorMessage = "Account already exists. Switch to Login."
                                            isLoading = false
                                            return@launch
                                        }

                                        val newUser = UserEntity(
                                            username = cleanUser,
                                            password = PasswordHasher.hashPassword(password),
                                            name = name.trim().ifBlank { cleanUser.substringBefore("@") },
                                            hostelNumber = hostelNumber.trim(),
                                            roomNumber = roomNumber.trim(),
                                            phoneNumber = phoneNumber.trim(),
                                            department = department.trim()
                                        )
                                        dao.insertUser(newUser)

                                        try {
                                            withTimeoutOrNull(3000L) {
                                                profileSync.backupProfile(newUser)
                                            }
                                        } catch (_: Exception) {
                                            // Saved locally; will sync later
                                        }

                                        isLoading = false
                                        onLoginSuccess(newUser)
                                    } else {
                                        val user = dao.getUserByUsername(cleanUser)
                                        if (user == null || !PasswordHasher.verifyPassword(password, user.password)) {
                                            errorMessage = "Invalid username or password."
                                            isLoading = false
                                            return@launch
                                        }

                                        try {
                                            withTimeoutOrNull(3000L) {
                                                profileSync.restoreProfile(user.username)
                                            }
                                        } catch (_: Exception) {
                                            // Offline fallback: keep local profile
                                        }

                                        val refreshedUser = dao.getUserByUsername(user.username) ?: user
                                        isLoading = false
                                        onLoginSuccess(refreshedUser)
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Authentication failed."
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = when {
                                isLoading && isRegisterMode -> "Creating account..."
                                isLoading -> "Signing in..."
                                isRegisterMode -> "Create Account"
                                else -> "Login"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (isLoading) return@OutlinedButton
                            isLoading = true
                            errorMessage = null

                            scope.launch {
                                val picked = GoogleAuthHelper.pickGoogleAccount(context, webClientId)
                                picked.onSuccess { account ->
                                    val cleanEmail = account.email.trim()
                                    val existing = dao.getUserByUsername(cleanEmail)
                                    val activeUser = if (existing != null) {
                                        existing.copy(
                                            name = existing.name.ifBlank {
                                                account.displayName ?: cleanEmail.substringBefore("@")
                                            },
                                            linkedGoogleEmail = cleanEmail,
                                            photoUrl = account.photoUrl ?: existing.photoUrl
                                        ).also { updated ->
                                            dao.updateUserDetails(
                                                username = updated.username,
                                                name = updated.name,
                                                hostelNumber = updated.hostelNumber,
                                                roomNumber = updated.roomNumber,
                                                phoneNumber = updated.phoneNumber,
                                                department = updated.department,
                                                linkedGoogleEmail = updated.linkedGoogleEmail
                                            )
                                        }
                                    } else {
                                        UserEntity(
                                            username = cleanEmail,
                                            password = PasswordHasher.hashPassword(UUID.randomUUID().toString()),
                                            name = account.displayName?.takeIf { it.isNotBlank() }
                                                ?: cleanEmail.substringBefore("@"),
                                            linkedGoogleEmail = cleanEmail,
                                            photoUrl = account.photoUrl
                                        ).also { created ->
                                            dao.insertUser(created)
                                        }
                                    }

                                    try {
                                        withTimeoutOrNull(3000L) {
                                            profileSync.restoreProfile(activeUser.username)
                                            profileSync.backupProfile(
                                                dao.getUserByUsername(activeUser.username) ?: activeUser
                                            )
                                        }
                                    } catch (_: Exception) {
                                        // Saved locally; will sync when online
                                    }

                                    isLoading = false
                                    onLoginSuccess(dao.getUserByUsername(activeUser.username) ?: activeUser)
                                }.onFailure { err ->
                                    errorMessage = err.message ?: "Google sign-in failed."
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Continue with Google", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Local-first Room + Cloud sync enabled",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}