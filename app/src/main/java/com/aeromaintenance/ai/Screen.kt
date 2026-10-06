package com.aeromaintenance.ai

/** Every destination in the app. Navigation is a simple back stack held in [AeroViewModel]. */
sealed interface Screen {
    val title: String

    data object Dashboard : Screen { override val title = "AeroMaintenance AI" }
    data object Fleet : Screen { override val title = "Aircraft" }
    data class AircraftDetail(val id: String) : Screen { override val title = id }
    data object Monitoring : Screen { override val title = "Sensor monitoring" }
    data object Analysis : Screen { override val title = "AI prediction" }
    data object Insights : Screen { override val title = "AI insights" }
    data object Maintenance : Screen { override val title = "Maintenance" }
    data object Alerts : Screen { override val title = "Alerts" }
    data object Reports : Screen { override val title = "Reports" }
    data object FacultyDemo : Screen { override val title = "Faculty demo mode" }
    data object About : Screen { override val title = "About" }

    companion object {
        /** Destinations in the bottom navigation bar. */
        val tabs: List<Screen> = listOf(Dashboard, Fleet, Monitoring, Analysis, Maintenance)

        fun fromKey(key: String?): Screen? = when (key?.lowercase()) {
            "dashboard" -> Dashboard
            "aircraft", "fleet" -> Fleet
            "monitoring", "sensors" -> Monitoring
            "analysis", "ai" -> Analysis
            "insights" -> Insights
            "maintenance", "predictive", "planning" -> Maintenance
            "alerts" -> Alerts
            "reports" -> Reports
            "faculty" -> FacultyDemo
            "about" -> About
            else -> null
        }
    }
}
