package com.example.chess.engine

import com.example.chess.model.ChessPiece
import com.example.chess.model.GameState
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square

object ChessEvaluator {

    // Piece-Square Tables (White perspective, rank 0 to 7)
    private val PAWN_TABLE = arrayOf(
        intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf( 5,  5, 10, 25, 25, 10,  5,  5),
        intArrayOf( 0,  0,  0, 20, 20,  0,  0,  0),
        intArrayOf( 5, -5,-10,  0,  0,-10, -5,  5),
        intArrayOf( 5, 10, 10,-20,-20, 10, 10,  5),
        intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0)
    )

    private val KNIGHT_TABLE = arrayOf(
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50),
        intArrayOf(-40,-20,  0,  0,  0,  0,-20,-40),
        intArrayOf(-30,  0, 10, 15, 15, 10,  0,-30),
        intArrayOf(-30,  5, 15, 20, 20, 15,  5,-30),
        intArrayOf(-30,  0, 15, 20, 20, 15,  0,-30),
        intArrayOf(-30,  5, 10, 15, 15, 10,  5,-30),
        intArrayOf(-40,-20,  0,  5,  5,  0,-20,-40),
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50)
    )

    private val BISHOP_TABLE = arrayOf(
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20),
        intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
        intArrayOf(-10,  0,  5, 10, 10,  5,  0,-10),
        intArrayOf(-10,  5,  5, 10, 10,  5,  5,-10),
        intArrayOf(-10,  0, 10, 10, 10, 10,  0,-10),
        intArrayOf(-10, 10, 10, 10, 10, 10, 10,-10),
        intArrayOf(-10,  5,  0,  0,  0,  0,  5,-10),
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20)
    )

    private val ROOK_TABLE = arrayOf(
        intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf( 5, 10, 10, 10, 10, 10, 10,  5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf( 0,  0,  0,  5,  5,  0,  0,  0)
    )

    private val QUEEN_TABLE = arrayOf(
        intArrayOf(-20,-10,-10, -5, -5,-10,-10,-20),
        intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
        intArrayOf(-10,  0,  5,  5,  5,  5,  0,-10),
        intArrayOf( -5,  0,  5,  5,  5,  5,  0, -5),
        intArrayOf(  0,  0,  5,  5,  5,  5,  0, -5),
        intArrayOf(-10,  5,  5,  5,  5,  5,  0,-10),
        intArrayOf(-10,  0,  5,  0,  0,  0,  0,-10),
        intArrayOf(-20,-10,-10, -5, -5,-10,-10,-20)
    )

    private val KING_MIDDLE_TABLE = arrayOf(
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-20,-30,-30,-40,-40,-30,-30,-20),
        intArrayOf(-10,-20,-20,-20,-20,-20,-20,-10),
        intArrayOf( 20, 20,  0,  0,  0,  0, 20, 20),
        intArrayOf( 20, 30, 10,  0,  0, 10, 30, 20)
    )

    fun evaluate(state: GameState): Float {
        if (state.isCheckmate) {
            return if (state.activeColor == PieceColor.WHITE) -1000.0f else 1000.0f
        }
        if (state.isStalemate || state.isDraw) return 0.0f

        var whiteScore = 0
        var blackScore = 0

        for (f in 0..7) {
            for (r in 0..7) {
                val piece = state.board[f][r] ?: continue
                val pValue = piece.type.baseValue
                val pstValue = getPstValue(piece, f, r)

                val totalPieceScore = pValue + pstValue
                if (piece.color == PieceColor.WHITE) {
                    whiteScore += totalPieceScore
                } else {
                    blackScore += totalPieceScore
                }
            }
        }

        val diff = whiteScore - blackScore
        return diff / 100.0f
    }

    private fun getPstValue(piece: ChessPiece, file: Int, rank: Int): Int {
        val r = if (piece.color == PieceColor.WHITE) rank else 7 - rank
        val f = file
        val table = when (piece.type) {
            PieceType.PAWN -> PAWN_TABLE
            PieceType.KNIGHT -> KNIGHT_TABLE
            PieceType.BISHOP -> BISHOP_TABLE
            PieceType.ROOK -> ROOK_TABLE
            PieceType.QUEEN -> QUEEN_TABLE
            PieceType.KING -> KING_MIDDLE_TABLE
        }
        return table[r][f]
    }
}
