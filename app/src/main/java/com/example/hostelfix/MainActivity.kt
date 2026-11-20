package com.example.hostelfix

import android.os.Bundle
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
import com.example.hostelfix.data.HostelDatabase
import com.example.hostelfix.data.IssueEntity
import com.example.hostelfix.data.UserEntity
import com.example.hostelfix.ui.add.AddIssueScreen
import com.example.hostelfix.ui.chatbot.HostelBuddyChatScreen
import com.example.hostelfix.ui.home.HomeScreen
import com.example.hostelfix.ui.login.LoginScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = HostelDatabase.getDatabase(applicationContext)
        val dao = database.hostelDao()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var loggedInEmail by remember { mutableStateOf<String?>(null) }
                    var studentName by remember { mutableStateOf("Student") }

                    var openCount by remember { mutableIntStateOf(0) }
                    var inProgressCount by remember { mutableIntStateOf(0) }
                    var resolvedCount by remember { mutableIntStateOf(0) }
                    var studentIssues by remember { mutableStateOf<List<IssueEntity>>(emptyList()) }

                    // Collect reactive Room flows whenever loggedInEmail changes
                    LaunchedEffect(loggedInEmail) {
                        loggedInEmail?.let { email ->
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
                                onLoginSuccess = { email, name ->
                                    lifecycleScope.launch {
                                        dao.insertUser(UserEntity(email, name, null))
                                        loggedInEmail = email
                                        studentName = name
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
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
                                onNavigateToChat = { navController.navigate("chatbot") },
                                onNavigateToAdd = { navController.navigate("add_issue") },
                                onLogout = {
                                    loggedInEmail = null
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("add_issue") {
                            AddIssueScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onSubmit = { title, desc ->
                                    loggedInEmail?.let { email ->
                                        lifecycleScope.launch {
                                            dao.insertIssue(
                                                IssueEntity(
                                                    studentEmail = email,
                                                    title = title,
                                                    description = desc,
                                                    status = "OPEN",
                                                    timeAgo = "Just now"
                                                )
                                            )
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            )
                        }
                        composable("chatbot") {
                            HostelBuddyChatScreen()
                        }
                    }
                }
            }
        }
    }
}