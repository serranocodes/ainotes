// File: NoteDetailScreen.kt
package com.example.ainotes.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.NotesViewModel
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    notesViewModel: NotesViewModel,
    navController: NavController
) {
    val notes = notesViewModel.notes
    val note = notes.find { it.id == noteId } ?: return

    val bodyStyle = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = Color(0xFFECECEC),
        lineHeightStyle = LineHeightStyle(
            LineHeightStyle.Alignment.Proportional,
            LineHeightStyle.Trim.None
        )
    )

    var isEditing by remember { mutableStateOf(false) }
    var value by remember(note) { mutableStateOf(TextFieldValue(note.content)) }

    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val bringRequester = remember { BringIntoViewRequester() }

    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    var lastLayout: TextLayoutResult? by remember { mutableStateOf(null) }
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    // Keep the caret line above the keyboard (without adding IME padding to the whole screen)
    LaunchedEffect(isEditing, value.selection, imeBottomPx, lastLayout) {
        if (isEditing && lastLayout != null) {
            val idx = value.selection.end.coerceIn(0, max(0, value.text.length - 1))
            val box = runCatching { lastLayout!!.getBoundingBox(idx) }.getOrNull()
            if (box != null) {
                val pad = with(density) { 24.dp.toPx() }
                bringRequester.bringIntoView(
                    Rect(box.left, max(0f, box.top - pad), box.right, box.bottom + pad)
                )
            } else if (value.selection.end >= value.text.length - 2) {
                scope.launch { scroll.animateScrollTo(scroll.maxValue) }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0D0F13),
        contentWindowInsets = WindowInsets.safeDrawing,
        // Move ONLY the bottom actions with the IME
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()                // ⬅️ floats just above keyboard
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { notesViewModel.toggleFavorite(noteId) }) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Filled.Favorite
                        else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color(0xFFB9FFE8)
                    )
                }
                IconButton(onClick = {
                    notesViewModel.deleteNote(noteId)
                    navController.popBackStack()
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFFF6B6B))
                }
                IconButton(onClick = {
                    if (isEditing) {
                        notesViewModel.updateNote(noteId, value.text)
                        focus.clearFocus(force = true)
                        keyboard?.hide()
                    }
                    isEditing = !isEditing
                    if (isEditing) {
                        // place caret at end so bring-into-view knows where to scroll
                        value = value.copy(selection = TextRange(value.text.length))
                    }
                }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit",
                        tint = if (isEditing) Color(0xFF32D74B) else Color(0xFF7AA8FF)
                    )
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)                 // no imePadding here
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = note.resolvedTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (note.aiSummary.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    color = Color(0xFF13171E),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AI Summary",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF9AA4B2)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = note.aiSummary,
                            style = bodyStyle,
                            color = Color(0xFFECECEC)
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
                color = Color(0xFF13171E),
                tonalElevation = 0.dp,
                shape = MaterialTheme.shapes.large,
                border = if (isEditing)
                    BorderStroke(1.dp, Color(0x8DE30303))
                else
                    BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scroll)
                        .bringIntoViewRequester(bringRequester)
                ) {
                    if (isEditing) {
                        BasicTextField(
                            value = value,
                            onValueChange = { nv ->
                                value = nv
                                if (nv.selection.end >= nv.text.length - 2) {
                                    scope.launch { scroll.animateScrollTo(scroll.maxValue) }
                                }
                            },
                            textStyle = bodyStyle,
                            cursorBrush = SolidColor(Color(0xFF7DF9FF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { st ->
                                    if (st.isFocused) {
                                        scope.launch { scroll.animateScrollTo(scroll.maxValue) }
                                    }
                                },
                            onTextLayout = { layout -> lastLayout = layout }
                        )
                        // small spacer so last line isn’t under the bottom bar
                        Spacer(Modifier.height(12.dp))
                    } else {
                        SelectionContainer {
                            Text(
                                text = value.text,
                                style = bodyStyle,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}