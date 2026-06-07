package com.dispensa.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dispensa.app.ui.dispensa.DispensaViewerScreen
import com.dispensa.app.ui.pomodoro.PomodoroScreen
import com.dispensa.app.ui.quiz.QuizScreen
import com.dispensa.app.ui.settings.SettingsScreen
import com.dispensa.app.ui.topic.TopicDetailScreen
import com.dispensa.app.ui.topics.TopicsListScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val TOPICS = "topics"
    const val SETTINGS = "settings"
    const val TOPIC_DETAIL = "topic/{topicId}"
    const val DISPENSA_VIEWER = "dispensa/{topicId}/{dispensaId}"
    const val QUIZ = "quiz/{topicId}/{dispensaId}"
    const val POMODORO = "pomodoro/{topicTitle}"

    fun topicDetail(topicId: String) = "topic/$topicId"
    fun dispensaViewer(topicId: String, dispensaId: String) = "dispensa/$topicId/$dispensaId"
    fun quiz(topicId: String, dispensaId: String) = "quiz/$topicId/$dispensaId"
    fun pomodoro(topicTitle: String) =
        "pomodoro/${URLEncoder.encode(topicTitle.ifBlank { "_" }, "UTF-8")}"
}

@Composable
fun DispensaApp() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = Routes.TOPICS) {
                composable(Routes.TOPICS) {
                    TopicsListScreen(
                        onOpenTopic = { id -> navController.navigate(Routes.topicDetail(id)) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenPomodoro = { navController.navigate(Routes.pomodoro("")) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    Routes.TOPIC_DETAIL,
                    arguments = listOf(navArgument("topicId") { type = NavType.StringType }),
                ) { entry ->
                    val topicId = entry.arguments?.getString("topicId").orEmpty()
                    TopicDetailScreen(
                        topicId = topicId,
                        onBack = { navController.popBackStack() },
                        onOpenDispensa = { dId ->
                            navController.navigate(Routes.dispensaViewer(topicId, dId))
                        },
                        onOpenPomodoro = { title ->
                            navController.navigate(Routes.pomodoro(title))
                        },
                    )
                }
                composable(
                    Routes.DISPENSA_VIEWER,
                    arguments = listOf(
                        navArgument("topicId") { type = NavType.StringType },
                        navArgument("dispensaId") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val topicId = entry.arguments?.getString("topicId").orEmpty()
                    val dispensaId = entry.arguments?.getString("dispensaId").orEmpty()
                    DispensaViewerScreen(
                        topicId = topicId,
                        dispensaId = dispensaId,
                        onBack = { navController.popBackStack() },
                        onOpenQuiz = { navController.navigate(Routes.quiz(topicId, dispensaId)) },
                    )
                }
                composable(
                    Routes.QUIZ,
                    arguments = listOf(
                        navArgument("topicId") { type = NavType.StringType },
                        navArgument("dispensaId") { type = NavType.StringType },
                    ),
                ) { entry ->
                    QuizScreen(
                        topicId = entry.arguments?.getString("topicId").orEmpty(),
                        dispensaId = entry.arguments?.getString("dispensaId").orEmpty(),
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    Routes.POMODORO,
                    arguments = listOf(navArgument("topicTitle") { type = NavType.StringType }),
                ) { entry ->
                    val raw = entry.arguments?.getString("topicTitle").orEmpty()
                    val title = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault("")
                    PomodoroScreen(
                        topicTitle = if (title == "_") "" else title,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
