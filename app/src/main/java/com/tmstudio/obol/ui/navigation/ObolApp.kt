package com.tmstudio.obol.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.tmstudio.obol.notifications.ReminderNotifier
import com.tmstudio.obol.ui.add.AddScreen
import com.tmstudio.obol.ui.calendar.CalendarScreen
import com.tmstudio.obol.ui.details.DetailsScreen
import com.tmstudio.obol.ui.overview.OverviewScreen
import com.tmstudio.obol.ui.plan.PlanScreen
import com.tmstudio.obol.ui.settings.SettingsScreen
import com.tmstudio.obol.ui.theme.ObolTheme

@Composable
fun ObolApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

    // Dozvola za obavijesti traži se tek kad korisnik doda prvu pretplatu, ne pri
    // prvom pokretanju (spec, poglavlje 5). Ispod Androida 13 dozvola ne postoji.
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navigateToTab: (TopLevelDestination) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = ObolTheme.colors.bg,
        contentColor = ObolTheme.colors.textPrimary,
        bottomBar = {
            // Ekrani Dodaj, Postavi plan i Detalji nemaju donju traku.
            if (currentTab != null) {
                ObolBottomBar(selected = currentTab, onSelect = navigateToTab)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = OverviewRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<OverviewRoute> {
                OverviewScreen(
                    onAddSubscription = { navController.navigate(AddRoute) },
                    onOpenSubscription = { id -> navController.navigate(DetailsRoute(id)) },
                    onOpenCalendar = { navigateToTab(TopLevelDestination.Calendar) },
                )
            }
            composable<AddRoute> {
                AddScreen(
                    onBack = { navController.popBackStack() },
                    onServiceSelected = { serviceId -> navController.navigate(PlanRoute(serviceId)) },
                    onAddManually = { navController.navigate(PlanRoute(serviceId = null)) },
                )
            }
            composable<PlanRoute> { entry ->
                val isEditing = entry.toRoute<PlanRoute>().subscriptionId != null
                PlanScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { first ->
                        if (first) requestNotificationPermission()
                        if (isEditing) {
                            navController.popBackStack()
                        } else {
                            // Tok dodavanja završava na Pregledu, bez Dodaj i Postavi plan na stogu.
                            navController.popBackStack(OverviewRoute, inclusive = false)
                        }
                    },
                )
            }
            composable<DetailsRoute>(
                // Dodir na obavijest otvara Detalje (vidi ReminderNotifier.DEEP_LINK_BASE).
                deepLinks = listOf(navDeepLink<DetailsRoute>(basePath = ReminderNotifier.DEEP_LINK_BASE)),
            ) {
                DetailsScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(PlanRoute(subscriptionId = id)) },
                )
            }
            composable<CalendarRoute> {
                CalendarScreen(onOpenSubscription = { id -> navController.navigate(DetailsRoute(id)) })
            }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

