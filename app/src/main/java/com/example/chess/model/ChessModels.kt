package com.example.chess.model

import androidx.compose.ui.graphics.Color

enum class PieceType(val symbol: String, val sanChar: String, val baseValue: Int) {
    PAWN("P", "", 100),
    KNIGHT("N", "N", 320),
    BISHOP("B", "B", 330),
    ROOK("R", "R", 500),
    QUEEN("Q", "Q", 900),
    KING("K", "K", 20000)
}

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

data class Square(val file: Int, val rank: Int) {
    init {
        require(file in 0..7 && rank in 0..7) { "Invalid square coordinates: ($file, $rank)" }
    }

    val isLight: Boolean get() = (file + rank) % 2 != 0

    val notation: String get() = "${('a' + file)}${rank + 1}"

    companion object {
        fun fromNotation(notation: String): Square? {
            if (notation.length != 2) return null
            val file = notation[0].lowercaseChar() - 'a'
            val rank = notation[1] - '1'
            return if (file in 0..7 && rank in 0..7) Square(file, rank) else null
        }
    }
}

data class ChessPiece(
    val id: String,
    val type: PieceType,
    val color: PieceColor,
    val moveCount: Int = 0
)

enum class MoveType {
    NORMAL,
    PAWN_DOUBLE,
    CASTLING_KINGSIDE,
    CASTLING_QUEENSIDE,
    EN_PASSANT,
    PROMOTION
}

data class Move(
    val from: Square,
    val to: Square,
    val piece: ChessPiece,
    val capturedPiece: ChessPiece? = null,
    val moveType: MoveType = MoveType.NORMAL,
    val promotionPieceType: PieceType? = null,
    val san: String = ""
)

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
)

enum class GameMode {
    VS_AI,
    PASS_AND_PLAY,
    SPECTATE,
    PUZZLES
}

enum class AiDifficulty(val displayName: String, val searchDepth: Int) {
    EASY("Novice (Depth 1)", 1),
    MEDIUM("Intermediate (Depth 2)", 2),
    HARD("Master (Depth 3)", 3),
    GEMINI_GRANDMASTER("Gemini Grandmaster", 4)
}

enum class BoardTheme(
    val displayName: String,
    val lightSquareColor: Long,
    val darkSquareColor: Long,
    val frameColor: Long,
    val whitePieceColor: Long,
    val blackPieceColor: Long,
    val whiteAccentColor: Long,
    val blackAccentColor: Long
) {
    WOOD(
        displayName = "Mahogany & Ivory",
        lightSquareColor = 0xFFF0D9B5,
        darkSquareColor = 0xFFB58863,
        frameColor = 0xFF4A2E1B,
        whitePieceColor = 0xFFFFF8EA,
        blackPieceColor = 0xFF2B1F1D,
        whiteAccentColor = 0xFFD4AF37,
        blackAccentColor = 0xFF8B4513
    ),
    MARBLE(
        displayName = "Obsidian & Pearl",
        lightSquareColor = 0xFFE0E6ED,
        darkSquareColor = 0xFF4A5568,
        frameColor = 0xFF1A202C,
        whitePieceColor = 0xFFF7FAFC,
        blackPieceColor = 0xFF171923,
        whiteAccentColor = 0xFF63B3ED,
        blackAccentColor = 0xFF9F7AEA
    ),
    CYBER_NEON(
        displayName = "Cyberpunk Neon",
        lightSquareColor = 0xFF1E293B,
        darkSquareColor = 0xFF0F172A,
        frameColor = 0xFF020617,
        whitePieceColor = 0xFF38BDF8,
        blackPieceColor = 0xFFF43F5E,
        whiteAccentColor = 0xFF00F0FF,
        blackAccentColor = 0xFFFF0055
    ),
    ROYAL_GOLD(
        displayName = "Royal Gold & Silver",
        lightSquareColor = 0xFFE2E8F0,
        darkSquareColor = 0xFF64748B,
        frameColor = 0xFF1E293B,
        whitePieceColor = 0xFFFFD700,
        blackPieceColor = 0xFF94A3B8,
        whiteAccentColor = 0xFFFFF5A5,
        blackAccentColor = 0xFFCBD5E1
    )
}

data class GameState(
    val board: Array<Array<ChessPiece?>> = Array(8) { Array(8) { null } },
    val activeColor: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantSquare: Square? = null,
    val moveHistory: List<Move> = emptyList(),
    val moveCount: Int = 0,
    val halfmoveClock: Int = 0,
    val fullmoveNumber: Int = 1,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isDraw: Boolean = false,
    val drawReason: String? = null,
    val capturedWhitePieces: List<ChessPiece> = emptyList(),
    val capturedBlackPieces: List<ChessPiece> = emptyList(),
    val evaluationScore: Float = 0.0f
) {
    fun getPiece(square: Square): ChessPiece? = board[square.file][square.rank]

    fun isGameOver(): Boolean = isCheckmate || isStalemate || isDraw

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return activeColor == other.activeColor &&
                moveCount == other.moveCount &&
                isCheck == other.isCheck &&
                isCheckmate == other.isCheckmate &&
                isStalemate == other.isStalemate &&
                isDraw == other.isDraw
    }

    override fun hashCode(): Int {
        var result = activeColor.hashCode()
        result = 31 * result + moveCount
        result = 31 * result + isCheck.hashCode()
        return result
    }
}

data class Puzzle(
    val id: String,
    val title: String,
    val description: String,
    val fen: String,
    val solutionMovesNotation: List<String>,
    val difficulty: String,
    val rating: Int
)
