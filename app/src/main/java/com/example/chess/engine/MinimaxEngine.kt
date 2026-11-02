package com.example.chess.engine

import com.example.chess.model.GameState
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object MinimaxEngine {

    suspend fun findBestMove(state: GameState, depth: Int): Move? = withContext(Dispatchers.Default) {
        val legalMoves = MoveGenerator.getAllLegalMoves(state)
        if (legalMoves.isEmpty()) return@withContext null

        if (depth <= 1) {
            // Sort moves by capture value or random if easy
            val sortedMoves = legalMoves.sortedByDescending { move ->
                (move.capturedPiece?.type?.baseValue ?: 0) - (move.piece.type.baseValue / 10)
            }
            return@withContext sortedMoves.firstOrNull()
        }

        var bestMove: Move? = null
        var isMaximizing = state.activeColor == PieceColor.WHITE

        var alpha = -100000.0f
        var beta = 100000.0f

        val sortedMoves = legalMoves.sortedByDescending { move ->
            (move.capturedPiece?.type?.baseValue ?: 0) * 10 - move.piece.type.baseValue
        }

        for (move in sortedMoves) {
            val nextState = MoveGenerator.makeMove(state, move)
            val score = minimax(nextState, depth - 1, alpha, beta, !isMaximizing)

            if (isMaximizing) {
                if (score > alpha) {
                    alpha = score
                    bestMove = move
                }
            } else {
                if (score < beta) {
                    beta = score
                    bestMove = move
                }
            }
        }

        return@withContext bestMove ?: legalMoves.randomOrNull()
    }

    private fun minimax(state: GameState, depth: Int, alpha: Float, beta: Float, isMaximizing: Boolean): Float {
        if (depth == 0 || state.isGameOver()) {
            return ChessEvaluator.evaluate(state)
        }

        var currAlpha = alpha
        var currBeta = beta
        val legalMoves = MoveGenerator.getAllLegalMoves(state)

        if (legalMoves.isEmpty()) {
            return ChessEvaluator.evaluate(state)
        }

        val sortedMoves = legalMoves.sortedByDescending { move ->
            (move.capturedPiece?.type?.baseValue ?: 0) * 10 - move.piece.type.baseValue
        }

        if (isMaximizing) {
            var maxEval = -100000.0f
            for (move in sortedMoves) {
                val nextState = MoveGenerator.makeMove(state, move)
                val eval = minimax(nextState, depth - 1, currAlpha, currBeta, false)
                maxEval = max(maxEval, eval)
                currAlpha = max(currAlpha, eval)
                if (currBeta <= currAlpha) break
            }
            return maxEval
        } else {
            var minEval = 100000.0f
            for (move in sortedMoves) {
                val nextState = MoveGenerator.makeMove(state, move)
                val eval = minimax(nextState, depth - 1, currAlpha, currBeta, true)
                minEval = min(minEval, eval)
                currBeta = min(currBeta, eval)
                if (currBeta <= currAlpha) break
            }
            return minEval
        }
    }
}
