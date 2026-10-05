package com.example.hostelfix.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class Issue(
    val id: Int,
    val title: String,
    val description: String,
    val status: Status,
    val timeAgo: String
) {
    enum class Status { OPEN, IN_PROGRESS, RESOLVED }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onIssueClick: (Issue) -> Unit = {}
) {
    val issuesState = remember { mutableStateListOf<Issue>().apply { addAll(sampleIssues()) } }
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("HostelFix") },
                actions = {
                    // Placeholder for future actions (profile, filter)
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_search),
                            contentDescription = "Search"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text("Search issues, rooms, keywords") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            SummaryRow()

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Issues", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                TextButton(onClick = { /*TODO: navigate to all issues*/ }) {
                    Text("See all")
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val filtered = if (query.isBlank()) issuesState else issuesState.filter {
                    it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
                }

                items(filtered) { issue ->
                    IssueCard(issue = issue, onClick = { onIssueClick(issue) })
                }
            }
        }
    }
}

@Composable
private fun SummaryRow() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(modifier = Modifier.weight(1f), title = "Open", value = "12", color = Color(0xFFEF4444))
        SummaryCard(modifier = Modifier.weight(1f), title = "In progress", value = "3", color = Color(0xFFF59E0B))
        SummaryCard(modifier = Modifier.weight(1f), title = "Resolved", value = "34", color = Color(0xFF10B981))
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun IssueCard(issue: Issue, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
private fun StatusChip(status: Issue.Status) {
    val (text, color) = when (status) {
        Issue.Status.OPEN -> "Open" to Color(0xFFDC2626)
        Issue.Status.IN_PROGRESS -> "In Progress" to Color(0xFFF59E0B)
        Issue.Status.RESOLVED -> "Resolved" to Color(0xFF10B981)
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

private fun sampleIssues(): List<Issue> = listOf(
    Issue(
        id = 1,
        title = "Leaky faucet in Room 203",
        description = "The faucet in room 203 is leaking continuously. Needs plumber.",
        status = Issue.Status.OPEN,
        timeAgo = "2h ago"
    ),
    Issue(
        id = 2,
        title = "Broken study table",
        description = "The study table in the common room is wobbly and the screws are missing.",
        status = Issue.Status.IN_PROGRESS,
        timeAgo = "1d ago"
    ),
    Issue(
        id = 3,
        title = "No hot water in Block B",
        description = "Students reported cold showers since morning in Block B.",
        status = Issue.Status.RESOLVED,
        timeAgo = "3d ago"
    )
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    Surface {
        HomeScreen()
    }
}