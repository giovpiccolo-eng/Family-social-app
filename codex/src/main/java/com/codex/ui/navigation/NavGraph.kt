package com.codex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.codex.data.repository.CodexRepository
import com.codex.ui.achievement.AchievementsScreen
import com.codex.ui.codexmap.CodexMapScreen
import com.codex.ui.dashboard.DashboardScreen
import com.codex.ui.home.HomeScreen
import com.codex.ui.mission.MissionScreen

sealed class Schermo(val route: String) {
    object Home : Schermo("home")
    object Missione : Schermo("missione/{giorno}") {
        fun con(giorno: Int) = "missione/$giorno"
    }
    object Mappa : Schermo("mappa")
    object Achievements : Schermo("achievements")
    object Dashboard : Schermo("dashboard")
}

@Composable
fun CodexNavGraph(navController: NavHostController, repo: CodexRepository) {

    NavHost(navController = navController, startDestination = Schermo.Home.route) {

        composable(Schermo.Home.route) {
            HomeScreen(
                repo = repo,
                onIniziaMissione = { giorno ->
                    navController.navigate(Schermo.Missione.con(giorno))
                },
                onApriMappa = { navController.navigate(Schermo.Mappa.route) },
                onApriAchievements = { navController.navigate(Schermo.Achievements.route) },
                onApriDashboard = { navController.navigate(Schermo.Dashboard.route) }
            )
        }

        composable(
            Schermo.Missione.route,
            arguments = listOf(navArgument("giorno") { type = NavType.IntType })
        ) { back ->
            val giorno = back.arguments?.getInt("giorno") ?: 1
            MissionScreen(
                giorno = giorno,
                repo = repo,
                onMissioneCompletata = { navController.popBackStack() },
                onTornaHome = { navController.popBackStack() }
            )
        }

        composable(Schermo.Mappa.route) {
            CodexMapScreen(
                repo = repo,
                onIndietro = { navController.popBackStack() }
            )
        }

        composable(Schermo.Achievements.route) {
            AchievementsScreen(
                repo = repo,
                onIndietro = { navController.popBackStack() }
            )
        }

        composable(Schermo.Dashboard.route) {
            DashboardScreen(
                repo = repo,
                onIndietro = { navController.popBackStack() }
            )
        }
    }
}
