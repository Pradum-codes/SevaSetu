package com.example.sevasetu

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sevasetu.data.repository.AuthContainer
import com.example.sevasetu.navigation.AppRoute
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

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash.route
    ) {
        composable(AppRoute.Splash.route) {
            SplashRoute(navController)
        }
        composable(AppRoute.Login.route) {
            LoginRoute(navController)
        }
        composable(AppRoute.AccountCreation.route) {
            AccountCreationRoute(navController)
        }
        composable(AppRoute.Home.route) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DashboardScreen(
                    onNavigateReports = { navController.selectTab(AppRoute.Reports.route) },
                    onNavigateAlerts = { navController.selectTab(AppRoute.Alerts.route) },
                    onNavigateProfile = { navController.selectTab(AppRoute.Profile.route) },
                    onNavigateIssueReport = { navController.navigate(AppRoute.IssueReport.route) }
                )
            }
        }
        composable(AppRoute.Reports.route) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                MyReportsScreen(
                    onNavigateHome = { navController.selectTab(AppRoute.Home.route) },
                    onNavigateAlerts = { navController.selectTab(AppRoute.Alerts.route) },
                    onNavigateProfile = { navController.selectTab(AppRoute.Profile.route) },
                    onNavigateIssueReport = { navController.navigate(AppRoute.IssueReport.route) }
                )
            }
        }
        composable(AppRoute.Alerts.route) {
            AlertsScreenContent(
                onNavigateHome = { navController.selectTab(AppRoute.Home.route) },
                onNavigateReports = { navController.selectTab(AppRoute.Reports.route) },
                onNavigateProfile = { navController.selectTab(AppRoute.Profile.route) }
            )
        }
        composable(AppRoute.Profile.route) {
            ProfileScreenContent(
                onNavigateHome = { navController.selectTab(AppRoute.Home.route) },
                onNavigateReports = { navController.selectTab(AppRoute.Reports.route) },
                onNavigateAlerts = { navController.selectTab(AppRoute.Alerts.route) },
                onNavigateLogin = {
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoute.IssueReport.route) {
            IssueReportScreen(
                onBack = { navController.popBackStack() },
                onNavigateHome = { navController.selectTab(AppRoute.Home.route) },
                onNavigateAlerts = { navController.selectTab(AppRoute.Alerts.route) },
                onNavigateProfile = { navController.selectTab(AppRoute.Profile.route) }
            )
        }
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

private fun NavHostController.selectTab(route: String) {
    if (currentDestination?.route == route) return

    val reused = popBackStack(route, inclusive = false)
    if (!reused) {
        navigate(route) {
            launchSingleTop = true
        }
    }
}
