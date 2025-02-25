package com.example.ainotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ainotes.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    onLogoutClicked: () -> Unit,
    viewModel: SettingsViewModel
) {
    val userData by viewModel.userData.collectAsState()
    val appVersion by viewModel.appVersion.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Dark background
    ) {
        if (userData == null) {
            // 🔥 Show loading indicator while fetching data
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onBackPressed() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }

                    item { SectionHeader("USAGE") }
                    item { SettingsItem(title = "Usage: 0 of 30 mins", subtitle = "Welcome to NotesApp!") }
                    item { Divider(color = Color.Gray, thickness = 1.dp) }

                    item { SettingsItem(title = "Version Type: Free") }
                    item { SettingsItem(title = "Version: $appVersion") }
                    item { Divider(color = Color.Gray, thickness = 1.dp) }

                    item { SectionHeader("PERSONAL INFORMATION") }
                    item { SettingsItem(title = "Name", value = userData?.name ?: "...") }
                    item { SettingsItem(title = "Email", value = userData?.email ?: "...") }
                    item { Divider(color = Color.Gray, thickness = 1.dp) }

                    item { SectionHeader("PREFERENCES") }

                    // 🔥 Clicking opens language selection
                    item {
                        SettingsItem(
                            title = "Transcription Language",
                            value = userData?.transcriptionLanguage ?: "...",
                            onClick = { showLanguageDialog = true }
                        )
                    }

                    // Toggle Preferences
                    item { ToggleItem("Auto Delete Notes", userData?.autoDeleteNotes ?: true) { viewModel.updatePreference("autoDeleteNotes", it) } }
                    item { ToggleItem("Category Detection", userData?.categoryDetection ?: true) { viewModel.updatePreference("categoryDetection", it) } }
                    item { ToggleItem("Smart Summaries", userData?.smartSummaries ?: true) { viewModel.updatePreference("smartSummaries", it) } }
                    item { ToggleItem("Enable Transcription", userData?.transcriptionEnabled ?: true) { viewModel.updatePreference("transcriptionEnabled", it) } }

                    item { Divider(color = Color.Gray, thickness = 1.dp) }

                    item { SectionHeader("HELP") }
                    item { SettingsItem(title = "Privacy Policy") }
                    item { SettingsItem(title = "Terms of Use") }
                    item { Divider(color = Color.Gray, thickness = 1.dp) }

                    // 🔥 Make Logout Red
                    item {
                        SettingsItem(
                            title = "Log Out",
                            textColor = Color.Red, // 🔥 Logout is red
                            onClick = {
                                viewModel.clearUserData()  // Clear cached user data
                                onLogoutClicked()          // Then proceed with logout
                            }
                        )
                    }
                }

                // 🔥 Move Dialog OUTSIDE LazyColumn to fix the error
                if (showLanguageDialog) {
                    LanguageSelectionDialog(
                        availableLanguages = availableLanguages,
                        selectedLanguage = userData?.transcriptionLanguage ?: "English",
                        onDismiss = { showLanguageDialog = false },
                        onLanguageSelected = {
                            viewModel.updateTranscriptionLanguage(it)
                            showLanguageDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    availableLanguages: List<String>,
    selectedLanguage: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.error)
            }
        },
        title = {
            Text(
                "Choose Transcription Language",
                color = Color.White, // 🔥 Ensures visibility on dark background
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2C2C), shape = MaterialTheme.shapes.medium) // 🔥 Darker background
                    .padding(16.dp)
            ) {
                LazyColumn {
                    items(availableLanguages) { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { onLanguageSelected(language) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language == selectedLanguage,
                                onClick = { onLanguageSelected(language) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = Color.Gray // 🔥 Softer contrast
                                )
                            )
                            Text(
                                text = language,
                                color = Color.White, // 🔥 Ensures visibility
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF1A1A1A),
        shape = MaterialTheme.shapes.large
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String = "",
    value: String = "",
    textColor: Color = Color.White,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(Color.Transparent)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = textColor, fontSize = 16.sp)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, color = Color.Gray, fontSize = 14.sp)
            }
        }
        if (value.isNotEmpty()) {
            Text(text = value, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun ToggleItem(title: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    var state by remember { mutableStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 16.sp)
        Switch(
            checked = state,
            onCheckedChange = {
                state = it
                onToggle(it)
            },
            modifier = Modifier.scale(0.8f) // 🔥 Makes the switch smaller
        )
    }
}