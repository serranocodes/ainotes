// File: SettingsScreen.kt
package com.example.ainotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ainotes.viewmodel.SettingsViewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ainotes.data.AiSummaryPreferences
import com.example.ainotes.data.TranscriptionPreferences
import com.example.ainotes.util.LanguageCodes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    val bg = Color(0xFF0D0F13)
    val hairline = Color(0x22FFFFFF)
    val onBg = Color(0xFFECEDEF)
    val subText = Color(0xFF9AA4B2)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(userData?.autoTitleSummary, userData?.autoNoteSummary) {
        userData?.autoTitleSummary?.let { scope.launch { AiSummaryPreferences.setAutoTitle(context, it) } }
        userData?.autoNoteSummary?.let { scope.launch { AiSummaryPreferences.setAutoNote(context, it) } }
    }

    Scaffold(
        containerColor = bg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", color = onBg) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = onBg
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = onBg,
                    titleContentColor = onBg
                )
            )
        }
    ) { inner ->
        if (userData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // ---- USAGE ----
                item { SectionHeader("USAGE") }
                item { SettingsItem(title = "Usage: 0 of 30 mins", subtitle = "Welcome to NotesApp!") }
                item { Divider(color = hairline, thickness = 1.dp) }

                // ---- VERSION ----
                item { SettingsItem(title = "Version Type", value = "Free") }
                item { SettingsItem(title = "Version", value = appVersion) }
                item { Divider(color = hairline, thickness = 1.dp) }

                // ---- PERSONAL ----
                item { SectionHeader("PERSONAL INFORMATION") }
                item { SettingsItem(title = "Name", value = userData?.name.orEmpty()) }
                item { SettingsItem(title = "Email", value = userData?.email.orEmpty()) }
                item { Divider(color = hairline, thickness = 1.dp) }

                // ---- PREFERENCES ----
                item { SectionHeader("PREFERENCES") }
                item {
                    SettingsItem(
                        title = "Transcription Language",
                        value = userData?.transcriptionLanguage ?: "English (US)",
                        onClick = { showLanguageDialog = true }
                    )
                }
                item {
                    ToggleItem(
                        title = "Auto Delete Notes",
                        value = userData?.autoDeleteNotes ?: true
                    ) { viewModel.updatePreference("autoDeleteNotes", it) }
                }
                item {
                    ToggleItem(
                        title = "Category Detection",
                        value = userData?.categoryDetection ?: true
                    ) { viewModel.updatePreference("categoryDetection", it) }
                }
                item {
                    ToggleItem(
                        title = "Smart Summaries",
                        value = userData?.smartSummaries ?: true
                    ) { viewModel.updatePreference("smartSummaries", it) }
                }
                item {
                    ToggleItem(
                        title = "Enable Transcription",
                        value = userData?.transcriptionEnabled ?: true
                    ) { viewModel.updatePreference("transcriptionEnabled", it) }
                }
                item {
                    ToggleItem(
                        title = "AI Title Summaries",
                        value = userData?.autoTitleSummary ?: false
                    ) {
                        viewModel.updatePreference("autoTitleSummary", it)
                        scope.launch { AiSummaryPreferences.setAutoTitle(context, it) }
                    }
                }
                item {
                    ToggleItem(
                        title = "AI Note Summaries",
                        value = userData?.autoNoteSummary ?: false
                    ) {
                        viewModel.updatePreference("autoNoteSummary", it)
                        scope.launch { AiSummaryPreferences.setAutoNote(context, it) }
                    }
                }

                item { Divider(color = hairline, thickness = 1.dp) }

                // ---- HELP ----
                item { SectionHeader("HELP") }
                item { SettingsItem(title = "Privacy Policy") }
                item { SettingsItem(title = "Terms of Use") }
                item { Divider(color = hairline, thickness = 1.dp) }

                // ---- LOG OUT ----
                item {
                    SettingsItem(
                        title = "Log Out",
                        textColor = Color(0xFFFF6B6B),
                        onClick = {
                            viewModel.clearUserData()
                            onLogoutClicked()
                        }
                    )
                }
            }

            if (showLanguageDialog) {
                LanguageSelectionDialog(
                    availableLanguages = availableLanguages,
                    selectedLanguage = userData?.transcriptionLanguage ?: "English (US)",
                    onDismiss = { showLanguageDialog = false },
                    onLanguageSelected = { displayName ->
                        // 1) Save UI label to Firestore
                        viewModel.updateTranscriptionLanguage(displayName)
                        // 2) Save engine tag locally for the recognizer
                        val tag = LanguageCodes.nameToTag(displayName)
                        scope.launch {
                            TranscriptionPreferences.setLanguageTag(context, tag)
                        }
                        showLanguageDialog = false
                    }
                )
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
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2C2C), shape = MaterialTheme.shapes.medium)
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
                                    unselectedColor = Color.Gray
                                )
                            )
                            Text(
                                text = language,
                                color = Color.White,
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
        color = Color(0xFF9AA4B2),
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
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
        Column(Modifier.weight(1f)) {
            Text(text = title, color = textColor, fontSize = 16.sp)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, color = Color(0xFF9AA4B2), fontSize = 14.sp)
            }
        }
        if (value.isNotEmpty()) {
            Text(text = value, color = Color(0xFF9AA4B2), fontSize = 14.sp)
        }
    }
}

@Composable
fun ToggleItem(title: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    var state by remember(value) { mutableStateOf(value) }

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
            modifier = Modifier.scale(0.9f)
        )
    }
}
