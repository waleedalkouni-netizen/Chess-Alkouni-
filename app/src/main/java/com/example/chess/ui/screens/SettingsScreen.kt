package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.BoardTheme
import com.example.chess.ui.CurrentScreen
import com.example.chess.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    uiState: com.example.chess.ui.UiState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Themes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(CurrentScreen.Home) },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF140F21))
            )
        },
        containerColor = Color(0xFF0F0B1A)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme Customization Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF221A35)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D2D5C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF8B5CF6))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("3D Board & Piece Theme", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        BoardTheme.values().forEach { theme ->
                            val isSel = uiState.boardTheme == theme
                            Surface(
                                onClick = { viewModel.setBoardTheme(theme) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF171124),
                                border = if (isSel) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("setting_theme_${theme.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(theme.displayName, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(
                                            text = if (isSel) "Active Theme" else "Tap to apply",
                                            fontSize = 11.sp,
                                            color = if (isSel) Color(0xFFFFD700) else Color.Gray
                                        )
                                    }
                                    if (isSel) {
                                        Text("✓", color = Color(0xFFFFD700), fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Audio & Haptics Toggles
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF221A35)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D2D5C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sound & Feedback", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Piece Move & Capture Sounds", color = Color.White)
                            Switch(
                                checked = uiState.isSoundEnabled,
                                onCheckedChange = { viewModel.setSoundEnabled(it) },
                                modifier = Modifier.testTag("switch_sound")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Haptic Vibration Feedback", color = Color.White)
                            Switch(
                                checked = uiState.isHapticsEnabled,
                                onCheckedChange = { viewModel.setHapticsEnabled(it) },
                                modifier = Modifier.testTag("switch_haptics")
                            )
                        }
                    }
                }
            }
        }
    }
}
