package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cefrLevel: String, // e.g. "A1", "A2", "B1", "B2", "C1", "C2"
    val italianWord: String,
    val englishDefinition: String,
    val sampleSentence: String,
    val sampleSentenceTranslation: String = "",
    val imageUrl: String,
    val nextReviewDate: Long, // Unix timestamp in milliseconds
    val repetitionInterval: Int, // in days
    val easeFactor: Float // SM-2 ease factor, initial is usually 2.5f
)
