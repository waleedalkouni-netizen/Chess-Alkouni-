package com.example.chess.data

import com.example.chess.model.Puzzle

object PuzzleProvider {
    val samplePuzzles = listOf(
        Puzzle(
            id = "puz_1",
            title = "Scholar's Mate Finish",
            description = "White to move. Deliver checkmate in 1 move with Queen to f7!",
            fen = "r1bqkb1r/pppp1ppp/2n5/4p3/2B1P3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 1",
            solutionMovesNotation = listOf("Qxf7#"),
            difficulty = "Beginner",
            rating = 800
        ),
        Puzzle(
            id = "puz_2",
            title = "Classic Back-Rank Mate",
            description = "White to move. Black's king is trapped behind pawns on g7/h7. Deliver mate with Rook to d8!",
            fen = "3r2k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            solutionMovesNotation = listOf("Rxd8#"),
            difficulty = "Easy",
            rating = 1000
        ),
        Puzzle(
            id = "puz_3",
            title = "Knight Fork Tactical Strike",
            description = "White to move. Deliver a lethal Knight fork on c7 checking king and attacking rook!",
            fen = "r1bqk2r/pppp1ppp/2n2n2/4p3/2B1P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 0 1",
            solutionMovesNotation = listOf("Nxf7"),
            difficulty = "Intermediate",
            rating = 1300
        ),
        Puzzle(
            id = "puz_4",
            title = "Smothered Mate Elegance",
            description = "White to move. Use your Knight to deliver a smothered checkmate to the cornered king!",
            fen = "6rk/5Npp/8/8/8/8/5PPP/6K1 w - - 0 1",
            solutionMovesNotation = listOf("Nf7#"),
            difficulty = "Advanced",
            rating = 1600
        )
    )
}
