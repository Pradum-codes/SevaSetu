package com.example.sevasetu.navigation

sealed class AppRoute(val route: String) {
    data object Splash : AppRoute("splash")
    data object Login : AppRoute("login")
    data object AccountCreation : AppRoute("accountCreation")

    data object Home : AppRoute("home")
    data object Reports : AppRoute("reports")
    data object Alerts : AppRoute("alerts")
    data object Profile : AppRoute("profile")

    data object IssueReport : AppRoute("issueReport")
}

val bottomTabRoutes = listOf(
    AppRoute.Home.route,
    AppRoute.Reports.route,
    AppRoute.Alerts.route,
    AppRoute.Profile.route
)
