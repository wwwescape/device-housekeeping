package com.wwwescape.devicehousekeeping.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import com.wwwescape.devicehousekeeping.ui.components.CenteredCollapsingTopBar
import com.wwwescape.devicehousekeeping.ui.components.title
import com.wwwescape.devicehousekeeping.ui.navigation.Destination
import com.wwwescape.devicehousekeeping.ui.screens.apps.AppCachesScreen
import com.wwwescape.devicehousekeeping.ui.screens.category.CategoryScreen
import com.wwwescape.devicehousekeeping.ui.screens.history.HistoryScreen
import com.wwwescape.devicehousekeeping.ui.screens.home.HomeScreen
import com.wwwescape.devicehousekeeping.ui.screens.settings.ExclusionsScreen
import com.wwwescape.devicehousekeeping.ui.screens.settings.LicensesScreen
import com.wwwescape.devicehousekeeping.ui.screens.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceHousekeepingApp(navController: NavHostController = rememberNavController()) {
    // Activity-scoped: Home and every category screen share one scan result and selection.
    val housekeeping: HousekeepingViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = Destination.fromRoute(currentRoute)
    val isHome = currentRoute == null || currentDestination == Destination.Home
    val isSettingsFamily = currentDestination in setOf(Destination.Settings, Destination.Licenses, Destination.Exclusions)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val titleText = when (currentDestination) {
        Destination.Category -> backStackEntry?.arguments?.getString(Destination.CATEGORY_ARG)
            ?.let { runCatching { JunkCategory.valueOf(it) }.getOrNull() }
            ?.title()
            ?: stringResource(R.string.app_name)
        else -> stringResource(currentDestination.titleRes)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenteredCollapsingTopBar(
                title = titleText,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (isHome) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo_mark),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(28.dp),
                        )
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
                actions = {
                    if (isHome) {
                        IconButton(onClick = { navController.navigate(Destination.History.route) }) {
                            Icon(imageVector = Icons.Rounded.History, contentDescription = stringResource(R.string.title_history))
                        }
                    }
                    if (!isSettingsFamily) {
                        IconButton(onClick = { navController.navigate(Destination.Settings.route) }) {
                            Icon(imageVector = Icons.Rounded.Settings, contentDescription = stringResource(R.string.title_settings))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally(tween(320)) { it / 6 } + fadeIn(tween(320)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(320)) },
            popExitTransition = { slideOutHorizontally(tween(260)) { it / 6 } + fadeOut(tween(260)) },
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    viewModel = housekeeping,
                    onOpenCategory = { navController.navigate(Destination.categoryRoute(it)) },
                    onOpenAppCaches = { navController.navigate(Destination.AppCaches.route) },
                )
            }
            composable(
                route = Destination.Category.route,
                arguments = listOf(navArgument(Destination.CATEGORY_ARG) { type = NavType.StringType }),
            ) { entry ->
                val category = entry.arguments?.getString(Destination.CATEGORY_ARG)
                    ?.let { runCatching { JunkCategory.valueOf(it) }.getOrNull() }
                if (category != null) CategoryScreen(category = category, viewModel = housekeeping)
            }
            composable(Destination.AppCaches.route) { AppCachesScreen(viewModel = housekeeping) }
            composable(Destination.History.route) { HistoryScreen(viewModel = housekeeping) }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    onNavigateToLicenses = { navController.navigate(Destination.Licenses.route) },
                    onNavigateToExclusions = { navController.navigate(Destination.Exclusions.route) },
                )
            }
            composable(Destination.Exclusions.route) { ExclusionsScreen() }
            composable(Destination.Licenses.route) { LicensesScreen() }
        }
    }
}
