package com.example.ainotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // State variables for user data
    var email by remember { mutableStateOf("Loading...") }
    var name by remember { mutableStateOf("Loading...") }
    var translationLanguage by remember { mutableStateOf("Loading...") }

    // Fetch user data from Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        email = document.getString("email") ?: "Unknown Email"
                        name = document.getString("name") ?: "Unknown Name"
                        translationLanguage = document.getString("translationLanguage") ?: "English" // Default to English
                    } else {
                        email = "Document does not exist"
                        name = "Document does not exist"
                        translationLanguage = "Unknown"
                    }
                }
                .addOnFailureListener { exception ->
                    email = "Failed to fetch"
                    name = "Failed to fetch"
                    translationLanguage = "Failed to fetch"
                }
        } else {
            email = "User not logged in"
            name = "User not logged in"
            translationLanguage = "User not logged in"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Dark background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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
                Spacer(modifier = Modifier.width(48.dp)) // Placeholder to balance the layout
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Usage Section
            SectionHeader("USAGE")
            SettingsItem(
                title = "Usage: 0 of 30 mins",
                subtitle = "Welcome to NotesApp!",
                showArrow = false
            )
            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Free Version Section
            SettingsItem(title = "Free Version", showArrow = true)
            SettingsItem(title = "Version 1.0.0, build 1", showArrow = false)

            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Personal Information Section
            SectionHeader("PERSONAL INFORMATION")
            SettingsItem(title = "Name", value = name, showArrow = false)
            SettingsItem(title = "Email", value = email, showArrow = false)

            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Preferences Section
            SectionHeader("PREFERENCES")
            SettingsItem(
                title = "Transcription Language",
                value = translationLanguage,
                showArrow = true
            )

            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Help Section
            SectionHeader("HELP")
            SettingsItem(title = "Privacy Policy", showArrow = true)
            SettingsItem(title = "Terms of Use", showArrow = true)

            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Log Out Section
            SettingsItem(
                title = "Log Out",
                showArrow = false,
                textColor = Color.White,
                onClick = { onLogoutClicked() }
            )
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
    showArrow: Boolean = false,
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
        if (value.isNotEmpty() || showArrow) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) {
                    Text(text = value, color = Color.Gray, fontSize = 14.sp)
                }
                if (showArrow) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
