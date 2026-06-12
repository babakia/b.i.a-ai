package com.example.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Flashcard
import com.example.data.FlashcardRepository
import com.example.util.SpacedRepetition
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FlashcardRepository
    private var tts: TextToSpeech? = null

    // State indicating if TextToSpeech is initialized successfully
    private val _isTtsInitialized = MutableStateFlow(false)
    val isTtsInitialized: StateFlow<Boolean> = _isTtsInitialized.asStateFlow()

    // Currently selected filter: "All", "Due", "A1", "A2", "B1", "B2", "C1", "C2"
    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // Trigger state to force re-evaluation of Due cards
    private val _timeTrigger = MutableStateFlow(System.currentTimeMillis())

    // Complete deck of retrieved cards matching the current filter
    val flashcardsList: StateFlow<List<Flashcard>> = combine(_selectedFilter, _timeTrigger) { filter, time ->
        filter to time
    }.flatMapLatest { (filter, timestamp) ->
        when (filter) {
            "All" -> repository.allFlashcards
            "Due" -> repository.getDueFlashcards(timestamp)
            else -> repository.getFlashcardsByCefrLevel(filter)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Index of the currently active card in the filtered list
    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    // Current active flashcard
    val currentFlashcard: StateFlow<Flashcard?> = combine(flashcardsList, _currentCardIndex) { list, index ->
        if (list.isNotEmpty() && index in list.indices) {
            list[index]
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Seeding/Loading status
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Stats
    val dueCount: StateFlow<Int> = _timeTrigger.flatMapLatest { timestamp ->
        repository.getDueFlashcards(timestamp).map { it.size }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    init {
        // Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(application)
        repository = FlashcardRepository(database.flashcardDao())

        // Start seeding and initialization
        checkAndSeedDatabase()
        initTextToSpeech(application)
    }

    /**
     * Initializes the Android TTS Engine in standard Italian.
     */
    private fun initTextToSpeech(application: Application) {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.ITALIAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("FlashcardViewModel", "Italian language is not supported or missing data on this device.")
                    _isTtsInitialized.value = false
                } else {
                    _isTtsInitialized.value = true
                }
            } else {
                Log.e("FlashcardViewModel", "Initialization of TextToSpeech failed with status code: $status")
                _isTtsInitialized.value = false
            }
        }
    }

    /**
     * Checks if database is populated. Whith empty database, parses the prepopulated assets JSON.
     */
    private fun checkAndSeedDatabase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentCount = repository.getCount()
                if (currentCount == 0) {
                    // Seed from assets
                    val jsonString = loadJsonFromAssets("flashcards.json")
                    if (jsonString != null) {
                        val parsedCards = parseJsonToFlashcards(jsonString)
                        if (parsedCards.isNotEmpty()) {
                            repository.insertFlashcards(parsedCards)
                            Log.d("FlashcardViewModel", "Successfully seeded database with ${parsedCards.size} cards.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FlashcardViewModel", "Failed to check or seed database.", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadJsonFromAssets(fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("FlashcardViewModel", "Could not open or load asset $fileName", e)
            null
        }
    }

    private fun parseJsonToFlashcards(json: String): List<Flashcard> {
        return try {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val listType = Types.newParameterizedType(List::class.java, Flashcard::class.java)
            val adapter = moshi.adapter<List<Flashcard>>(listType)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e("FlashcardViewModel", "Failed to parse json.", e)
            emptyList()
        }
    }

    /**
     * Set active filter option. Resets index.
     */
    fun setFilter(filter: String) {
        viewModelScope.launch {
            _selectedFilter.value = filter
            _currentCardIndex.value = 0
            _timeTrigger.value = System.currentTimeMillis() // Update time for current Due cards
        }
    }

    /**
     * Navigation helper to manually go next or reset.
     */
    fun nextCard() {
        val size = flashcardsList.value.size
        if (size > 0) {
            _currentCardIndex.value = (_currentCardIndex.value + 1) % size
        }
    }

    fun previousCard() {
        val size = flashcardsList.value.size
        if (size > 0) {
            _currentCardIndex.value = if (_currentCardIndex.value == 0) size - 1 else _currentCardIndex.value - 1
        }
    }

    /**
     * Applies the spaced repetition algorithm to the specific flashcard based on 1 to 4 levels,
     * updates Room, and updates time trigger.
     *
     * @param flashcard The current card under review.
     * @param level Recall rating level from 1 to 4.
     */
    fun reviewCardWithLevel(flashcard: Flashcard, level: Int) {
        viewModelScope.launch {
            // Map 1-4 levels to SM-2 quality scores (1, 3, 4, 5)
            val qualityScore = when (level) {
                1 -> 1 // Rivedi
                2 -> 3 // Difficile
                3 -> 4 // Bene
                4 -> 5 // Facile!
                else -> 4
            }

            // Execute SM-2 algorithm math
            val sm2Result = SpacedRepetition.calculateNextReview(
                currentInterval = flashcard.repetitionInterval,
                easeFactor = flashcard.easeFactor,
                quality = qualityScore,
                currentTimestamp = System.currentTimeMillis()
            )

            // Build updated card record
            val updatedCard = flashcard.copy(
                repetitionInterval = sm2Result.intervalDays,
                easeFactor = sm2Result.easeFactor,
                nextReviewDate = sm2Result.nextReviewDate
            )

            // Save back into local Room persistence
            repository.insertFlashcard(updatedCard)

            // Safely transition index forward or reset
            val matchingList = flashcardsList.value
            if (matchingList.isNotEmpty()) {
                val maxIndex = matchingList.size - 1
                if (_currentCardIndex.value >= maxIndex) {
                    // We reviewed the last card of the list, wrap around to 0
                    _currentCardIndex.value = 0
                }
                // Update time trigger to refresh due counts
                _timeTrigger.value = System.currentTimeMillis()
            }
        }
    }

    /**
     * Applies the spaced repetition algorithm to the specific flashcard and updates Room,
     * then increments to the next card index in the queue.
     *
     * @param flashcard The current card under review.
     * @param remembered True if recalled successfully (SM-2 quality = 4), False if forgotten (SM-2 quality = 1).
     */
    fun reviewCard(flashcard: Flashcard, remembered: Boolean) {
        viewModelScope.launch {
            // Quality scale mapping:
            // Swiped Right (Success): Quality Score 4 (good response recalled after hesitation)
            // Swiped Left (Failure): Quality Score 1 (incorrect response, correct one remembered)
            val qualityScore = if (remembered) 4 else 1

            // Execute SM-2 algorithm math
            val sm2Result = SpacedRepetition.calculateNextReview(
                currentInterval = flashcard.repetitionInterval,
                easeFactor = flashcard.easeFactor,
                quality = qualityScore,
                currentTimestamp = System.currentTimeMillis()
            )

            // Build updated card record
            val updatedCard = flashcard.copy(
                repetitionInterval = sm2Result.intervalDays,
                easeFactor = sm2Result.easeFactor,
                nextReviewDate = sm2Result.nextReviewDate
            )

            // Save back into local Room persistence
            repository.insertFlashcard(updatedCard)

            // Safely transition index forward or reset
            val matchingList = flashcardsList.value
            if (matchingList.isNotEmpty()) {
                val maxIndex = matchingList.size - 1
                if (_currentCardIndex.value >= maxIndex) {
                    // We reviewed the last card of the list, wrap around to 0
                    _currentCardIndex.value = 0
                }
                // Update time trigger to refresh due counts
                _timeTrigger.value = System.currentTimeMillis()
            }
        }
    }

    /**
     * Reads the provided text aloud in Italian. Stops any ongoing speech.
     */
    fun speakText(text: String) {
        if (_isTtsInitialized.value && tts != null) {
            try {
                // Stop any current audio
                tts?.stop()
                // Speak the active word
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ItalianFlashcardsSpeakID")
            } catch (e: Exception) {
                Log.e("FlashcardViewModel", "Failed to play speech audio.", e)
            }
        } else {
            Log.w("FlashcardViewModel", "TTS is not initialized or unavailable.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Gracefully release Audio resources
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isTtsInitialized.value = false
    }
}
