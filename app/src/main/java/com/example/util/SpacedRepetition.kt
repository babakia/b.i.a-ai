package com.example.util

import kotlin.math.roundToInt

object SpacedRepetition {

    /**
     * Data class holding the calculation results of the SM-2 algorithm.
     */
    data class ReviewResult(
        val intervalDays: Int,
        val easeFactor: Float,
        val nextReviewDate: Long // Unix timestamp in milliseconds
    )

    /**
     * Implements the classic SuperMemo-2 (SM-2) spaced repetition scheduling algorithm.
     *
     * Mathematically, the updated ease factor (EF') is calculated as:
     * EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
     *
     * where:
     * - EF is the current ease factor
     * - q is the quality of review (0 to 5)
     *
     * Clamping:
     * - The ease factor cannot drop below 1.3. If EF' < 1.3, EF' is set to 1.3.
     *
     * Interval calculation:
     * - If the review quality is incorrect/failed (q < 3), the repetition interval is reset to 1 day.
     * - If successful (q >= 3):
     *   - For the first repetition (interval <= 0), the interval is 1 day.
     *   - For the second repetition (interval == 1), the interval is 6 days.
     *   - For subsequent repetitions (interval >= 6), the interval is calculated by scaling the
     *     current interval by the updated ease factor: interval_new = round(interval_current * EF')
     *
     * @param currentInterval Current repetition interval in days
     * @param easeFactor Current ease factor (default is usually 2.5)
     * @param quality Quality of response (0-5)
     * @param currentTimestamp The current timestamp in milliseconds (defaults to System.currentTimeMillis)
     */
    fun calculateNextReview(
        currentInterval: Int,
        easeFactor: Float,
        quality: Int,
        currentTimestamp: Long = System.currentTimeMillis()
    ): ReviewResult {
        // Ensure quality score is constrained to the 0-5 range
        val q = quality.coerceIn(0, 5)

        // 1. Calculate New Ease Factor
        // Math background: 
        // - Perfect score (5) results in EF' = EF + 0.1
        // - Good score (4) results in EF' = EF + 0
        // - Active recall failure (3) results in EF' = EF - 0.14
        // - Worse scores decrease the EF even further.
        val easeFactorAdjustment = 0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)
        val newEaseFactor = (easeFactor + easeFactorAdjustment).coerceAtLeast(1.3f)

        // 2. Calculate New Interval
        val newInterval = if (q < 3) {
            // Failed recall: reset repetition interval to 1 day
            1
        } else {
            // Successful recall
            when {
                currentInterval <= 0 -> 1 // First time learning
                currentInterval == 1 -> 6 // Second review
                else -> (currentInterval * newEaseFactor).roundToInt() // Subsequent reviews
            }
        }

        // 3. Compute next review timestamp (converting interval in days to milliseconds)
        val millisInDay = 24 * 60 * 60 * 1000L
        val nextReviewDate = currentTimestamp + (newInterval * millisInDay)

        return ReviewResult(
            intervalDays = newInterval,
            easeFactor = newEaseFactor,
            nextReviewDate = nextReviewDate
        )
    }
}
