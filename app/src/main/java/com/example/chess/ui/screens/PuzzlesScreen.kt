package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.data.PuzzleProvider
import com.example.chess.ui.CurrentScreen
import com.example.chess.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzlesScreen(
    viewModel: MainViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tactical Chess Puzzles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(CurrentScreen.Home) },
                        modifier = Modifier.testTag("puzzles_back_button")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(PuzzleProvider.samplePuzzles) { puzzle ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF221A35)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D2D5C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("puzzle_item_${puzzle.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(puzzle.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Surface(
                                color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${puzzle.difficulty} (${puzzle.rating})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA78BFA),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(puzzle.description, fontSize = 13.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.startPuzzle(puzzle) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_play_${puzzle.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Solve in 3D Arena")
                        }
                    }
                }
            }
        }
    }
}
