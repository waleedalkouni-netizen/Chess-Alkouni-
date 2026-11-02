package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.AiDifficulty
import com.example.chess.model.BoardTheme
import com.example.chess.model.GameMode
import com.example.chess.model.PieceColor
import com.example.chess.ui.CurrentScreen
import com.example.chess.ui.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    uiState: com.example.chess.ui.UiState
) {
    var selectedDifficulty by remember { mutableStateOf(uiState.aiDifficulty) }
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0B1A), Color(0xFF1D162B), Color(0xFF0F0B1A))
                )
            )
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Header Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2E1C4E), Color(0xFF4A2B78), Color(0xFF2A1646))
                        )
                    )
                    .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Surface(
                                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "REALTIME 3D ENGINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Chess 3D Realm",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = "Chess Crown",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Experience grandmaster chess with interactive 3D camera, realistic lighting, local Minimax AI, and Gemini AI tactical move coach.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Mode 1: Play vs AI
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF221A35)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D2D5C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Single Player vs AI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select AI Level:", fontSize = 13.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiDifficulty.values().forEach { diff ->
                            val isSel = selectedDifficulty == diff
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedDifficulty = diff },
                                label = {
                                    Text(
                                        text = diff.name.lowercase().replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.testTag("ai_diff_${diff.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Play As:", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = selectedColor == PieceColor.WHITE,
                            onClick = { selectedColor = PieceColor.WHITE },
                            label = { Text("White (First)") },
                            modifier = Modifier.testTag("color_white")
                        )
                        FilterChip(
                            selected = selectedColor == PieceColor.BLACK,
                            onClick = { selectedColor = PieceColor.BLACK },
                            label = { Text("Black (Second)") },
                            modifier = Modifier.testTag("color_black")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.startNewGame(GameMode.VS_AI, selectedDifficulty, selectedColor) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_vs_ai_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start 3D Match vs AI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Action Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pass & Play
                ModeCard(
                    title = "Pass & Play",
                    subtitle = "2 Players Local",
                    icon = Icons.Default.Group,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f).testTag("mode_pass_play")
                ) {
                    viewModel.startNewGame(GameMode.PASS_AND_PLAY)
                }

                // Spectate AI vs AI
                ModeCard(
                    title = "Spectate AI",
                    subtitle = "AI vs AI Battle",
                    icon = Icons.Default.Visibility,
                    color = Color(0xFFF43F5E),
                    modifier = Modifier.weight(1f).testTag("mode_spectate")
                ) {
                    viewModel.startNewGame(GameMode.SPECTATE)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Puzzles
                ModeCard(
                    title = "Tactics Puzzles",
                    subtitle = "Solve Mate & Pins",
                    icon = Icons.Default.Psychology,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f).testTag("mode_puzzles")
                ) {
                    viewModel.navigateTo(CurrentScreen.Puzzles)
                }

                // Match History
                ModeCard(
                    title = "Match History",
                    subtitle = "PGN & Replays",
                    icon = Icons.Default.History,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f).testTag("mode_history")
                ) {
                    viewModel.navigateTo(CurrentScreen.History)
                }
            }
        }

        // Theme Selector Preview
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF221A35)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D2D5C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3D Board Theme", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        TextButton(onClick = { viewModel.navigateTo(CurrentScreen.Settings) }) {
                            Text("All Themes", color = Color(0xFFA78BFA))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BoardTheme.values().forEach { theme ->
                            val isSel = uiState.boardTheme == theme
                            Surface(
                                onClick = { viewModel.setBoardTheme(theme) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFF8B5CF6) else Color(0xFF171124),
                                border = if (isSel) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .height(60.dp)
                                    .testTag("theme_select_${theme.name.lowercase()}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = theme.displayName.split(" ")[0],
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF221A35)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D2D5C)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
