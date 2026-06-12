package com.example.data

import kotlinx.coroutines.flow.Flow

class FlashcardRepository(private val flashcardDao: FlashcardDao) {
    val allFlashcards: Flow<List<Flashcard>> = flashcardDao.getAllFlashcards()

    fun getFlashcardsByCefrLevel(cefrLevel: String): Flow<List<Flashcard>> {
        return flashcardDao.getFlashcardsByCefrLevel(cefrLevel)
    }

    fun getDueFlashcards(currentTimestamp: Long): Flow<List<Flashcard>> {
        return flashcardDao.getDueFlashcards(currentTimestamp)
    }

    suspend fun insertFlashcards(flashcards: List<Flashcard>) {
        flashcardDao.insertFlashcards(flashcards)
    }

    suspend fun insertFlashcard(flashcard: Flashcard) {
        flashcardDao.insertFlashcard(flashcard)
    }

    suspend fun getCount(): Int {
        return flashcardDao.getCount()
    }
}
