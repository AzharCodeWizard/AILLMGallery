package com.azhar.aillmgallery.ui.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Same gradient palette as QuizListScreen
private val QuizGradientStart = Color(0xFF6366F1)
private val QuizGradientEnd = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizId: Long,
    onBack: () -> Unit,
    viewModel: PlayQuizViewModel = viewModel()
) {
    LaunchedEffect(quizId) {
        viewModel.loadQuiz(quizId)
    }

    val quizState by viewModel.quizState.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val score by viewModel.score.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()
    val selectedIndex by viewModel.selectedAnswerIndex.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = quizState?.quiz?.title ?: "Loading...",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (quizState == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading quiz...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@Column
            }

            val totalQuestions = quizState?.questions?.size ?: 0

            if (isFinished) {
                // ───── Premium Results Screen ─────
                ResultsView(score = score, totalQuestions = totalQuestions, onBack = onBack)
            } else {
                // ───── Active Question Screen ─────
                val questions = quizState?.questions ?: emptyList()
                if (currentIndex >= questions.size) return@Column

                val currentQuestion = questions[currentIndex]
                val progress = (currentIndex + 1).toFloat() / totalQuestions.toFloat()

                QuestionView(
                    questionText = currentQuestion.questionText,
                    options = currentQuestion.options,
                    correctAnswerIndex = currentQuestion.correctAnswerIndex,
                    selectedIndex = selectedIndex,
                    currentIndex = currentIndex,
                    totalQuestions = totalQuestions,
                    progress = progress,
                    onOptionClick = { viewModel.submitAnswer(it) },
                    onNext = { viewModel.nextQuestion() }
                )
            }
        }
    }
}

@Composable
private fun QuestionView(
    questionText: String,
    options: List<String>,
    correctAnswerIndex: Int,
    selectedIndex: Int?,
    currentIndex: Int,
    totalQuestions: Int,
    progress: Float,
    onOptionClick: (Int) -> Unit,
    onNext: () -> Unit
) {
    val hasAnswered = selectedIndex != null

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── Progress Bar ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Question ${currentIndex + 1} of $totalQuestions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = QuizGradientStart
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
            label = "progressAnim"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(listOf(QuizGradientStart, QuizGradientEnd))
                    )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Question Card with gradient accent ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                QuizGradientStart.copy(alpha = 0.08f),
                                QuizGradientEnd.copy(alpha = 0.04f)
                            )
                        )
                    )
            ) {
                Text(
                    text = questionText,
                    modifier = Modifier.padding(28.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Option Cards ──
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            options.forEachIndexed { index, option ->
                OptionCard(
                    label = ('A' + index).toString(),
                    text = option,
                    isSelected = selectedIndex == index,
                    isCorrect = index == correctAnswerIndex,
                    hasAnswered = hasAnswered,
                    onClick = { onOptionClick(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (hasAnswered) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuizGradientStart
                )
            ) {
                Text(
                    text = if (currentIndex + 1 == totalQuestions) "See Results" else "Next Question",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OptionCard(
    label: String,
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    hasAnswered: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            hasAnswered && isCorrect -> Color(0xFF22C55E).copy(alpha = 0.12f)
            hasAnswered && isSelected && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.12f)
            isSelected -> QuizGradientStart.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "optionBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            hasAnswered && isCorrect -> Color(0xFF22C55E)
            hasAnswered && isSelected && !isCorrect -> Color(0xFFEF4444)
            isSelected -> QuizGradientStart
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        label = "optionBorder"
    )

    val badgeColor by animateColorAsState(
        targetValue = when {
            hasAnswered && isCorrect -> Color(0xFF22C55E)
            hasAnswered && isSelected && !isCorrect -> Color(0xFFEF4444)
            isSelected -> QuizGradientStart
            else -> QuizGradientStart.copy(alpha = 0.15f)
        },
        label = "badgeColor"
    )

    val badgeTextColor by animateColorAsState(
        targetValue = when {
            hasAnswered && isCorrect -> Color.White
            hasAnswered && isSelected && !isCorrect -> Color.White
            isSelected -> Color.White
            else -> QuizGradientStart
        },
        label = "badgeTextColor"
    )

    val icon = when {
        hasAnswered && isCorrect -> Icons.Filled.CheckCircle
        hasAnswered && isSelected && !isCorrect -> Icons.Filled.Cancel
        else -> null
    }

    val iconTint = when {
        hasAnswered && isCorrect -> Color(0xFF22C55E)
        hasAnswered && isSelected && !isCorrect -> Color(0xFFEF4444)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected || (hasAnswered && isCorrect)) 4.dp else 1.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = borderColor.copy(alpha = 0.3f),
                spotColor = borderColor.copy(alpha = 0.3f)
            )
            .clickable(enabled = !hasAnswered) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = if (isSelected || (hasAnswered && isCorrect)) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Option label badge — always colorful
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected || (hasAnswered && isCorrect)) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultsView(
    score: Int,
    totalQuestions: Int,
    onBack: () -> Unit
) {
    val percentage = if (totalQuestions > 0) (score * 100) / totalQuestions else 0
    val resultColor = when {
        percentage >= 80 -> Color(0xFF22C55E)
        percentage >= 50 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    val resultMessage = when {
        percentage >= 80 -> "Excellent! You're a master! 🏆"
        percentage >= 50 -> "Good effort! Keep learning! 📚"
        else -> "Don't give up! Try again! 💪"
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Trophy icon
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        resultColor.copy(alpha = 0.2f),
                        resultColor.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = resultColor,
            modifier = Modifier.size(56.dp)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Quiz Complete!",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = resultMessage,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Score Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            QuizGradientStart.copy(alpha = 0.15f),
                            QuizGradientEnd.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Score",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$score / $totalQuestions",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = resultColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(48.dp))

    Button(
        onClick = onBack,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = QuizGradientStart)
    ) {
        Text("Return to Quizzes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(32.dp))
}
