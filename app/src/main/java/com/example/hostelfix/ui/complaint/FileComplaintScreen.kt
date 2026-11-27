package com.example.hostelfix.ui.complaint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileComplaintScreen(
    onNavigateBack: () -> Unit,
    onSubmitComplaint: (title: String, description: String, category: String, onDone: () -> Unit) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("MAINTENANCE") }
    var showError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSubmittedDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val categories = listOf(
        "MAINTENANCE" to "Maintenance",
        "ACADEMICAL" to "Academical"
    )

    if (showSubmittedDialog) {
        AlertDialog(
            onDismissRequest = {
                showSubmittedDialog = false
                onNavigateBack()
            },
            title = { Text("Complaint Submitted", fontWeight = FontWeight.Bold) },
            text = { Text("Your complaint has been submitted successfully.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSubmittedDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.SemiBold)
                }
            }
        )
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
                        text = "File a Complaint",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = { if (!isSubmitting) onNavigateBack() },
                        enabled = !isSubmitting
                    ) {
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
                    Text(
                        text = "Select Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        categories.forEach { (key, label) ->
                            FilterChip(
                                selected = selectedCategory == key,
                                onClick = {
                                    if (!isSubmitting) {
                                        selectedCategory = key
                                    }
                                },
                                label = { Text(label) },
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            showError = false
                        },
                        label = { Text("Complaint Title") },
                        placeholder = { Text("e.g. Broken fan or Lab PC issue") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        isError = showError && title.isBlank()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            showError = false
                        },
                        label = { Text("Description") },
                        placeholder = { Text("Describe the issue clearly...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        isError = showError && description.isBlank()
                    )

                    if (showError) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Please fill in both title and description.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (title.isBlank() || description.isBlank()) {
                        showError = true
                    } else {
                        isSubmitting = true
                        onSubmitComplaint(title.trim(), description.trim(), selectedCategory) {
                            isSubmitting = false
                            showSubmittedDialog = true
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSubmitting) "Submitting..." else "Submit Complaint",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}