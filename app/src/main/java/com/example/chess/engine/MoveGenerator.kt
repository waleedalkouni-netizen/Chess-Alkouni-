package com.example.chess.engine

import com.example.chess.model.*
import java.util.UUID

object MoveGenerator {

    fun createInitialState(): GameState {
        val board = Array(8) { Array<ChessPiece?>(8) { null } }

        fun placeRow(rank: Int, color: PieceColor) {
            val pieces = listOf(
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
            )
            for (file in 0..7) {
                val pieceType = pieces[file]
                board[file][rank] = ChessPiece(
                    id = "${color.name.lowercase()}_${pieceType.name.lowercase()}_$file",
                    type = pieceType,
                    color = color
                )
            }
        }

        fun placePawns(rank: Int, color: PieceColor) {
            for (file in 0..7) {
                board[file][rank] = ChessPiece(
                    id = "${color.name.lowercase()}_pawn_$file",
                    type = PieceType.PAWN,
                    color = color
                )
            }
        }

        placeRow(0, PieceColor.WHITE)
        placePawns(1, PieceColor.WHITE)
        placePawns(6, PieceColor.BLACK)
        placeRow(7, PieceColor.BLACK)

        return GameState(
            board = board,
            activeColor = PieceColor.WHITE,
            castlingRights = CastlingRights(),
            enPassantSquare = null
        )
    }

    fun getLegalMoves(state: GameState, square: Square): List<Move> {
        val piece = state.getPiece(square) ?: return emptyList()
        if (piece.color != state.activeColor) return emptyList()

        val pseudoMoves = getPseudoLegalMoves(state, square)
        return pseudoMoves.filter { move ->
            val nextState = applyMoveUnchecked(state, move)
            !isInCheck(nextState, piece.color)
        }
    }

    fun getAllLegalMoves(state: GameState): List<Move> {
        val legalMoves = mutableListOf<Move>()
        for (file in 0..7) {
            for (rank in 0..7) {
                val sq = Square(file, rank)
                val piece = state.getPiece(sq)
                if (piece != null && piece.color == state.activeColor) {
                    legalMoves.addAll(getLegalMoves(state, sq))
                }
            }
        }
        return legalMoves
    }

    fun isInCheck(state: GameState, color: PieceColor): Boolean {
        val kingSquare = findKing(state, color) ?: return false
        val enemyColor = color.opposite()

        for (f in 0..7) {
            for (r in 0..7) {
                val sq = Square(f, r)
                val p = state.getPiece(sq)
                if (p != null && p.color == enemyColor) {
                    val attacks = getAttacks(state, sq, p)
                    if (attacks.contains(kingSquare)) return true
                }
            }
        }
        return false
    }

    private fun findKing(state: GameState, color: PieceColor): Square? {
        for (f in 0..7) {
            for (r in 0..7) {
                val p = state.board[f][r]
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    return Square(f, r)
                }
            }
        }
        return null
    }

    private fun getPseudoLegalMoves(state: GameState, from: Square): List<Move> {
        val piece = state.getPiece(from) ?: return emptyList()
        val moves = mutableListOf<Move>()

        when (piece.type) {
            PieceType.PAWN -> getPawnMoves(state, from, piece, moves)
            PieceType.KNIGHT -> getKnightMoves(state, from, piece, moves)
            PieceType.BISHOP -> getSlidingMoves(state, from, piece, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), moves)
            PieceType.ROOK -> getSlidingMoves(state, from, piece, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), moves)
            PieceType.QUEEN -> getSlidingMoves(state, from, piece, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1), moves)
            PieceType.KING -> getKingMoves(state, from, piece, moves)
        }

        return moves
    }

    private fun getAttacks(state: GameState, from: Square, piece: ChessPiece): List<Square> {
        val attacks = mutableListOf<Square>()
        val f = from.file
        val r = from.rank

        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) 1 else -1
                for (df in listOf(-1, 1)) {
                    val nf = f + df
                    val nr = r + dir
                    if (nf in 0..7 && nr in 0..7) attacks.add(Square(nf, nr))
                }
            }
            PieceType.KNIGHT -> {
                val knightOffsets = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for ((df, dr) in knightOffsets) {
                    val nf = f + df
                    val nr = r + dr
                    if (nf in 0..7 && nr in 0..7) attacks.add(Square(nf, nr))
                }
            }
            PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN -> {
                val dirs = when (piece.type) {
                    PieceType.BISHOP -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
                    PieceType.ROOK -> listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                    else -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1)
                }
                for ((df, dr) in dirs) {
                    var nf = f + df
                    var nr = r + dr
                    while (nf in 0..7 && nr in 0..7) {
                        val sq = Square(nf, nr)
                        attacks.add(sq)
                        if (state.getPiece(sq) != null) break
                        nf += df
                        nr += dr
                    }
                }
            }
            PieceType.KING -> {
                for (df in -1..1) {
                    for (dr in -1..1) {
                        if (df == 0 && dr == 0) continue
                        val nf = f + df
                        val nr = r + dr
                        if (nf in 0..7 && nr in 0..7) attacks.add(Square(nf, nr))
                    }
                }
            }
        }
        return attacks
    }

    private fun getPawnMoves(state: GameState, from: Square, piece: ChessPiece, moves: MutableList<Move>) {
        val dir = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRank = if (piece.color == PieceColor.WHITE) 1 else 6
        val promoRank = if (piece.color == PieceColor.WHITE) 7 else 0

        val f = from.file
        val r = from.rank

        // One step forward
        val r1 = r + dir
        if (r1 in 0..7 && state.getPiece(Square(f, r1)) == null) {
            val target = Square(f, r1)
            if (r1 == promoRank) {
                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { pType ->
                    moves.add(Move(from, target, piece, moveType = MoveType.PROMOTION, promotionPieceType = pType))
                }
            } else {
                moves.add(Move(from, target, piece, moveType = MoveType.NORMAL))
            }

            // Two steps forward
            val r2 = r + 2 * dir
            if (r == startRank && r2 in 0..7 && state.getPiece(Square(f, r2)) == null) {
                moves.add(Move(from, Square(f, r2), piece, moveType = MoveType.PAWN_DOUBLE))
            }
        }

        // Captures
        for (df in listOf(-1, 1)) {
            val nf = f + df
            val nr = r + dir
            if (nf in 0..7 && nr in 0..7) {
                val targetSq = Square(nf, nr)
                val targetPiece = state.getPiece(targetSq)
                if (targetPiece != null && targetPiece.color != piece.color) {
                    if (nr == promoRank) {
                        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { pType ->
                            moves.add(Move(from, targetSq, piece, targetPiece, moveType = MoveType.PROMOTION, promotionPieceType = pType))
                        }
                    } else {
                        moves.add(Move(from, targetSq, piece, targetPiece, moveType = MoveType.NORMAL))
                    }
                } else if (targetSq == state.enPassantSquare) {
                    val capturedPawnSq = Square(nf, r)
                    val capturedPawn = state.getPiece(capturedPawnSq)
                    moves.add(Move(from, targetSq, piece, capturedPawn, moveType = MoveType.EN_PASSANT))
                }
            }
        }
    }

    private fun getKnightMoves(state: GameState, from: Square, piece: ChessPiece, moves: MutableList<Move>) {
        val offsets = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        for ((df, dr) in offsets) {
            val nf = from.file + df
            val nr = from.rank + dr
            if (nf in 0..7 && nr in 0..7) {
                val targetSq = Square(nf, nr)
                val dest = state.getPiece(targetSq)
                if (dest == null || dest.color != piece.color) {
                    moves.add(Move(from, targetSq, piece, dest))
                }
            }
        }
    }

    private fun getSlidingMoves(
        state: GameState,
        from: Square,
        piece: ChessPiece,
        directions: List<Pair<Int, Int>>,
        moves: MutableList<Move>
    ) {
        for ((df, dr) in directions) {
            var nf = from.file + df
            var nr = from.rank + dr
            while (nf in 0..7 && nr in 0..7) {
                val targetSq = Square(nf, nr)
                val dest = state.getPiece(targetSq)
                if (dest == null) {
                    moves.add(Move(from, targetSq, piece))
                } else {
                    if (dest.color != piece.color) {
                        moves.add(Move(from, targetSq, piece, dest))
                    }
                    break
                }
                nf += df
                nr += dr
            }
        }
    }

    private fun getKingMoves(state: GameState, from: Square, piece: ChessPiece, moves: MutableList<Move>) {
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val nf = from.file + df
                val nr = from.rank + dr
                if (nf in 0..7 && nr in 0..7) {
                    val targetSq = Square(nf, nr)
                    val dest = state.getPiece(targetSq)
                    if (dest == null || dest.color != piece.color) {
                        moves.add(Move(from, targetSq, piece, dest))
                    }
                }
            }
        }

        // Castling
        if (piece.moveCount == 0 && !isInCheck(state, piece.color)) {
            val rank = if (piece.color == PieceColor.WHITE) 0 else 7
            val cr = state.castlingRights

            // Kingside
            val canKingside = if (piece.color == PieceColor.WHITE) cr.whiteKingside else cr.blackKingside
            if (canKingside) {
                val f1 = Square(5, rank)
                val f2 = Square(6, rank)
                val rookSq = Square(7, rank)
                val rook = state.getPiece(rookSq)
                if (state.getPiece(f1) == null && state.getPiece(f2) == null && rook?.type == PieceType.ROOK && rook.moveCount == 0) {
                    if (!isSquareAttacked(state, f1, piece.color.opposite()) && !isSquareAttacked(state, f2, piece.color.opposite())) {
                        moves.add(Move(from, f2, piece, moveType = MoveType.CASTLING_KINGSIDE))
                    }
                }
            }

            // Queenside
            val canQueenside = if (piece.color == PieceColor.WHITE) cr.whiteQueenside else cr.blackQueenside
            if (canQueenside) {
                val d = Square(3, rank)
                val c = Square(2, rank)
                val b = Square(1, rank)
                val rookSq = Square(0, rank)
                val rook = state.getPiece(rookSq)
                if (state.getPiece(d) == null && state.getPiece(c) == null && state.getPiece(b) == null && rook?.type == PieceType.ROOK && rook.moveCount == 0) {
                    if (!isSquareAttacked(state, d, piece.color.opposite()) && !isSquareAttacked(state, c, piece.color.opposite())) {
                        moves.add(Move(from, c, piece, moveType = MoveType.CASTLING_QUEENSIDE))
                    }
                }
            }
        }
    }

    private fun isSquareAttacked(state: GameState, square: Square, attackerColor: PieceColor): Boolean {
        for (f in 0..7) {
            for (r in 0..7) {
                val sq = Square(f, r)
                val p = state.getPiece(sq)
                if (p != null && p.color == attackerColor) {
                    if (getAttacks(state, sq, p).contains(square)) return true
                }
            }
        }
        return false
    }

    fun makeMove(state: GameState, rawMove: Move): GameState {
        val san = computeSan(state, rawMove)
        val move = rawMove.copy(san = san)
        val nextState = applyMoveUnchecked(state, move)

        val nextActive = state.activeColor.opposite()
        val inCheck = isInCheck(nextState, nextActive)
        val legalMoves = getAllLegalMoves(nextState)
        val isCheckmate = inCheck && legalMoves.isEmpty()
        val isStalemate = !inCheck && legalMoves.isEmpty()
        val isDraw = isStalemate || nextState.halfmoveClock >= 100

        val eval = ChessEvaluator.evaluate(nextState)

        val finalSan = when {
            isCheckmate -> "$san#"
            inCheck -> "$san+"
            else -> san
        }

        return nextState.copy(
            moveHistory = state.moveHistory + move.copy(san = finalSan),
            moveCount = state.moveCount + 1,
            isCheck = inCheck,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate,
            isDraw = isDraw,
            drawReason = if (isStalemate) "Stalemate" else if (nextState.halfmoveClock >= 100) "50-Move Rule" else null,
            evaluationScore = eval
        )
    }

    private fun applyMoveUnchecked(state: GameState, move: Move): GameState {
        val newBoard = Array(8) { f -> Array(8) { r -> state.board[f][r] } }
        val piece = move.piece
        val from = move.from
        val to = move.to

        newBoard[from.file][from.rank] = null

        val updatedPiece = when (move.moveType) {
            MoveType.PROMOTION -> ChessPiece(
                id = piece.id,
                type = move.promotionPieceType ?: PieceType.QUEEN,
                color = piece.color,
                moveCount = piece.moveCount + 1
            )
            else -> piece.copy(moveCount = piece.moveCount + 1)
        }
        newBoard[to.file][to.rank] = updatedPiece

        var captured = move.capturedPiece
        val newCapturedWhite = state.capturedWhitePieces.toMutableList()
        val newCapturedBlack = state.capturedBlackPieces.toMutableList()

        if (move.moveType == MoveType.EN_PASSANT) {
            val pawnRank = if (piece.color == PieceColor.WHITE) to.rank - 1 else to.rank + 1
            val capPawnSq = Square(to.file, pawnRank)
            captured = state.getPiece(capPawnSq)
            newBoard[to.file][pawnRank] = null
        }

        captured?.let {
            if (it.color == PieceColor.WHITE) newCapturedWhite.add(it)
            else newCapturedBlack.add(it)
        }

        // Handle Rook move for Castling
        val rank = if (piece.color == PieceColor.WHITE) 0 else 7
        if (move.moveType == MoveType.CASTLING_KINGSIDE) {
            val rook = newBoard[7][rank]
            newBoard[7][rank] = null
            newBoard[5][rank] = rook?.copy(moveCount = (rook.moveCount ?: 0) + 1)
        } else if (move.moveType == MoveType.CASTLING_QUEENSIDE) {
            val rook = newBoard[0][rank]
            newBoard[0][rank] = null
            newBoard[3][rank] = rook?.copy(moveCount = (rook.moveCount ?: 0) + 1)
        }

        // Update Castling Rights
        var wK = state.castlingRights.whiteKingside
        var wQ = state.castlingRights.whiteQueenside
        var bK = state.castlingRights.blackKingside
        var bQ = state.castlingRights.blackQueenside

        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) { wK = false; wQ = false }
            else { bK = false; bQ = false }
        }
        if (piece.type == PieceType.ROOK) {
            if (from.file == 0 && from.rank == 0) wQ = false
            if (from.file == 7 && from.rank == 0) wK = false
            if (from.file == 0 && from.rank == 7) bQ = false
            if (from.file == 7 && from.rank == 7) bK = false
        }

        val enPassantSq = if (move.moveType == MoveType.PAWN_DOUBLE) {
            val epRank = if (piece.color == PieceColor.WHITE) from.rank + 1 else from.rank - 1
            Square(from.file, epRank)
        } else null

        val halfmove = if (piece.type == PieceType.PAWN || captured != null) 0 else state.halfmoveClock + 1
        val fullmove = if (state.activeColor == PieceColor.BLACK) state.fullmoveNumber + 1 else state.fullmoveNumber

        return GameState(
            board = newBoard,
            activeColor = state.activeColor.opposite(),
            castlingRights = CastlingRights(wK, wQ, bK, bQ),
            enPassantSquare = enPassantSq,
            moveHistory = state.moveHistory,
            moveCount = state.moveCount,
            halfmoveClock = halfmove,
            fullmoveNumber = fullmove,
            capturedWhitePieces = newCapturedWhite,
            capturedBlackPieces = newCapturedBlack
        )
    }

    private fun computeSan(state: GameState, move: Move): String {
        if (move.moveType == MoveType.CASTLING_KINGSIDE) return "O-O"
        if (move.moveType == MoveType.CASTLING_QUEENSIDE) return "O-O-O"

        val piece = move.piece
        val isCapture = move.capturedPiece != null || move.moveType == MoveType.EN_PASSANT
        val sb = StringBuilder()

        if (piece.type != PieceType.PAWN) {
            sb.append(piece.type.sanChar)
            // Disambiguation
            val siblings = getAllLegalMoves(state).filter {
                it.piece.type == piece.type && it.to == move.to && it.from != move.from
            }
            if (siblings.isNotEmpty()) {
                val sameFile = siblings.any { it.from.file == move.from.file }
                val sameRank = siblings.any { it.from.rank == move.from.rank }
                if (!sameFile) {
                    sb.append(('a' + move.from.file))
                } else if (!sameRank) {
                    sb.append((move.from.rank + 1).toString())
                } else {
                    sb.append(move.from.notation)
                }
            }
        } else if (isCapture) {
            sb.append(('a' + move.from.file))
        }

        if (isCapture) sb.append("x")
        sb.append(move.to.notation)

        if (move.moveType == MoveType.PROMOTION && move.promotionPieceType != null) {
            sb.append("=").append(move.promotionPieceType.sanChar)
        }

        return sb.toString()
    }
}
