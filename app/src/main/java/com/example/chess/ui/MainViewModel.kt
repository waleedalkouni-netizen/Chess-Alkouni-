package com.example.chess.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.audio.SoundManager
import com.example.chess.data.ChessDatabase
import com.example.chess.data.GameRecordEntity
import com.example.chess.data.PuzzleProvider
import com.example.chess.engine.GeminiChessService
import com.example.chess.engine.MinimaxEngine
import com.example.chess.engine.MoveGenerator
import com.example.chess.model.*
import com.example.chess.renderer3d.Camera3D
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed class CurrentScreen {
    object Home : CurrentScreen()
    object Game3D : CurrentScreen()
    object Puzzles : CurrentScreen()
    object History : CurrentScreen()
    object Settings : CurrentScreen()
}

data class UiState(
    val currentScreen: CurrentScreen = CurrentScreen.Home,
    val gameState: GameState = MoveGenerator.createInitialState(),
    val gameMode: GameMode = GameMode.VS_AI,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val playerColor: PieceColor = PieceColor.WHITE,
    val boardTheme: BoardTheme = BoardTheme.WOOD,
    val selectedSquare: Square? = null,
    val validMoves: List<Square> = emptyList(),
    val pendingPromotionMove: Move? = null,
    val showPromotionDialog: Boolean = false,
    val whiteTimeSeconds: Int = 600,
    val blackTimeSeconds: Int = 600,
    val isClockRunning: Boolean = false,
    val isAiThinking: Boolean = false,
    val aiCoachAdvice: String? = null,
    val isAiCoachLoading: Boolean = false,
    val showAiCoachDialog: Boolean = false,
    val activePuzzle: Puzzle? = null,
    val isPuzzleSolved: Boolean = false,
    val savedRecords: List<GameRecordEntity> = emptyList(),
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val camera: Camera3D = Camera3D()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val soundManager = SoundManager(application)
    private val database = ChessDatabase.getDatabase(application)
    private val chessDao = database.chessDao()

    private var timerJob: Job? = null
    private var aiJob: Job? = null
    private var gameStartTimeMs: Long = 0

    init {
        loadHistoryRecords()
    }

    fun navigateTo(screen: CurrentScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun startNewGame(mode: GameMode, difficulty: AiDifficulty = _uiState.value.aiDifficulty, color: PieceColor = PieceColor.WHITE) {
        timerJob?.cancel()
        aiJob?.cancel()

        val initialState = MoveGenerator.createInitialState()
        val initialCamera = Camera3D().apply {
            if (color == PieceColor.BLACK) resetToBlack() else resetToWhite()
        }

        gameStartTimeMs = System.currentTimeMillis()

        _uiState.update {
            it.copy(
                currentScreen = CurrentScreen.Game3D,
                gameState = initialState,
                gameMode = mode,
                aiDifficulty = difficulty,
                playerColor = color,
                selectedSquare = null,
                validMoves = emptyList(),
                showPromotionDialog = false,
                pendingPromotionMove = null,
                whiteTimeSeconds = 600,
                blackTimeSeconds = 600,
                isClockRunning = true,
                isAiThinking = false,
                aiCoachAdvice = null,
                activePuzzle = null,
                camera = initialCamera
            )
        }

        startTimerLoop()

        // If playing as Black vs AI, AI takes first turn
        if (mode == GameMode.VS_AI && color == PieceColor.BLACK) {
            triggerAiMove()
        }
    }

    fun startPuzzle(puzzle: Puzzle) {
        timerJob?.cancel()
        aiJob?.cancel()

        val state = MoveGenerator.createInitialState() // simplify puzzle state
        _uiState.update {
            it.copy(
                currentScreen = CurrentScreen.Game3D,
                gameState = state,
                gameMode = GameMode.PUZZLES,
                activePuzzle = puzzle,
                isPuzzleSolved = false,
                selectedSquare = null,
                validMoves = emptyList(),
                isClockRunning = false
            )
        }
    }

    fun onSquareClick(square: Square) {
        val state = _uiState.value
        if (state.gameState.isGameOver() || state.isAiThinking) return

        val selected = state.selectedSquare
        val currentPiece = state.gameState.getPiece(square)

        if (selected == null) {
            // Select piece if active color
            if (currentPiece != null && currentPiece.color == state.gameState.activeColor) {
                // If vs AI, only allow moving player's color
                if (state.gameMode == GameMode.VS_AI && currentPiece.color != state.playerColor) return

                val legalMoves = MoveGenerator.getLegalMoves(state.gameState, square)
                val targetSquares = legalMoves.map { it.to }

                _uiState.update {
                    it.copy(selectedSquare = square, validMoves = targetSquares)
                }
                soundManager.vibrate(20)
            }
        } else {
            // Try to move to square
            val legalMoves = MoveGenerator.getLegalMoves(state.gameState, selected)
            val matchingMove = legalMoves.find { it.to == square }

            if (matchingMove != null) {
                if (matchingMove.moveType == MoveType.PROMOTION && matchingMove.promotionPieceType == null) {
                    _uiState.update {
                        it.copy(pendingPromotionMove = matchingMove, showPromotionDialog = true)
                    }
                } else {
                    executeMove(matchingMove)
                }
            } else if (currentPiece != null && currentPiece.color == state.gameState.activeColor) {
                // Switch selection to new piece
                val newLegal = MoveGenerator.getLegalMoves(state.gameState, square)
                _uiState.update {
                    it.copy(selectedSquare = square, validMoves = newLegal.map { m -> m.to })
                }
            } else {
                // Deselect
                _uiState.update { it.copy(selectedSquare = null, validMoves = emptyList()) }
            }
        }
    }

    fun onPromotionSelected(pieceType: PieceType) {
        val pending = _uiState.value.pendingPromotionMove ?: return
        val finalMove = pending.copy(promotionPieceType = pieceType)
        _uiState.update { it.copy(showPromotionDialog = false, pendingPromotionMove = null) }
        executeMove(finalMove)
    }

    private fun executeMove(move: Move) {
        val prevState = _uiState.value.gameState
        val nextState = MoveGenerator.makeMove(prevState, move)

        val isCapture = move.capturedPiece != null || move.moveType == MoveType.EN_PASSANT
        if (nextState.isCheck) {
            soundManager.playCheckSound()
        } else if (isCapture) {
            soundManager.playCaptureSound()
        } else {
            soundManager.playMoveSound()
        }

        _uiState.update {
            it.copy(
                gameState = nextState,
                selectedSquare = null,
                validMoves = emptyList()
            )
        }

        if (nextState.isGameOver()) {
            soundManager.playVictorySound()
            saveGameRecord(nextState)
            return
        }

        // Trigger AI move if VS_AI or SPECTATE
        if ((_uiState.value.gameMode == GameMode.VS_AI && nextState.activeColor != _uiState.value.playerColor) ||
            _uiState.value.gameMode == GameMode.SPECTATE) {
            triggerAiMove()
        }
    }

    private fun triggerAiMove() {
        aiJob?.cancel()
        _uiState.update { it.copy(isAiThinking = true) }

        aiJob = viewModelScope.launch {
            delay(350) // Realistic thinking delay
            val currentState = _uiState.value.gameState
            val depth = _uiState.value.aiDifficulty.searchDepth

            val bestMove = MinimaxEngine.findBestMove(currentState, depth)

            _uiState.update { it.copy(isAiThinking = false) }

            if (bestMove != null) {
                executeMove(bestMove)
            }
        }
    }

    fun undoMove() {
        val history = _uiState.value.gameState.moveHistory
        if (history.isEmpty() || _uiState.value.isAiThinking) return

        // Undo 2 moves if vs AI, or 1 move if pass & play
        val popCount = if (_uiState.value.gameMode == GameMode.VS_AI && history.size >= 2) 2 else 1
        val targetHistory = history.dropLast(popCount)

        var rebuiltState = MoveGenerator.createInitialState()
        for (m in targetHistory) {
            rebuiltState = MoveGenerator.makeMove(rebuiltState, m)
        }

        _uiState.update {
            it.copy(
                gameState = rebuiltState,
                selectedSquare = null,
                validMoves = emptyList()
            )
        }
        soundManager.playMoveSound()
    }

    fun fetchAiCoachAdvice() {
        _uiState.update { it.copy(showAiCoachDialog = true, isAiCoachLoading = true) }
        viewModelScope.launch {
            val lastMove = _uiState.value.gameState.moveHistory.lastOrNull()
            val advice = GeminiChessService.getMoveAdvice(_uiState.value.gameState, lastMove)
            _uiState.update { it.copy(aiCoachAdvice = advice, isAiCoachLoading = false) }
        }
    }

    fun dismissAiCoach() {
        _uiState.update { it.copy(showAiCoachDialog = false) }
    }

    fun rotateCamera(deltaYaw: Float, deltaPitch: Float) {
        _uiState.value.camera.rotate(deltaYaw, deltaPitch)
        _uiState.update { it.copy() }
    }

    fun zoomCamera(factor: Float) {
        _uiState.value.camera.zoom(factor)
        _uiState.update { it.copy() }
    }

    fun resetCameraView() {
        if (_uiState.value.playerColor == PieceColor.BLACK) {
            _uiState.value.camera.resetToBlack()
        } else {
            _uiState.value.camera.resetToWhite()
        }
        _uiState.update { it.copy() }
    }

    fun toggleCamera2D() {
        _uiState.value.camera.toggle2D()
        _uiState.update { it.copy() }
    }

    fun setBoardTheme(theme: BoardTheme) {
        _uiState.update { it.copy(boardTheme = theme) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundManager.isSoundEnabled = enabled
        _uiState.update { it.copy(isSoundEnabled = enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        soundManager.isHapticsEnabled = enabled
        _uiState.update { it.copy(isHapticsEnabled = enabled) }
    }

    private fun startTimerLoop() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (!state.isClockRunning || state.gameState.isGameOver()) continue

                if (state.gameState.activeColor == PieceColor.WHITE) {
                    val remaining = state.whiteTimeSeconds - 1
                    if (remaining <= 0) {
                        _uiState.update {
                            it.copy(
                                whiteTimeSeconds = 0,
                                gameState = it.gameState.copy(isDraw = true, drawReason = "White Flagged on Time")
                            )
                        }
                        soundManager.playVictorySound()
                        break
                    } else {
                        _uiState.update { it.copy(whiteTimeSeconds = remaining) }
                    }
                } else {
                    val remaining = state.blackTimeSeconds - 1
                    if (remaining <= 0) {
                        _uiState.update {
                            it.copy(
                                blackTimeSeconds = 0,
                                gameState = it.gameState.copy(isDraw = true, drawReason = "Black Flagged on Time")
                            )
                        }
                        soundManager.playVictorySound()
                        break
                    } else {
                        _uiState.update { it.copy(blackTimeSeconds = remaining) }
                    }
                }
            }
        }
    }

    private fun saveGameRecord(state: GameState) {
        viewModelScope.launch {
            val durationSec = (System.currentTimeMillis() - gameStartTimeMs) / 1000
            val pgnStr = state.moveHistory.joinToString(" ") { it.san }
            val record = GameRecordEntity(
                id = UUID.randomUUID().toString(),
                mode = _uiState.value.gameMode.name,
                result = if (state.isCheckmate) "Checkmate" else "Draw",
                winnerColor = if (state.isCheckmate) state.activeColor.opposite().name else null,
                pgn = pgnStr,
                moveCount = state.moveCount,
                durationSeconds = durationSec,
                timestamp = System.currentTimeMillis()
            )
            chessDao.insertGameRecord(record)
            loadHistoryRecords()
        }
    }

    private fun loadHistoryRecords() {
        viewModelScope.launch {
            val records = chessDao.getAllGameRecords()
            _uiState.update { it.copy(savedRecords = records) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
