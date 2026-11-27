package com.example.hostelfix

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hostelfix.data.FirebaseIssueSync
import com.example.hostelfix.data.FirebaseProfileSync
import com.example.hostelfix.data.HostelDatabase
import com.example.hostelfix.data.IssueEntity
import com.example.hostelfix.ui.add.AddIssueScreen
import com.example.hostelfix.ui.chatbot.HostelBuddyChatScreen
import com.example.hostelfix.ui.complaint.FileComplaintScreen
import com.example.hostelfix.ui.detail.IssueDetailScreen
import com.example.hostelfix.ui.home.HomeScreen
import com.example.hostelfix.ui.login.LoginScreen
import com.example.hostelfix.ui.profile.UserDetailsScreen
import com.example.hostelfix.ui.splash.SplashScreen
import com.example.hostelfix.ui.theme.HostelFixTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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
                    var activeRegNo by remember { mutableStateOf<String?>(null) }
                    var studentName by remember { mutableStateOf("Student") }
                    var lastSubmitTime by remember { mutableLongStateOf(0L) }

                    var openCount by remember { mutableIntStateOf(0) }
                    var inProgressCount by remember { mutableIntStateOf(0) }
                    var resolvedCount by remember { mutableIntStateOf(0) }
                    var studentIssues by remember { mutableStateOf<List<IssueEntity>>(emptyList()) }

                    LaunchedEffect(activeRegNo) {
                        val cleanRegNo = activeRegNo?.trim()?.takeIf { it.isNotEmpty() }
                        if (cleanRegNo == null) {
                            openCount = 0
                            inProgressCount = 0
                            resolvedCount = 0
                            studentIssues = emptyList()
                            return@LaunchedEffect
                        }

                        try {
                            withTimeoutOrNull(4000L) {
                                profileSync.restoreProfile(cleanRegNo)
                                issueSync.restoreIssuesFromFirebase(cleanRegNo)
                            }
                            dao.getUserByUsername(cleanRegNo)?.let { refreshed ->
                                studentName = refreshed.name
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FirebaseSync", "Restore failed", e)
                        }

                        launch {
                            dao.getOpenCount(cleanRegNo).collectLatest { openCount = it }
                        }
                        launch {
                            dao.getInProgressCount(cleanRegNo).collectLatest { inProgressCount = it }
                        }
                        launch {
                            dao.getResolvedCount(cleanRegNo).collectLatest { resolvedCount = it }
                        }
                        launch {
                            dao.getIssuesForStudent(cleanRegNo).collectLatest { studentIssues = it }
                        }
                    }

                    val deleteIssueAction: (IssueEntity) -> Unit = { issue ->
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
                    }

                    fun createIssue(
                        title: String,
                        desc: String,
                        category: String,
                        onDone: () -> Unit
                    ) {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastSubmitTime < 1500L) {
                            onDone()
                            return
                        }
                        lastSubmitTime = now

                        val regNo = activeRegNo?.trim()?.takeIf { it.isNotEmpty() }
                        if (regNo == null) {
                            onDone()
                            return
                        }

                        lifecycleScope.launch {
                            val newIssue = IssueEntity(
                                studentEmail = regNo,
                                title = title,
                                description = desc,
                                category = category,
                                status = "OPEN",
                                timeAgo = "Just now"
                            )
                            val insertedId: Long = dao.insertIssue(newIssue)
                            val savedIssue = if (insertedId > 0L) {
                                newIssue.copy(id = insertedId.toInt())
                            } else {
                                newIssue
                            }
                            try {
                                withTimeoutOrNull(3000L) {
                                    issueSync.backupIssue(savedIssue)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("FirebaseSync", "Backup failed", e)
                            }
                            onDone()
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        enterTransition = { fadeIn(tween(300)) },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(300)) },
                        popExitTransition = { fadeOut(tween(200)) }
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onFinished = {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                dao = dao,
                                profileSync = profileSync,
                                onLoginSuccess = { user ->
                                    activeRegNo = user.username.trim()
                                    studentName = user.name
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                studentEmail = activeRegNo ?: "",
                                studentName = studentName,
                                openCount = openCount,
                                inProgressCount = inProgressCount,
                                resolvedCount = resolvedCount,
                                issues = studentIssues,
                                onIssueClick = { issue ->
                                    navController.navigate("issue_detail/${issue.id}") {
                                        launchSingleTop = true
                                    }
                                },
                                onDeleteIssue = deleteIssueAction,
                                onNavigateToProfile = {
                                    navController.navigate("user_details") {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToChat = {
                                    navController.navigate("chatbot") {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToAdd = {
                                    navController.navigate("add_issue") {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToComplaint = {
                                    navController.navigate("file_complaint") {
                                        launchSingleTop = true
                                    }
                                },
                                onLogout = {
                                    activeRegNo = null
                                    openCount = 0
                                    inProgressCount = 0
                                    resolvedCount = 0
                                    studentIssues = emptyList()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(
                            route = "issue_detail/{issueId}",
                            arguments = listOf(navArgument("issueId") { type = NavType.IntType })
                        ) { entry ->
                            val issueId = entry.arguments?.getInt("issueId") ?: -1
                            IssueDetailScreen(
                                issue = studentIssues.firstOrNull { it.id == issueId },
                                onNavigateBack = { navController.popBackStack() },
                                onDelete = deleteIssueAction
                            )
                        }
                        composable("user_details") {
                            UserDetailsScreen(
                                username = activeRegNo ?: "",
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
                                    createIssue(
                                        title = title,
                                        desc = desc,
                                        category = "MAINTENANCE",
                                        onDone = onDone
                                    )
                                }
                            )
                        }
                        composable("file_complaint") {
                            FileComplaintScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onSubmitComplaint = { title, desc, category, onDone ->
                                    createIssue(
                                        title = title,
                                        desc = desc,
                                        category = category,
                                        onDone = onDone
                                    )
                                }
                            )
                        }
                        composable("chatbot") {
                            HostelBuddyChatScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onSendPrompt = { prompt, onReply ->
                                    val reply = when {
                                        prompt.contains("status", ignoreCase = true) ||
                                                prompt.contains("open", ignoreCase = true) ||
                                                prompt.contains("count", ignoreCase = true) ->
                                            "$studentName, you currently have $openCount open, $inProgressCount in progress, and $resolvedCount resolved issues."
                                        prompt.contains("delete", ignoreCase = true) ||
                                                prompt.contains("window", ignoreCase = true) ->
                                            "You can delete a reported issue within 1 hour of filing from Home or Issue Details."
                                        prompt.contains("report", ignoreCase = true) ||
                                                prompt.contains("complaint", ignoreCase = true) ||
                                                prompt.contains("room", ignoreCase = true) ->
                                            "Use Quick Issue for fast maintenance entries or File Complaint to choose Maintenance or Academical."
                                        else ->
                                            "Your issues and profile save locally in Room and sync to Firebase Realtime Database."
                                    }
                                    onReply(reply)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}