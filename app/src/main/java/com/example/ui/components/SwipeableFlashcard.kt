package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Flashcard
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class RecallLevel(
    val value: Int,
    val label: String,
    val textColor: Color,
    val bgColor: Color
)

@Composable
fun SwipeableFlashcard(
    flashcard: Flashcard,
    onForgotten: () -> Unit,
    onRemembered: () -> Unit,
    onReadAloudClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isCardFlipped: Boolean? = null,
    onFlippedChange: ((Boolean) -> Unit)? = null,
    onReviewWithLevel: (Int) -> Unit = {}
) {
    var localIsFlipped by remember { mutableStateOf(false) }
    val isFlipped = isCardFlipped ?: localIsFlipped
    val setFlipped: (Boolean) -> Unit = { value ->
        if (onFlippedChange != null) {
            onFlippedChange(value)
        } else {
            localIsFlipped = value
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Screen dimensions to compute swipe bounds
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val swipeThreshold = screenWidthPx * 0.35f

    // Animatable horizontal offset for card swipe
    val offsetX = remember { Animatable(0f) }

    // Reset card state when flashcard changes
    LaunchedEffect(flashcard.id) {
        setFlipped(false)
        offsetX.snapTo(0f)
    }

    // Determine the color representing each CEFR Level
    val levelColor = when (flashcard.cefrLevel) {
        "A1" -> Color(0xFF4CAF50) // Green
        "A2" -> Color(0xFF8BC34A) // Light Green
        "B1" -> Color(0xFFFFCA28) // Amber
        "B2" -> Color(0xFFFF9800) // Orange
        "C1" -> Color(0xFFE91E63) // Pink
        "C2" -> Color(0xFF9C27B0) // Purple
        else -> MaterialTheme.colorScheme.primary
    }

    // Card Rotation (0 to 180 degrees) for Flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "FlipCardRotation"
    )

    // Translate horizontal swipe to simple rotation and scale
    val swipePercent = (offsetX.value / screenWidthPx).coerceIn(-1f, 1f)
    val cardRotationZ = swipePercent * 15f // Rotate slightly during drag
    val cardScale = (1f - abs(swipePercent) * 0.08f).coerceAtLeast(0.9f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(450.dp)
            .padding(16.dp)
            .pointerInput(flashcard.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value > swipeThreshold) {
                                // Swiped Right -> Remembered (Level 3 - Buono)
                                offsetX.animateTo(screenWidthPx)
                                onReviewWithLevel(3)
                                onRemembered()
                            } else if (offsetX.value < -swipeThreshold) {
                                // Swiped Left -> Forgotten (Level 1 - Rivedi)
                                offsetX.animateTo(-screenWidthPx)
                                onReviewWithLevel(1)
                                onForgotten()
                            } else {
                                // Reset position
                                offsetX.animateTo(0f, tween(300))
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, tween(200))
                        }
                    }
                )
            }
            .graphicsLayer {
                translationX = offsetX.value
                rotationZ = cardRotationZ
                scaleX = cardScale
                scaleY = cardScale
            }
            .shadow(12.dp, shape = RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { setFlipped(!isFlipped) }
    ) {
        // Swipe Indicator Overlays
        if (offsetX.value != 0f) {
            val alphaValue = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 0.95f)
            if (offsetX.value > 0) {
                // Draging Right -> Green indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2E7D32).copy(alpha = alphaValue * 0.4f))
                ) {
                    Text(
                        text = "BUONO\n✓",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(32.dp)
                            .alpha(alphaValue)
                    )
                }
            } else {
                // Dragging Left -> Red indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFC62828).copy(alpha = alphaValue * 0.4f))
                ) {
                    Text(
                        text = "RIVEDI\n✗",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(32.dp)
                            .alpha(alphaValue)
                    )
                }
            }
        }

        // Main card contents with the rotation applied
        Card(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12 * density
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            // If card is rotated past 90 degrees, show the Back of the card; otherwise show Front.
            if (rotation <= 90f) {
                // FRONT SIDE OF THE CARD
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    levelColor.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                ) {
                    // Level Badge
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                "Level ${flashcard.cefrLevel}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = levelColor
                        ),
                        border = null,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                    )

                    // Embellishment (Subtle green-white-red tiny ribbon or "🇮🇹" icon watermarked nicely)
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                            .alpha(0.6f)
                    ) {
                        Text("🇮🇹", fontSize = 28.sp)
                    }

                    // Large display of Italian word right in the center
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = flashcard.italianWord,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Play Button to read the word on the front
                        IconButton(
                            onClick = { onReadAloudClick(flashcard.italianWord) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = levelColor.copy(alpha = 0.15f),
                                contentColor = levelColor
                            ),
                            modifier = Modifier
                                .size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Ascolta la pronuncia",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Tocca per scoprire la traduzione",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = levelColor,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // BACK SIDE OF THE CARD
                // Because elements would be mirrored due to the Card's 180 degrees Y-rotation,
                // we rotate the content container Y-axis by 180 degrees *again* to cancel the mirroring.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = 180f
                        }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Level Badge and Header (Back)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    "Level ${flashcard.cefrLevel}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = levelColor
                            ),
                            border = null
                        )

                        // Speaker Button to trigger TTS Speak callback
                        IconButton(
                            onClick = { onReadAloudClick(flashcard.italianWord) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Ascolta la pronuncia"
                            )
                        }
                    }

                    // Main Vocabulary Definition Group
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Word Photo integrated inside the backside
                        if (flashcard.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = flashcard.imageUrl,
                                contentDescription = "Visual representation of ${flashcard.italianWord}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 110.dp, height = 75.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = flashcard.italianWord,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = levelColor,
                                letterSpacing = 1.5.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = flashcard.englishDefinition,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp,
                            modifier = Modifier.width(40.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Context sentence & Translation
                        Text(
                            text = "Esempio:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = flashcard.sampleSentence,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        if (flashcard.sampleSentenceTranslation.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = flashcard.sampleSentenceTranslation,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                    lineHeight = 16.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    // 1-4 level self-remember indicators
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "Quanto bene ricordi la parola?",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val levels = listOf(
                                RecallLevel(1, "Rivedi", Color(0xFFC2185B), Color(0xFFFCE4EC)),
                                RecallLevel(2, "Difficile", Color(0xFFE65100), Color(0xFFFFF3E0)),
                                RecallLevel(3, "Bene", Color(0xFF0D47A1), Color(0xFFE3F2FD)),
                                RecallLevel(4, "Facile!", Color(0xFF1B5E20), Color(0xFFE8F5E9))
                            )
                            levels.forEach { level ->
                                Button(
                                    onClick = { onReviewWithLevel(level.value) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = level.bgColor,
                                        contentColor = level.textColor
                                    ),
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text(
                                        text = "${level.value}: ${level.label}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Footer tap indicator
                    Text(
                        text = "Tocca per vedere la parola",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.outline,
                            fontStyle = FontStyle.Italic
                        )
                    )
                }
            }
        }
    }
}
