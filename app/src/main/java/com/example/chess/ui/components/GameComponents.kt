package com.example.chess.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import kotlin.math.abs

@Composable
fun ChessClockView(
    whiteTimeSeconds: Int,
    blackTimeSeconds: Int,
    activeColor: PieceColor,
    isClockRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClockBox(
            label = "White",
            timeSeconds = whiteTimeSeconds,
            isActive = activeColor == PieceColor.WHITE && isClockRunning,
            color = Color.White,
            textColor = Color.Black,
            modifier = Modifier.testTag("white_clock")
        )

        ClockBox(
            label = "Black",
            timeSeconds = blackTimeSeconds,
            isActive = activeColor == PieceColor.BLACK && isClockRunning,
            color = Color(0xFF1E1E2E),
            textColor = Color.White,
            modifier = Modifier.testTag("black_clock")
        )
    }
}

@Composable
private fun ClockBox(
    label: String,
    timeSeconds: Int,
    isActive: Boolean,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val minutes = timeSeconds / 60
    val seconds = timeSeconds % 60
    val timeStr = String.format("%02d:%02d", minutes, seconds)

    val borderColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFFFFD700) else Color.Transparent,
        label = "clock_border"
    )

    Surface(
        modifier = modifier
            .width(140.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = color,
        tonalElevation = if (isActive) 8.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.7f)
            )
            Text(
                text = timeStr,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
        }
    }
}

@Composable
fun EvaluationBar(
    evalScore: Float,
    modifier: Modifier = Modifier
) {
    // evalScore ranges from -10.0 to +10.0 (positive = White, negative = Black)
    val clamped = evalScore.coerceIn(-10f, 10f)
    val whitePercentage = ((clamped + 10f) / 20f).coerceIn(0.05f, 0.95f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF1E192B))
            .border(1.dp, Color(0xFF4A4458), RoundedCornerShape(7.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(whitePercentage)
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f - whitePercentage)
                .background(Color(0xFF171923))
        )
    }
}

@Composable
fun CapturedPiecesView(
    capturedPieces: List<ChessPiece>,
    color: PieceColor,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val grouped = capturedPieces.groupBy { it.type }
        for ((type, list) in grouped) {
            val symbol = when (type) {
                PieceType.PAWN -> "♟"
                PieceType.KNIGHT -> "♞"
                PieceType.BISHOP -> "♝"
                PieceType.ROOK -> "♜"
                PieceType.QUEEN -> "♛"
                PieceType.KING -> "♚"
            }
            Text(
                text = "$symbol${if (list.size > 1) "x${list.size}" else ""}",
                fontSize = 14.sp,
                color = if (color == PieceColor.WHITE) Color.White else Color.Gray,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Composable
fun AiCoachDialog(
    advice: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Gemini Grandmaster Coach", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Text(
                    text = advice ?: "Analyzing board position...",
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("ai_coach_ok_button")
            ) {
                Text("Understood")
            }
        }
    )
}

@Composable
fun PromotionDialog(
    color: PieceColor,
    onSelected: (PieceType) -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Must select */ },
        title = { Text("Pawn Promotion", fontWeight = FontWeight.Bold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { pType ->
                    val name = pType.name.lowercase().replaceFirstChar { it.uppercase() }
                    Surface(
                        onClick = { onSelected(pType) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("promo_${pType.name.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = pType.sanChar.ifEmpty { "Q" },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
