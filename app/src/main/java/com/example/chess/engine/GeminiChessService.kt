package com.example.chess.engine

import com.example.chess.model.GameState
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiChessService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String ?: ""
            if (key == "MY_GEMINI_API_KEY") "" else key
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun getMoveAdvice(state: GameState, lastMove: Move?): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val movesNotation = state.moveHistory.takeLast(10).joinToString(" ") { it.san }
        val activeColor = if (state.activeColor == PieceColor.WHITE) "White" else "Black"

        if (apiKey.isEmpty()) {
            // Smart local fallback if API key not available yet
            val eval = state.evaluationScore
            val status = when {
                state.isCheck -> "Warning! $activeColor is currently in CHECK! Protect your King immediately."
                eval > 1.5f -> "White has a strong positional advantage (+${String.format("%.1f", eval)}). Focus on controlling the center."
                eval < -1.5f -> "Black is leading (+${String.format("%.1f", -eval)}). Look for tactical pins and pawn levers."
                else -> "$activeColor turn. Control center squares e4/d4/e5/d5, develop knights and bishops, and prepare castling."
            }
            return@withContext status
        }

        val prompt = """
            You are a 3D Chess Grandmaster Coach analyzing an active chess match.
            Current side to move: $activeColor
            Recent move sequence: ${if (movesNotation.isEmpty()) "Game starting" else movesNotation}
            Evaluation score: ${state.evaluationScore}
            
            Provide a short, punchy 2-sentence grandmaster advice on the best strategic move and key tactical ideas for $activeColor.
        """.trimIndent()

        callGeminiApi(apiKey, prompt) ?: "$activeColor to move. Focus on piece activity, king safety, and key central squares."
    }

    suspend fun analyzePostGame(state: GameState): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val movesNotation = state.moveHistory.joinToString(" ") { it.san }
        val result = when {
            state.isCheckmate -> "${if (state.activeColor == PieceColor.WHITE) "Black" else "White"} won by Checkmate!"
            state.isStalemate -> "Draw by Stalemate."
            else -> "Game concluded."
        }

        if (apiKey.isEmpty()) {
            return@withContext "Game Summary: $result Total moves: ${state.moveHistory.size}. Excellent tactical battle! Keep practicing your opening development and king safety."
        }

        val prompt = """
            You are a World Chess Champion reviewing a completed match.
            Match Result: $result
            Moves: $movesNotation
            
            Write a 3-sentence post-game analysis highlighting key turning points, accuracy, and tactical lessons.
        """.trimIndent()

        callGeminiApi(apiKey, prompt) ?: "Game Summary: $result Total moves: ${state.moveHistory.size}. A well-contested tactical battle!"
    }

    private fun callGeminiApi(apiKey: String, textPrompt: String): String? {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val json = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", textPrompt))
                        })
                    })
                })
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return null
            val respStr = response.body?.string() ?: return null

            val rootObj = JSONObject(respStr)
            val candidates = rootObj.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            val text = parts.optJSONObject(0)?.optString("text")

            text?.trim()
        } catch (_: Exception) {
            null
        }
    }
}
