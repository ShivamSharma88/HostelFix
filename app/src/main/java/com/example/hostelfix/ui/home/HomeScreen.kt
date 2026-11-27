package com.example.hostelfix.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hostelfix.data.IssueEntity
import com.example.hostelfix.ui.components.CategoryBadge
import com.example.hostelfix.ui.components.EmptyState
import com.example.hostelfix.ui.components.FadeSlideIn
import com.example.hostelfix.ui.components.ResolutionProgress
import com.example.hostelfix.ui.components.StatCard
import com.example.hostelfix.ui.components.StatusChip
import com.example.hostelfix.ui.components.relativeTime
import com.example.hostelfix.ui.components.statusColor
import com.example.hostelfix.ui.theme.Brand
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    studentEmail: String,
    studentName: String,
    openCount: Int,
    inProgressCount: Int,
    resolvedCount: Int,
    issues: List<IssueEntity>,
    onIssueClick: (IssueEntity) -> Unit = {},
    onDeleteIssue: (IssueEntity) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToAdd: () -> Unit = {},
    onNavigateToComplaint: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var issueToDelete by remember { mutableStateOf<IssueEntity?>(null) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = System.currentTimeMillis()
        }
    }

    issueToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { issueToDelete = null },
            title = { Text("Delete Issue?") },
            text = {
                Text("Remove \"${target.title}\"? You can only delete complaints within 1 hour of filing.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteIssue(target)
                        issueToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { issueToDelete = null }) { Text("Cancel") }
            }
        )
    }

    val filterOptions = listOf(
        "ALL" to "All",
        "OPEN" to "Open",
        "IN_PROGRESS" to "In Progress",
        "RESOLVED" to "Resolved"
    )

    val filtered = remember(issues, selectedFilter, query) {
        val cleanQuery = query.trim()
        issues.filter { issue ->
            val matchesStatus = selectedFilter == "ALL" || issue.status.equals(selectedFilter, ignoreCase = true)
            val matchesQuery = cleanQuery.isEmpty() ||
                    issue.title.contains(cleanQuery, ignoreCase = true) ||
                    issue.description.contains(cleanQuery, ignoreCase = true) ||
                    issue.category.contains(cleanQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    val side = Modifier.padding(horizontal = 16.dp)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_input_add),
                        contentDescription = null
                    )
                },
                text = { Text("Quick Issue") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HomeHeader(
                    studentName = studentName,
                    studentEmail = studentEmail,
                    openCount = openCount,
                    now = currentTime,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToProfile = onNavigateToProfile,
                    onLogout = onLogout
                )
            }

            item {
                Row(
                    modifier = side.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickAction("⚡", "Quick Issue", onNavigateToAdd, Modifier.weight(1f))
                    QuickAction("📝", "Complaint", onNavigateToComplaint, Modifier.weight(1f))
                    QuickAction("🤖", "HostelBuddy", onNavigateToChat, Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = side.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard("Open", openCount, Brand.Red, Modifier.weight(1f))
                    StatCard("In Progress", inProgressCount, Brand.Amber, Modifier.weight(1f))
                    StatCard("Resolved", resolvedCount, Brand.Green, Modifier.weight(1f))
                }
            }

            item {
                ResolutionProgress(
                    open = openCount,
                    inProgress = inProgressCount,
                    resolved = resolvedCount,
                    modifier = side.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = side.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Search by title, room issue, or category...") },
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = side.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reported Issues",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filtered.size} shown",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    if (issues.isEmpty()) {
                        EmptyState(
                            emoji = "🛠️",
                            title = "No complaints yet",
                            message = "Tap Quick Issue or File Complaint to report your first problem."
                        )
                    } else {
                        EmptyState(
                            emoji = "🔍",
                            title = "Nothing matches",
                            message = "Try a different search or filter."
                        )
                    }
                }
            } else {
                itemsIndexed(filtered, key = { _, it -> it.id }) { index, issue ->
                    Box(modifier = side) {
                        FadeSlideIn(index = index) {
                            IssueCard(
                                issue = issue,
                                now = currentTime,
                                canDelete = issue.canBeDeleted(currentTime),
                                onClick = { onIssueClick(issue) },
                                onDeleteClick = {
                                    if (issue.canBeDeleted(currentTime)) issueToDelete = issue
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    studentName: String,
    studentEmail: String,
    openCount: Int,
    now: Long,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val greeting = remember(now / 60_000L) {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Hello"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brand.headerGradient)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 22.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(onClick = onNavigateToProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = studentName.firstOrNull()?.uppercase() ?: "S",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = studentName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onNavigateToChat) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_send),
                        contentDescription = "HostelBuddy AI",
                        tint = Color.White
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_more),
                            contentDescription = "More options",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Profile Details") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Logout",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onLogout()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
            ) {
                Text(
                    text = when {
                        openCount == 0 -> "All clear. You have no open issues 🎉"
                        openCount == 1 -> "You have 1 open issue being tracked"
                        else -> "You have $openCount open issues being tracked"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            if (studentEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reg. no: $studentEmail",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IssueCard(
    issue: IssueEntity,
    now: Long,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(statusColor(issue.status))
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CategoryBadge(issue.category)
                    StatusChip(status = issue.status)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (issue.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = issue.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (canDelete) {
                        TextButton(
                            onClick = onDeleteClick,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_delete),
                                contentDescription = "Delete Issue",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delete (< 1 hr)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = relativeTime(issue.createdAt, now),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}