package com.example.sevasetu

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sevasetu.data.repository.AuthContainer
import com.example.sevasetu.navigation.AppRoute
import com.example.sevasetu.navigation.bottomTabRoutes
import com.example.sevasetu.ui.common.AuthViewModel
import com.example.sevasetu.ui.common.AuthViewModelFactory
import com.example.sevasetu.ui.screen.alerts.AlertsScreenContent
import com.example.sevasetu.ui.screen.login.AccountCreationScreen
import com.example.sevasetu.ui.screen.profile.ProfileScreenContent
import com.example.sevasetu.ui.screen.reports.IssueReportScreen
import com.example.sevasetu.ui.screen.reports.MyReportsScreen
import com.example.sevasetu.ui.screen.SplashScreenContent
import com.example.sevasetu.ui.screen.dashboard.DashboardScreen
import com.example.sevasetu.ui.screen.login.LoginScreen
import com.example.sevasetu.ui.theme.SevaSetuTheme
import com.example.sevasetu.utils.TokenManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SevaSetuTheme {
                AppNavRoot()
            }
        }
    }
}

@Composable
private fun AppNavRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    BackHandler(
        enabled = currentRoute != null &&
            currentRoute != AppRoute.Home.route &&
            currentRoute != AppRoute.Splash.route &&
            currentRoute != AppRoute.Login.route &&
            currentRoute != AppRoute.AccountCreation.route
    ) {
        if (currentRoute in listOf(AppRoute.Reports.route, AppRoute.Alerts.route, AppRoute.Profile.route)) {
            navController.selectTab(AppRoute.Home.route)
        } else {
            navController.popBackStack()
        }
    }

    val shouldShowSharedBars = currentRoute in (bottomTabRoutes + AppRoute.IssueReport.route)

    Scaffold(
        containerColor = Color(0xFFF2F5F3),
        topBar = {
            if (shouldShowSharedBars) {
                SharedTopBar(
                    currentRoute = currentRoute.orEmpty(),
                    onBack = { navController.popBackStack() },
                    onNavigateProfile = { navController.selectTab(AppRoute.Profile.route) }
                )
            }
        },
        bottomBar = {
            if (currentRoute in bottomTabRoutes) {
                SharedBottomBar(
                    currentRoute = currentRoute.orEmpty(),
                    onSelectTab = { route -> navController.selectTab(route) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF2F5F3)),
            navController = navController,
            startDestination = AppRoute.Splash.route
        ) {
        composable(
            route = AppRoute.Splash.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(120)) },
            popExitTransition = { ExitTransition.None }
        ) {
            SplashRoute(navController)
        }
        composable(
            route = AppRoute.Login.route,
            enterTransition = { smoothEnterTransition(initialState, targetState) },
            exitTransition = { smoothExitTransition(initialState, targetState) },
            popEnterTransition = { smoothPopEnterTransition(initialState, targetState) },
            popExitTransition = { smoothPopExitTransition(initialState, targetState) }
        ) {
            LoginRoute(navController)
        }
        composable(
            route = AppRoute.AccountCreation.route,
            enterTransition = { smoothEnterTransition(initialState, targetState) },
            exitTransition = { smoothExitTransition(initialState, targetState) },
            popEnterTransition = { smoothPopEnterTransition(initialState, targetState) },
            popExitTransition = { smoothPopExitTransition(initialState, targetState) }
        ) {
            AccountCreationRoute(navController)
        }
        composable(
            route = AppRoute.Home.route,
            enterTransition = { smoothEnterTransition(initialState, targetState) },
            exitTransition = { smoothExitTransition(initialState, targetState) },
            popEnterTransition = { smoothPopEnterTransition(initialState, targetState) },
            popExitTransition = { smoothPopExitTransition(initialState, targetState) }
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DashboardScreen(
                    onNavigateReports = { navController.selectTab(AppRoute.Reports.route) },
                    onNavigateAlerts = { navController.selectTab(AppRoute.Alerts.route) },
                    onNavigateProfile = { navController.selectTab(AppRoute.Profile.route) },
                    onNavigateIssueReport = { navController.navigate(AppRoute.IssueReport.route) },
                    showAppBars = false
                )
            }
        }
        composable(
            route = AppRoute.Reports.route,
            enterTransition = { smoothEnterTransition(initialState, targetState) },
            exitTransition = { smoothExitTransition(initialState, targetState) },
            popEnterTransition = { smoothPopEnterTransition(initialState, targetState) },
            popExitTransition = { smoothPopExitTransition(initialState, targetState) }
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                MyReportsScreen(
                    onNavigateHome = { navController.selectTab(AppRoute.Home.route) },
                    onNavigateAlerts = { navController.selectTab(AppRoute.Alerts.route) },
                    onNavigateProfile = { navController.selectTab(AppRoute.Profile.route) },
                    onNavigateIssueReport = { navController.navigate(AppRoute.IssueReport.route) },
                    showAppBars = false
                )
            }
        }
        composable(
            route = AppRoute.Alerts.route,
            enterTransition = { smoothEnterTransition(initialState, targetState) },
            exitTransition = { smoothExitTransition(initialState, targetState) },
            popEnterTransition = { smoothPopEnterTransition(initialState, targetState) },
            popExitTransition = { smoothPopExitTransition(initialState, targetState) }
        ) {
            AlertsScreenContent(
                onNavigateHome = { navController.selectTab(AppRoute.Home.route) },
                onNavigateReports = { navController.selectTab(AppRoute.Reports.route) },
                onNavigateProfile = { navController.selectTab(AppRoute.Profile.route) },
                showAppBars = false
            )
        }
        composable(
            route = AppRoute.Profile.route,
            enterTransition = { smoothEnterTransition(initialState, targetState) },
            exitTransition = { smoothExitTransition(initialState, targetState) },
            popEnterTransition = { smoothPopEnterTransition(initialState, targetState) },
            popExitTransition = { smoothPopExitTransition(initialState, targetState) }
        ) {
            ProfileScreenContent(
                onNavigateHome = { navController.selectTab(AppRoute.Home.route) },
                onNavigateReports = { navController.selectTab(AppRoute.Reports.route) },
                onNavigateAlerts = { navController.selectTab(AppRoute.Alerts.route) },
                onNavigateLogin = {
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                showAppBars = false
            )
        }
        composable(
            route = AppRoute.IssueReport.route,
            enterTransition = { smoothEnterTransition(initialState, targetState) },
            exitTransition = { smoothExitTransition(initialState, targetState) },
            popEnterTransition = { smoothPopEnterTransition(initialState, targetState) },
            popExitTransition = { smoothPopExitTransition(initialState, targetState) }
        ) {
            IssueReportScreen(
                onBack = { navController.popBackStack() },
                onNavigateHome = { navController.selectTab(AppRoute.Home.route) },
                onNavigateAlerts = { navController.selectTab(AppRoute.Alerts.route) },
                onNavigateProfile = { navController.selectTab(AppRoute.Profile.route) },
                showAppBars = false
            )
        }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SharedTopBar(
    currentRoute: String,
    onBack: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                when (currentRoute) {
                    AppRoute.Reports.route -> "My Reports"
                    AppRoute.Alerts.route -> "Notifications"
                    AppRoute.Profile.route -> "Profile"
                    AppRoute.IssueReport.route -> "Report Issue"
                    else -> "SevaSetu"
                }
            )
        },
        navigationIcon = {
            if (currentRoute == AppRoute.IssueReport.route) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (currentRoute != AppRoute.Profile.route) {
                IconButton(onClick = onNavigateProfile) {
                    Icon(Icons.Default.Person, contentDescription = "Profile")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
private fun SharedBottomBar(
    currentRoute: String,
    onSelectTab: (String) -> Unit
) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = currentRoute == AppRoute.Home.route,
            onClick = { onSelectTab(AppRoute.Home.route) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("HOME") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF00875A),
                selectedTextColor = Color(0xFF00875A),
                indicatorColor = Color(0xFFE8F5E9)
            )
        )
        NavigationBarItem(
            selected = currentRoute == AppRoute.Reports.route,
            onClick = { onSelectTab(AppRoute.Reports.route) },
            icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Reports") },
            label = { Text("REPORTS") }
        )
        NavigationBarItem(
            selected = currentRoute == AppRoute.Alerts.route,
            onClick = { onSelectTab(AppRoute.Alerts.route) },
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
            label = { Text("ALERTS") }
        )
        NavigationBarItem(
            selected = currentRoute == AppRoute.Profile.route,
            onClick = { onSelectTab(AppRoute.Profile.route) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("PROFILE") }
        )
    }
}

@Composable
private fun SplashRoute(navController: NavHostController) {
    val context = LocalContext.current
    val tokenManager = remember(context) { TokenManager(context.applicationContext) }

    SplashScreenContent(
        onNavigate = {
            val next = if (tokenManager.getToken().isNullOrBlank()) {
                AppRoute.Login.route
            } else {
                AppRoute.Home.route
            }
            navController.navigate(next) {
                popUpTo(AppRoute.Splash.route) { inclusive = true }
            }
        }
    )
}

@Composable
private fun LoginRoute(navController: NavHostController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthContainer.provideAuthRepository(context))
    )

    LaunchedEffect(Unit) {
        if (authViewModel.restoreSession()) {
            navController.navigate(AppRoute.Home.route) {
                popUpTo(AppRoute.Login.route) { inclusive = true }
            }
        }
    }

    LoginScreen(
        authViewModel = authViewModel,
        onAuthSuccess = {
            navController.navigate(AppRoute.Home.route) {
                popUpTo(AppRoute.Login.route) { inclusive = true }
            }
        },
        onCreateAccountClick = {
            navController.navigate(AppRoute.AccountCreation.route)
        }
    )
}

@Composable
private fun AccountCreationRoute(navController: NavHostController) {
    AccountCreationScreen(
        onBackClick = { navController.popBackStack() },
        onLoginClick = {
            navController.navigate(AppRoute.Login.route) {
                popUpTo(AppRoute.AccountCreation.route) { inclusive = true }
            }
        },
        onRegistrationSuccess = {
            navController.popBackStack()
        }
    )
}

private val tabRoutes = setOf(
    AppRoute.Home.route,
    AppRoute.Reports.route,
    AppRoute.Alerts.route,
    AppRoute.Profile.route
)
private val tabRouteOrder = listOf(
    AppRoute.Home.route,
    AppRoute.Reports.route,
    AppRoute.Alerts.route,
    AppRoute.Profile.route
)

private const val TAB_FADE_MS = 600
private const val TAB_SLIDE_MS = 600
private const val SCREEN_ENTER_MS = 600
private const val SCREEN_EXIT_MS = 600

private fun NavBackStackEntry?.routeOrEmpty(): String = this?.destination?.route.orEmpty()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.smoothEnterTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
) = if (initialState.routeOrEmpty() in tabRoutes && targetState.routeOrEmpty() in tabRoutes) {
    val movingForward = tabRouteOrder.indexOf(targetState.routeOrEmpty()) >=
        tabRouteOrder.indexOf(initialState.routeOrEmpty())
    slideInHorizontally(
        animationSpec = tween(durationMillis = TAB_SLIDE_MS, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth ->
            val offset = (fullWidth * 0.08f).toInt()
            if (movingForward) offset else -offset
        }
    ) + fadeIn(animationSpec = tween(durationMillis = TAB_FADE_MS, easing = LinearOutSlowInEasing))
} else {
    slideInHorizontally(
        animationSpec = tween(durationMillis = SCREEN_ENTER_MS, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> (fullWidth * 0.16f).toInt() }
    ) + fadeIn(animationSpec = tween(durationMillis = SCREEN_ENTER_MS, easing = LinearOutSlowInEasing))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.smoothExitTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
) = if (initialState.routeOrEmpty() in tabRoutes && targetState.routeOrEmpty() in tabRoutes) {
    val movingForward = tabRouteOrder.indexOf(targetState.routeOrEmpty()) >=
        tabRouteOrder.indexOf(initialState.routeOrEmpty())
    slideOutHorizontally(
        animationSpec = tween(durationMillis = TAB_SLIDE_MS, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth ->
            val offset = (fullWidth * 0.08f).toInt()
            if (movingForward) -offset else offset
        }
    ) + fadeOut(animationSpec = tween(durationMillis = TAB_FADE_MS, easing = FastOutSlowInEasing))
} else {
    slideOutHorizontally(
        animationSpec = tween(durationMillis = SCREEN_EXIT_MS, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> -(fullWidth * 0.12f).toInt() }
    ) + fadeOut(animationSpec = tween(durationMillis = SCREEN_EXIT_MS, easing = FastOutSlowInEasing))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.smoothPopEnterTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
) = if (initialState.routeOrEmpty() in tabRoutes && targetState.routeOrEmpty() in tabRoutes) {
    val movingBack = tabRouteOrder.indexOf(targetState.routeOrEmpty()) <=
        tabRouteOrder.indexOf(initialState.routeOrEmpty())
    slideInHorizontally(
        animationSpec = tween(durationMillis = TAB_SLIDE_MS, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth ->
            val offset = (fullWidth * 0.08f).toInt()
            if (movingBack) -offset else offset
        }
    ) + fadeIn(animationSpec = tween(durationMillis = TAB_FADE_MS, easing = LinearOutSlowInEasing))
} else {
    slideInHorizontally(
        animationSpec = tween(durationMillis = SCREEN_ENTER_MS, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> -(fullWidth * 0.16f).toInt() }
    ) + fadeIn(animationSpec = tween(durationMillis = SCREEN_ENTER_MS, easing = LinearOutSlowInEasing))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.smoothPopExitTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
) = if (initialState.routeOrEmpty() in tabRoutes && targetState.routeOrEmpty() in tabRoutes) {
    val movingBack = tabRouteOrder.indexOf(targetState.routeOrEmpty()) <=
        tabRouteOrder.indexOf(initialState.routeOrEmpty())
    slideOutHorizontally(
        animationSpec = tween(durationMillis = TAB_SLIDE_MS, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth ->
            val offset = (fullWidth * 0.08f).toInt()
            if (movingBack) offset else -offset
        }
    ) + fadeOut(animationSpec = tween(durationMillis = TAB_FADE_MS, easing = FastOutSlowInEasing))
} else {
    slideOutHorizontally(
        animationSpec = tween(durationMillis = SCREEN_EXIT_MS, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> (fullWidth * 0.12f).toInt() }
    ) + fadeOut(animationSpec = tween(durationMillis = SCREEN_EXIT_MS, easing = FastOutSlowInEasing))
}

private fun NavHostController.selectTab(route: String) {
    if (currentDestination?.route == route) return

    val reused = popBackStack(route, inclusive = false)
    if (!reused) {
        navigate(route) {
            launchSingleTop = true
        }
    }
}
