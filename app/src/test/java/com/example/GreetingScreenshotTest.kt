package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.Flashcard
import com.example.ui.components.SwipeableFlashcard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockCard = Flashcard(
        id = 1,
        cefrLevel = "A1",
        italianWord = "ciao",
        englishDefinition = "hello or goodbye",
        sampleSentence = "Ciao, come stai oggi?",
        imageUrl = "",
        nextReviewDate = 0,
        repetitionInterval = 0,
        easeFactor = 2.5f
    )
    composeTestRule.setContent {
      MyApplicationTheme {
        SwipeableFlashcard(
            flashcard = mockCard,
            onForgotten = {},
            onRemembered = {},
            onReadAloudClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
