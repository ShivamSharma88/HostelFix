package com.example.hostelfix

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hostelfix.data.FirebaseIssueSync
import com.example.hostelfix.data.FirebaseProfileSync
import com.example.hostelfix.data.HostelDatabase
import com.example.hostelfix.data.IssueEntity
import com.example.hostelfix.ui.add.AddIssueScreen
import com.example.hostelfix.ui.chatbot.HostelBuddyChatScreen
import com.example.hostelfix.ui.complaint.FileComplaintScreen
import com.example.hostelfix.ui.home.HomeScreen
import com.example.hostelfix.ui.login.LoginScreen
import com.example.hostelfix.ui.profile.UserDetailsScreen
import com.example.hostelfix.ui.theme.HostelFixTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = HostelDatabase.getDatabase(applicationContext)
        val dao = database.hostelDao()

        setContent {
            HostelFixTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val issueSync = remember { FirebaseIssueSync(dao) }
                    val profileSync = remember { FirebaseProfileSync(dao) }
                    var loggedInEmail by remember { mutableStateOf<String?>(null) }
                    var studentName by remember { mutableStateOf("Student") }
                    var lastSubmitTime by remember { mutableLongStateOf(0L) }

                    var openCount by remember { mutableIntStateOf(0) }
                    var inProgressCount by remember { mutableIntStateOf(0) }
                    var resolvedCount by remember { mutableIntStateOf(0) }
                    var studentIssues by remember { mutableStateOf<List<IssueEntity>>(emptyList()) }

                    LaunchedEffect(loggedInEmail) {
                        loggedInEmail?.let { email ->
                            try {
                                withTimeoutOrNull(3000L) {
                                    profileSync.restoreProfile(email)
                                    issueSync.restoreIssuesFromFirebase(email)
                                }
                                dao.getUserByUsername(email)?.let { refreshed ->
                                    studentName = refreshed.name
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("FirebaseSync", "Restore failed", e)
                            }

                            launch {
                                dao.getOpenCount(email).collectLatest { openCount = it }
                            }
                            launch {
                                dao.getInProgressCount(email).collectLatest { inProgressCount = it }
                            }
                            launch {
                                dao.getResolvedCount(email).collectLatest { resolvedCount = it }
                            }
                            launch {
                                dao.getIssuesForStudent(email).collectLatest { studentIssues = it }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = if (loggedInEmail == null) "login" else "home"
                    ) {
                        composable("login") {
                            LoginScreen(
                                dao = dao,
                                profileSync = profileSync,
                                onLoginSuccess = { user ->
                                    loggedInEmail = user.username
                                    studentName = user.name
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                studentEmail = loggedInEmail ?: "",
                                studentName = studentName,
                                openCount = openCount,
                                inProgressCount = inProgressCount,
                                resolvedCount = resolvedCount,
                                issues = studentIssues,
                                onDeleteIssue = { issue ->
                                    if (issue.canBeDeleted()) {
                                        lifecycleScope.launch {
                                            dao.deleteIssue(issue)
                                            try {
                                                withTimeoutOrNull(3000L) {
                                                    issueSync.deleteIssueFromFirebase(issue)
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("FirebaseSync", "Delete failed", e)
                                            }
                                        }
                                    }
                                },
                                onNavigateToProfile = { navController.navigate("user_details") },
                                onNavigateToChat = { navController.navigate("chatbot") },
                                onNavigateToAdd = { navController.navigate("add_issue") },
                                onNavigateToComplaint = { navController.navigate("file_complaint") },
                                onLogout = {
                                    loggedInEmail = null
                                    openCount = 0
                                    inProgressCount = 0
                                    resolvedCount = 0
                                    studentIssues = emptyList()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("user_details") {
                            UserDetailsScreen(
                                username = loggedInEmail ?: "",
                                dao = dao,
                                profileSync = profileSync,
                                onNavigateBack = { navController.popBackStack() },
                                onSaved = { updatedName ->
                                    studentName = updatedName
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("add_issue") {
                            AddIssueScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onSubmit = { title, desc, onDone ->
                                    val now = SystemClock.elapsedRealtime()
                                    if (now - lastSubmitTime < 1500L) {
                                        onDone()
                                        return@AddIssueScreen
                                    }
                                    lastSubmitTime = now

                                    val email = loggedInEmail
                                    if (email == null) {
                                        onDone()
                                        return@AddIssueScreen
                                    }

                                    lifecycleScope.launch {
                                        val newIssue = IssueEntity(
                                            studentEmail = email,
                                            title = title,
                                            description = desc,
                                            category = "MAINTENANCE",
                                            status = "OPEN",
                                            timeAgo = "Just now"
                                        )
                                        dao.insertIssue(newIssue)
                                        try {
                                            withTimeoutOrNull(3000L) {
                                                issueSync.backupIssue(newIssue)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("FirebaseSync", "Backup failed", e)
                                        }
                                        onDone()
                                    }
                                }
                            )
                        }
                        composable("file_complaint") {
                            FileComplaintScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onSubmitComplaint = { title, desc, category, onDone ->
                                    val now = SystemClock.elapsedRealtime()
                                    if (now - lastSubmitTime < 1500L) {
                                        onDone()
                                        return@FileComplaintScreen
                                    }
                                    lastSubmitTime = now

                                    val email = loggedInEmail
                                    if (email == null) {
                                        onDone()
                                        return@FileComplaintScreen
                                    }

                                    lifecycleScope.launch {
                                        val newIssue = IssueEntity(
                                            studentEmail = email,
                                            title = title,
                                            description = desc,
                                            category = category,
                                            status = "OPEN",
                                            timeAgo = "Just now"
                                        )
                                        dao.insertIssue(newIssue)
                                        try {
                                            withTimeoutOrNull(3000L) {
                                                issueSync.backupIssue(newIssue)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("FirebaseSync", "Backup failed", e)
                                        }
                                        onDone()
                                    }
                                }
                            )
                        }
                        composable("chatbot") {
                            HostelBuddyChatScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}