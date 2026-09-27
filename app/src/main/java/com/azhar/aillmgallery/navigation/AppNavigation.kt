package com.azhar.aillmgallery.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.azhar.aillmgallery.ui.chat.ChatScreen
import com.azhar.aillmgallery.ui.download.ModelDownloadScreen
import com.azhar.aillmgallery.ui.gallery.GalleryScreen
import com.azhar.aillmgallery.ui.settings.SettingsScreen
import com.azhar.aillmgallery.ui.quiz.QuizListScreen
import com.azhar.aillmgallery.ui.quiz.QuizScreen
import com.azhar.aillmgallery.ui.story.StoryScreen

private const val DURATION = 320

@Composable
fun AppNavigation() {
    val backStack = remember { mutableStateListOf<Any>(GalleryKey) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            // Give every screen its own ViewModelStore so, e.g., each chat keeps a separate
            // conversation and its ViewModel is cleared when the screen is popped.
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = {
                (slideInHorizontally(tween(DURATION)) { it / 4 } + fadeIn(tween(DURATION))) togetherWith
                    (slideOutHorizontally(tween(DURATION)) { -it / 8 } + fadeOut(tween(DURATION / 2)))
            },
            popTransitionSpec = {
                (slideInHorizontally(tween(DURATION)) { -it / 8 } + fadeIn(tween(DURATION))) togetherWith
                    (slideOutHorizontally(tween(DURATION)) { it / 4 } + fadeOut(tween(DURATION / 2)))
            },
            predictivePopTransitionSpec = {
                (slideInHorizontally(tween(DURATION)) { -it / 8 } + fadeIn(tween(DURATION))) togetherWith
                    (slideOutHorizontally(tween(DURATION)) { it / 4 } + fadeOut(tween(DURATION / 2)))
            },
            entryProvider = entryProvider {
                entry<GalleryKey> {
                    GalleryScreen(
                        onCapabilitySelected = { id, title ->
                            backStack.add(ChatKey(capabilityId = id, capabilityTitle = title))
                        },
                        onSettingsClick = {
                            backStack.add(SettingsKey)
                        },
                        onDownloadClick = {
                            backStack.add(ModelDownloadKey)
                        },
                        onQuizClick = {
                            backStack.add(QuizListKey)
                        },
                        onStoryClick = {
                            backStack.add(StoryKey)
                        }
                    )
                }

                entry<ChatKey> { key ->
                    ChatScreen(
                        capabilityId = key.capabilityId,
                        capabilityTitle = key.capabilityTitle,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<SettingsKey> {
                    SettingsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onModelHubClick = { backStack.add(ModelDownloadKey) }
                    )
                }

                entry<ModelDownloadKey> {
                    ModelDownloadScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                
                entry<QuizListKey> {
                    QuizListScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onQuizPlay = { quizId -> backStack.add(QuizPlayKey(quizId)) }
                    )
                }
                
                entry<QuizPlayKey> { key ->
                    QuizScreen(
                        quizId = key.quizId,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                
                entry<StoryKey> {
                    StoryScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
            }
        )
    }
}
