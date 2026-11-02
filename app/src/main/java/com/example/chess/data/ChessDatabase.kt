package com.example.chess.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "game_records")
data class GameRecordEntity(
    @PrimaryKey val id: String,
    val mode: String,
    val result: String,
    val winnerColor: String?,
    val pgn: String,
    val moveCount: Int,
    val durationSeconds: Long,
    val timestamp: Long
)

@Entity(tableName = "puzzle_progress")
data class PuzzleProgressEntity(
    @PrimaryKey val puzzleId: String,
    val isSolved: Boolean,
    val attempts: Int,
    val timestamp: Long
)

@Dao
interface ChessDao {
    @Query("SELECT * FROM game_records ORDER BY timestamp DESC")
    suspend fun getAllGameRecords(): List<GameRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameRecord(record: GameRecordEntity)

    @Query("SELECT * FROM puzzle_progress WHERE puzzleId = :puzzleId")
    suspend fun getPuzzleProgress(puzzleId: String): PuzzleProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPuzzleProgress(progress: PuzzleProgressEntity)
}

@Database(entities = [GameRecordEntity::class, PuzzleProgressEntity::class], version = 1, exportSchema = false)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun chessDao(): ChessDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getDatabase(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "chess_3d_realm.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
