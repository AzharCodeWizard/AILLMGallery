package com.azhar.aillmgallery.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.azhar.aillmgallery.ui.chat.ChatScreen
import com.azhar.aillmgallery.ui.download.ModelDownloadScreen
import com.azhar.aillmgallery.ui.gallery.GalleryScreen
import com.azhar.aillmgallery.ui.settings.SettingsScreen
import com.azhar.aillmgallery.ui.quiz.QuizListScreen
import com.azhar.aillmgallery.ui.quiz.QuizScreen
import com.azhar.aillmgallery.ui.story.StoryScreen

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
