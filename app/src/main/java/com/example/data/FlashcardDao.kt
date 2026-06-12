package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<Flashcard>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard)

    @Query("SELECT * FROM flashcards WHERE cefrLevel = :cefrLevel ORDER BY id ASC")
    fun getFlashcardsByCefrLevel(cefrLevel: String): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :currentTimestamp ORDER BY nextReviewDate ASC")
    fun getDueFlashcards(currentTimestamp: Long): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards ORDER BY id ASC")
    fun getAllFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT COUNT(*) FROM flashcards")
    suspend fun getCount(): Int
}
