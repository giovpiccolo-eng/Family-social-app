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
import com.dispensa.app.ui.settings.SettingsScreen
import com.dispensa.app.ui.topic.TopicDetailScreen
import com.dispensa.app.ui.topics.TopicsListScreen

object Routes {
    const val TOPICS = "topics"
    const val SETTINGS = "settings"
    const val TOPIC_DETAIL = "topic/{topicId}"
    const val DISPENSA_VIEWER = "dispensa/{topicId}/{dispensaId}"

    fun topicDetail(topicId: String) = "topic/$topicId"
    fun dispensaViewer(topicId: String, dispensaId: String) = "dispensa/$topicId/$dispensaId"
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
                    )
                }
                composable(
                    Routes.DISPENSA_VIEWER,
                    arguments = listOf(
                        navArgument("topicId") { type = NavType.StringType },
                        navArgument("dispensaId") { type = NavType.StringType },
                    ),
                ) { entry ->
                    DispensaViewerScreen(
                        topicId = entry.arguments?.getString("topicId").orEmpty(),
                        dispensaId = entry.arguments?.getString("dispensaId").orEmpty(),
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
