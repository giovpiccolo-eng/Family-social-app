package com.familynest.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.familynest.app.data.model.AppUser
import com.familynest.app.ui.create.CreatePostScreen
import com.familynest.app.ui.detail.PostDetailScreen
import com.familynest.app.ui.family.FamilyScreen
import com.familynest.app.ui.projects.ProjectsScreen
import com.familynest.app.ui.reader.ReaderLibraryScreen
import com.familynest.app.ui.reader.SpeedReaderScreen
import com.familynest.app.ui.reader.SpeedReaderViewModel

private sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Feed : Tab("feed", "Feed", Icons.Rounded.Home)
    data object Projects : Tab("projects", "Plans", Icons.Rounded.Checklist)
    data object Family : Tab("family", "Family", Icons.Rounded.People)
    data object Reader : Tab("reader", "Read", Icons.Rounded.AutoStories)
}

@Composable
fun MainScaffold(user: AppUser) {
    val navController = rememberNavController()
    val tabs = listOf(Tab.Feed, Tab.Projects, Tab.Family, Tab.Reader)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val onTab = currentRoute?.hierarchy?.any { dest ->
        tabs.any { it.route == dest.route }
    } == true

    // FAB is hidden on the reader tab and its nested screens
    val onReaderGraph = currentRoute?.hierarchy?.any { it.route == Tab.Reader.route } == true

    Scaffold(
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (onTab && !onReaderGraph) {
                FloatingActionButton(onClick = { navController.navigate("create") }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Share something")
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Feed.route,
            modifier = androidx.compose.ui.Modifier.padding(padding),
        ) {
            composable(Tab.Feed.route) {
                FeedScreen(
                    user = user,
                    onOpenPost = { navController.navigate("detail/$it") },
                )
            }
            composable(Tab.Projects.route) {
                ProjectsScreen(
                    user = user,
                    onOpenPost = { navController.navigate("detail/$it") },
                )
            }
            composable(Tab.Family.route) {
                FamilyScreen(user = user)
            }

            // Reader nested graph — ViewModel is shared across library + play screens
            navigation(startDestination = "reader_home", route = Tab.Reader.route) {
                composable("reader_home") { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry(Tab.Reader.route)
                    }
                    val vm: SpeedReaderViewModel = viewModel(parentEntry)
                    ReaderLibraryScreen(
                        viewModel = vm,
                        onStartReading = { navController.navigate("reader_play") },
                    )
                }
                composable("reader_play") { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry(Tab.Reader.route)
                    }
                    val vm: SpeedReaderViewModel = viewModel(parentEntry)
                    SpeedReaderScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            composable("create") {
                CreatePostScreen(
                    user = user,
                    onDone = { navController.popBackStack() },
                )
            }
            composable("detail/{postId}") { entry ->
                val postId = entry.arguments?.getString("postId").orEmpty()
                PostDetailScreen(
                    user = user,
                    postId = postId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
