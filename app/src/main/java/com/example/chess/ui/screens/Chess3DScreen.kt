package com.example.chess.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.GameMode
import com.example.chess.model.PieceColor
import com.example.chess.renderer3d.ChessBoard3DRenderer
import com.example.chess.ui.CurrentScreen
import com.example.chess.ui.MainViewModel
import com.example.chess.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chess3DScreen(
    viewModel: MainViewModel,
    uiState: com.example.chess.ui.UiState
) {
    var showPgnSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (uiState.gameMode) {
                                GameMode.VS_AI -> "vs AI (${uiState.aiDifficulty.name})"
                                GameMode.PASS_AND_PLAY -> "Pass & Play 2P"
                                GameMode.SPECTATE -> "AI Spectate"
                                GameMode.PUZZLES -> "Tactics Puzzle"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Theme: ${uiState.boardTheme.displayName}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(CurrentScreen.Home) },
                        modifier = Modifier.testTag("game_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Menu")
                    }
                },
                actions = {
                    // Gemini AI Coach button
                    IconButton(
                        onClick = { viewModel.fetchAiCoachAdvice() },
                        modifier = Modifier.testTag("ai_coach_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Coach",
                            tint = Color(0xFFFFD700)
                        )
                    }
                    // Reset Camera View
                    IconButton(
                        onClick = { viewModel.resetCameraView() },
                        modifier = Modifier.testTag("reset_camera_button")
                    ) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = "Reset Camera")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF140F21))
            )
        },
        containerColor = Color(0xFF0F0B1A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Opponent Player Bar (Black)
                PlayerHeader(
                    name = if (uiState.gameMode == GameMode.VS_AI) "AI Engine (${uiState.aiDifficulty.displayName})" else "Player 2 (Black)",
                    color = PieceColor.BLACK,
                    isActive = uiState.gameState.activeColor == PieceColor.BLACK && !uiState.gameState.isGameOver(),
                    capturedPieces = uiState.gameState.capturedWhitePieces,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                // Chess Clocks View
                ChessClockView(
                    whiteTimeSeconds = uiState.whiteTimeSeconds,
                    blackTimeSeconds = uiState.blackTimeSeconds,
                    activeColor = uiState.gameState.activeColor,
                    isClockRunning = uiState.isClockRunning
                )

                // Positional Evaluation Bar
                EvaluationBar(
                    evalScore = uiState.gameState.evaluationScore,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // 3D Interactive Renderer Canvas Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("chess_3d_canvas_container")
                ) {
                    ChessBoard3DRenderer(
                        modifier = Modifier.fillMaxSize(),
                        gameState = uiState.gameState,
                        camera = uiState.camera,
                        theme = uiState.boardTheme,
                        selectedSquare = uiState.selectedSquare,
                        validMoves = uiState.validMoves,
                        lastMove = uiState.gameState.moveHistory.lastOrNull(),
                        onSquareSelected = { sq -> viewModel.onSquareClick(sq) },
                        onCameraRotated = { yaw, pitch -> viewModel.rotateCamera(yaw, pitch) },
                        onCameraZoomed = { factor -> viewModel.zoomCamera(factor) }
                    )

                    // AI Thinking Indicator overlay
                    if (uiState.isAiThinking) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFFFFD700),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("AI is thinking...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Check Banner overlay
                    if (uiState.gameState.isCheck && !uiState.gameState.isGameOver()) {
                        Surface(
                            color = Color(0xFFEF4444),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "CHECK!",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Bottom Player Bar (White)
                PlayerHeader(
                    name = if (uiState.gameMode == GameMode.VS_AI) "You (White)" else "Player 1 (White)",
                    color = PieceColor.WHITE,
                    isActive = uiState.gameState.activeColor == PieceColor.WHITE && !uiState.gameState.isGameOver(),
                    capturedPieces = uiState.gameState.capturedBlackPieces,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                // 3D Controls Toolbar Bottom Row
                GameControlToolbar(
                    onUndo = { viewModel.undoMove() },
                    onResetCamera = { viewModel.resetCameraView() },
                    onToggle2D = { viewModel.toggleCamera2D() },
                    is2D = uiState.camera.isTopDown2D,
                    onTogglePgn = { showPgnSheet = !showPgnSheet },
                    onAiCoach = { viewModel.fetchAiCoachAdvice() }
                )
            }

            // PGN History Bottom Sheet / Drawer
            AnimatedVisibility(
                visible = showPgnSheet,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PgnSheetView(
                    moveHistory = uiState.gameState.moveHistory,
                    onClose = { showPgnSheet = false }
                )
            }
        }
    }

    // Pawn Promotion Modal
    if (uiState.showPromotionDialog) {
        PromotionDialog(
            color = uiState.gameState.activeColor,
            onSelected = { pType -> viewModel.onPromotionSelected(pType) }
        )
    }

    // AI Coach Modal
    if (uiState.showAiCoachDialog) {
        AiCoachDialog(
            advice = uiState.aiCoachAdvice,
            isLoading = uiState.isAiCoachLoading,
            onDismiss = { viewModel.dismissAiCoach() }
        )
    }

    // Game Over Dialog
    if (uiState.gameState.isGameOver()) {
        val title = when {
            uiState.gameState.isCheckmate -> "${if (uiState.gameState.activeColor == PieceColor.WHITE) "Black" else "White"} Wins!"
            uiState.gameState.isStalemate -> "Draw by Stalemate"
            else -> "Draw"
        }
        AlertDialog(
            onDismissRequest = { },
            title = { Text(title, fontWeight = FontWeight.Black, fontSize = 22.sp) },
            text = {
                Column {
                    Text(
                        text = if (uiState.gameState.isCheckmate) "Checkmate! Spectacular tactical victory." else "The match ended in a draw (${uiState.gameState.drawReason ?: "Stalemate"}).",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Total Moves: ${uiState.gameState.moveCount}", fontSize = 13.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.startNewGame(uiState.gameMode, uiState.aiDifficulty, uiState.playerColor) },
                    modifier = Modifier.testTag("rematch_button")
                ) {
                    Text("Play Rematch")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.navigateTo(CurrentScreen.Home) }) {
                    Text("Back to Home")
                }
            }
        )
    }
}

@Composable
private fun PlayerHeader(
    name: String,
    color: PieceColor,
    isActive: Boolean,
    capturedPieces: List<com.example.chess.model.ChessPiece>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0xFF22C55E) else Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) Color.White else Color.Gray
            )
        }

        CapturedPiecesView(capturedPieces = capturedPieces, color = color)
    }
}

@Composable
private fun GameControlToolbar(
    onUndo: () -> Unit,
    onResetCamera: () -> Unit,
    onToggle2D: () -> Unit,
    is2D: Boolean,
    onTogglePgn: () -> Unit,
    onAiCoach: () -> Unit
) {
    Surface(
        color = Color(0xFF1B152B),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onUndo, modifier = Modifier.testTag("btn_undo")) {
                Icon(Icons.Default.Undo, contentDescription = "Undo Move", tint = Color.White)
            }
            IconButton(onClick = onResetCamera, modifier = Modifier.testTag("btn_reset_cam")) {
                Icon(Icons.Default.ScreenRotation, contentDescription = "Reset Camera View", tint = Color(0xFFFFD700))
            }
            IconButton(onClick = onToggle2D, modifier = Modifier.testTag("btn_toggle_2d")) {
                Icon(
                    imageVector = if (is2D) Icons.Default.ViewInAr else Icons.Default.GridOn,
                    contentDescription = "Toggle 3D/2D View",
                    tint = Color.White
                )
            }
            IconButton(onClick = onAiCoach, modifier = Modifier.testTag("btn_ai_coach")) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Move Coach", tint = Color(0xFFFFD700))
            }
            IconButton(onClick = onTogglePgn, modifier = Modifier.testTag("btn_pgn_log")) {
                Icon(Icons.Default.FormatListNumbered, contentDescription = "Move Log PGN", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PgnSheetView(
    moveHistory: List<com.example.chess.model.Move>,
    onClose: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1730),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Move History (PGN)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (moveHistory.isEmpty()) {
                Text("No moves played yet.", color = Color.Gray, fontSize = 13.sp)
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(moveHistory.chunked(2)) { pair ->
                        val moveNum = (moveHistory.indexOf(pair[0]) / 2) + 1
                        val whiteMove = pair[0].san
                        val blackMove = if (pair.size > 1) pair[1].san else ""

                        Surface(
                            color = Color(0xFF2D2345),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$moveNum. $whiteMove  $blackMove",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
