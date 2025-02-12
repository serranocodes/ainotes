package com.example.ainotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import com.example.ainotes.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    onLogoutClicked: () -> Unit,
    viewModel: SettingsViewModel
) {
    val userData by viewModel.userData.collectAsState()
    val appVersion by viewModel.appVersion.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Dark background
    ) {
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
                            imageVector = Icons.Default.ArrowBack,
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
            item { SettingsItem(title = "Name", value = userData.name) }
            item { SettingsItem(title = "Email", value = userData.email) }
            item { Divider(color = Color.Gray, thickness = 1.dp) }

            item { SectionHeader("PREFERENCES") }
            item { SettingsItem(title = "Transcription Language", value = userData.transcriptionLanguage) }

            // 🔥 Toggles Section with Smaller Switches
            item { ToggleItem("Auto Delete Notes", userData.autoDeleteNotes) { viewModel.updatePreference("autoDeleteNotes", it) } }
            item { ToggleItem("Category Detection", userData.categoryDetection) { viewModel.updatePreference("categoryDetection", it) } }
            item { ToggleItem("Smart Summaries", userData.smartSummaries) { viewModel.updatePreference("smartSummaries", it) } }
            item { ToggleItem("Enable Transcription", userData.transcriptionEnabled) { viewModel.updatePreference("transcriptionEnabled", it) } }

            // 🔥 Theme Switch (Light/Dark Mode)
            item {
                ThemeToggleItem("Theme", userData.theme) { viewModel.updatePreference("theme", it) }
            }

            item { Divider(color = Color.Gray, thickness = 1.dp) }

            item { SectionHeader("HELP") }
            item { SettingsItem(title = "Privacy Policy") }
            item { SettingsItem(title = "Terms of Use") }
            item { Divider(color = Color.Gray, thickness = 1.dp) }

            item { SettingsItem(title = "Log Out", textColor = Color.White, onClick = onLogoutClicked) }
        }
    }
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

@Composable
fun ThemeToggleItem(title: String, value: String, onToggle: (String) -> Unit) {
    var isDarkMode by remember { mutableStateOf(value == "dark") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 16.sp)
        Switch(
            checked = isDarkMode,
            onCheckedChange = {
                isDarkMode = it
                onToggle(if (it) "dark" else "light")
            },
            modifier = Modifier.scale(0.8f) // 🔥 Makes the switch smaller
        )
    }
}
