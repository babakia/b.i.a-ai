package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Flashcard
import com.example.ui.components.SwipeableFlashcard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FlashcardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    ItalianFlashcardsApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItalianFlashcardsApp(
    modifier: Modifier = Modifier,
    viewModel: FlashcardViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val flashcards by viewModel.flashcardsList.collectAsStateWithLifecycle()
    val currentCardIndex by viewModel.currentCardIndex.collectAsStateWithLifecycle()
    val activeCard by viewModel.currentFlashcard.collectAsStateWithLifecycle()
    val activeFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val dueCount by viewModel.dueCount.collectAsStateWithLifecycle()
    val isTtsReady by viewModel.isTtsInitialized.collectAsStateWithLifecycle()

    var isCardFlipped by remember { mutableStateOf(false) }
    LaunchedEffect(activeCard?.id) {
        isCardFlipped = false
    }

    val filters = listOf("All", "Due", "A1", "A2", "B1", "B2", "C1", "C2")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Premium Italian Custom Brand Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand name with custom colors
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small Italian Flag icon badge
                    Row(
                        modifier = Modifier
                            .size(24.dp, 16.dp)
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF009246))) // Green
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))      // White
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFCE2B37))) // Red
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "RIPETI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                // TTS audio indicator
                if (isTtsReady) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Speech Active", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "TTS Vocalizer Ready",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.secondary,
                            leadingIconContentColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = null
                    )
                }
            }

            Text(
                text = "Learn vocabulary using smart spaced repetition scheduling",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // 2. Horizontal CEFR Filter Selection Group
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                val isSelected = filter == activeFilter
                val countLabel = if (filter == "Due") " ($dueCount)" else ""

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setFilter(filter) },
                    label = {
                        Text(
                            text = "$filter$countLabel",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )
            }
        }

        // 3. Central Deck Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                // Loading Spinner
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (flashcards.isEmpty()) {
                // Empty Deck View
                androidx.compose.animation.AnimatedVisibility(
                    visible = flashcards.isEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (activeFilter == "Due") Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = "Active collection finished",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = if (activeFilter == "Due") "Sessione completata!" else "Mazzo vuoto",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (activeFilter == "Due") {
                                "Hai ripassato tutti i vocaboli scaduti! Ottimo lavoro, il tuo cervello ringrazia."
                            } else {
                                "Nessun vocabolo presente con questi filtri."
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.outline
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.setFilter("All") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset filter"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mostra tutte le parole")
                        }
                    }
                }
            } else {
                // Active Interactive Cards Deck
                activeCard?.let { card ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Linear Queue Progress Tracker
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Deck: $activeFilter",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )

                                Text(
                                    text = "${currentCardIndex + 1} di ${flashcards.size}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val progressFraction = (currentCardIndex + 1).toFloat() / flashcards.size.toFloat()
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        // The Active Flashcard
                        SwipeableFlashcard(
                            flashcard = card,
                            isCardFlipped = isCardFlipped,
                            onFlippedChange = { isCardFlipped = it },
                            onForgotten = { viewModel.reviewCardWithLevel(card, 1) },
                            onRemembered = { viewModel.reviewCardWithLevel(card, 3) },
                            onReadAloudClick = { text -> viewModel.speakText(text) },
                            onReviewWithLevel = { level -> viewModel.reviewCardWithLevel(card, level) },
                            modifier = Modifier.weight(1f)
                        )

                        // 4. Dynamic Manual Review Buttons (Accessibility and Desk helper panel)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Card Button
                            IconButton(
                                onClick = { viewModel.previousCard() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Parola precedente",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Dynamic Center Section
                            if (!isCardFlipped) {
                                // Simple central "Gira Carta" action button when unflipped
                                Button(
                                    onClick = { isCardFlipped = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Gira la carta"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Gira Carta",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            } else {
                                // 4 memory levels segment row when flipped
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val levelsConfig = listOf(
                                        Triple(1, "Rivedi", Color(0xFFC2185B) to Color(0xFFFCE4EC)),
                                        Triple(2, "Difficile", Color(0xFFE65100) to Color(0xFFFFF3E0)),
                                        Triple(3, "Bene", Color(0xFF0D47A1) to Color(0xFFE3F2FD)),
                                        Triple(4, "Facile!", Color(0xFF1B5E20) to Color(0xFFE8F5E9))
                                    )
                                    levelsConfig.forEach { (level, label, colorPair) ->
                                        Button(
                                            onClick = { viewModel.reviewCardWithLevel(card, level) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colorPair.second,
                                                contentColor = colorPair.first
                                            ),
                                            contentPadding = PaddingValues(horizontal = 1.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = level.toString(),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = label,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Next Card Button
                            IconButton(
                                onClick = { viewModel.nextCard() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Prossima parola",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Help tip indicator
                        Text(
                            text = "💡 Trascina la carta (< Rivedi / Bene >) o tocca i pulsanti per studiare!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
