package com.example.hostelfix.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hostelfix.data.IssueEntity

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
    onNavigateToChat: () -> Unit = {},
    onNavigateToAdd: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome, $studentName") },
                actions = {
                    IconButton(onClick = onNavigateToChat) {
                        Icon(painter = painterResource(android.R.drawable.ic_menu_send), contentDescription = "AI Chat")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(painter = painterResource(android.R.drawable.ic_input_add), contentDescription = "Add Issue")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search your complaints...") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            SummaryRow(openCount.toString(), inProgressCount.toString(), resolvedCount.toString())

            Spacer(Modifier.height(16.dp))

            Text("Your Reported Issues", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(8.dp))

            val filtered = if (query.isBlank()) issues else issues.filter {
                it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No complaints found. Tap '+' to file a new issue.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { issue ->
                        IssueCard(issue = issue, onClick = { onIssueClick(issue) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(open: String, inProgress: String, resolved: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(modifier = Modifier.weight(1f), title = "Open", value = open, color = Color(0xFFEF4444))
        SummaryCard(modifier = Modifier.weight(1f), title = "In Progress", value = inProgress, color = Color(0xFFF59E0B))
        SummaryCard(modifier = Modifier.weight(1f), title = "Resolved", value = resolved, color = Color(0xFF10B981))
    }
}

@Composable
private fun SummaryCard(modifier: Modifier = Modifier, title: String, value: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun IssueCard(issue: IssueEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(issue.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(issue.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(status = issue.status)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(issue.timeAgo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (text, color) = when (status) {
        "OPEN" -> "Open" to Color(0xFFDC2626)
        "IN_PROGRESS" -> "In Progress" to Color(0xFFF59E0B)
        else -> "Resolved" to Color(0xFF10B981)
    }
    Box(
        modifier = Modifier.background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}