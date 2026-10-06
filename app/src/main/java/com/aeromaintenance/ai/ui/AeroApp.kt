@file:OptIn(ExperimentalMaterial3Api::class)

package com.aeromaintenance.ai.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.AeroViewModel
import com.aeromaintenance.ai.DemoScript
import com.aeromaintenance.ai.R
import com.aeromaintenance.ai.Screen
import com.aeromaintenance.ai.notify.AlertNotifier
import com.aeromaintenance.ai.ui.components.PrimaryButton
import com.aeromaintenance.ai.ui.components.SecondaryButton
import com.aeromaintenance.ai.ui.screens.AboutScreen
import com.aeromaintenance.ai.ui.screens.AircraftDetailScreen
import com.aeromaintenance.ai.ui.screens.AlertsScreen
import com.aeromaintenance.ai.ui.screens.AnalysisScreen
import com.aeromaintenance.ai.ui.screens.DashboardScreen
import com.aeromaintenance.ai.ui.screens.FacultyDemoScreen
import com.aeromaintenance.ai.ui.screens.FleetScreen
import com.aeromaintenance.ai.ui.screens.InsightsScreen
import com.aeromaintenance.ai.ui.screens.MaintenanceScreen
import com.aeromaintenance.ai.ui.screens.MonitoringScreen
import com.aeromaintenance.ai.ui.screens.ReportsScreen
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType
import kotlinx.coroutines.launch

/** Asks for the notification permission (Android 13+) right before the app first raises an alert. */
val LocalRequestNotifications = staticCompositionLocalOf<() -> Unit> { {} }

private data class TabItem(val screen: Screen, val label: String, val icon: ImageVector)

private val tabItems = listOf(
    TabItem(Screen.Dashboard, "Dashboard", Icons.Filled.Dashboard),
    TabItem(Screen.Fleet, "Aircraft", Icons.Filled.Flight),
    TabItem(Screen.Monitoring, "Monitor", Icons.Filled.MonitorHeart),
    TabItem(Screen.Analysis, "AI", Icons.Filled.Psychology),
    TabItem(Screen.Maintenance, "Maint.", Icons.Filled.Build),
)

@Composable
fun AeroApp(vm: AeroViewModel) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 && !AlertNotifier.canPost(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    BackHandler(enabled = !drawerState.isOpen && vm.backStack.size > 1) { vm.back() }

    CompositionLocalProvider(LocalRequestNotifications provides requestNotifications) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(vm, onClose = { scope.launch { drawerState.close() } }, requestNotifications)
            },
        ) {
            val current = vm.current
            val isRoot = current in Screen.tabs
            Scaffold(
                containerColor = Aero.Night,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Aero.Night,
                            titleContentColor = Aero.Text,
                            navigationIconContentColor = Aero.Text,
                            actionIconContentColor = Aero.Text,
                        ),
                        navigationIcon = {
                            if (isRoot) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                                }
                            } else {
                                IconButton(onClick = { vm.back() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        title = {
                            Column {
                                Text(current.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (current == Screen.Dashboard) {
                                    Text("Predictive maintenance prototype, simulated data", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { vm.navigate(Screen.Alerts) }) {
                                val count = vm.unacknowledgedCount
                                BadgedBox(badge = {
                                    if (count > 0) Badge(containerColor = Aero.Red, contentColor = Aero.Night) { Text("$count") }
                                }) {
                                    Icon(Icons.Filled.Notifications, contentDescription = "Alerts")
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    Column {
                        if (vm.demo != null) DemoPanel(vm)
                        NavigationBar(containerColor = Aero.Panel, tonalElevation = 0.dp) {
                            tabItems.forEach { item ->
                                val selected = current == item.screen ||
                                    (item.screen == Screen.Fleet && current is Screen.AircraftDetail)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { vm.navigate(item.screen) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Aero.Cyan,
                                        selectedTextColor = Aero.Cyan,
                                        indicatorColor = Aero.Cyan.copy(alpha = 0.16f),
                                        unselectedIconColor = Aero.TextMuted,
                                        unselectedTextColor = Aero.TextMuted,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Crossfade(targetState = current, animationSpec = tween(220), label = "screen") { screen ->
                    Box(Modifier.fillMaxSize()) {
                        ScreenHost(vm, screen, padding)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenHost(vm: AeroViewModel, screen: Screen, padding: PaddingValues) {
    when (screen) {
        Screen.Dashboard -> DashboardScreen(vm, padding)
        Screen.Fleet -> FleetScreen(vm, padding)
        is Screen.AircraftDetail -> AircraftDetailScreen(vm, screen.id, padding)
        Screen.Monitoring -> MonitoringScreen(vm, padding)
        Screen.Analysis -> AnalysisScreen(vm, padding)
        Screen.Insights -> InsightsScreen(vm, padding)
        Screen.Maintenance -> MaintenanceScreen(vm, padding)
        Screen.Alerts -> AlertsScreen(vm, padding)
        Screen.Reports -> ReportsScreen(vm, padding)
        Screen.FacultyDemo -> FacultyDemoScreen(vm, padding)
        Screen.About -> AboutScreen(padding)
    }
}

@Composable
private fun AppDrawer(vm: AeroViewModel, onClose: () -> Unit, requestNotifications: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = Aero.Panel, drawerContentColor = Aero.Text) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            Row(Modifier.padding(start = 8.dp, top = 20.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Aero.Night),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_launcher_foreground), null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("AeroMaintenance AI", style = MaterialTheme.typography.titleLarge, color = Aero.Text)
                    Text("Research prototype, simulated data", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
                }
            }
            HorizontalDivider(color = Aero.Hairline)
            DrawerGroup("Operations")
            DrawerEntry(vm, "Dashboard", Icons.Filled.Dashboard, Screen.Dashboard, onClose)
            DrawerEntry(vm, "Aircraft", Icons.Filled.Flight, Screen.Fleet, onClose)
            DrawerEntry(vm, "Sensor monitoring", Icons.Filled.MonitorHeart, Screen.Monitoring, onClose)
            DrawerEntry(vm, "AI prediction", Icons.Filled.Psychology, Screen.Analysis, onClose)
            DrawerEntry(vm, "Alerts", Icons.Filled.Notifications, Screen.Alerts, onClose, badge = vm.unacknowledgedCount)
            DrawerGroup("Maintenance")
            DrawerItemRaw("Predictive maintenance", Icons.Filled.Timeline, vm.current == Screen.Maintenance && vm.maintenanceTab == 0) {
                vm.openMaintenance(0); onClose()
            }
            DrawerItemRaw("Maintenance planning", Icons.Filled.Build, vm.current == Screen.Maintenance && vm.maintenanceTab == 1) {
                vm.openMaintenance(1); onClose()
            }
            DrawerEntry(vm, "Reports", Icons.Filled.Description, Screen.Reports, onClose)
            DrawerGroup("Explain")
            DrawerEntry(vm, "AI insights", Icons.Filled.Insights, Screen.Insights, onClose)
            DrawerEntry(vm, "Faculty demo mode", Icons.Filled.School, Screen.FacultyDemo, onClose)
            DrawerEntry(vm, "About and disclaimer", Icons.Filled.Info, Screen.About, onClose)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                "Start demo",
                onClick = { requestNotifications(); vm.startDemo(); onClose() },
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DrawerGroup(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = Aero.TextFaint,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun DrawerEntry(vm: AeroViewModel, label: String, icon: ImageVector, screen: Screen, onClose: () -> Unit, badge: Int = 0) {
    DrawerItemRaw(label, icon, vm.current == screen, badge) {
        vm.navigate(screen); onClose()
    }
}

@Composable
private fun DrawerItemRaw(label: String, icon: ImageVector, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, style = MaterialTheme.typography.titleSmall) },
        icon = { Icon(icon, null) },
        selected = selected,
        onClick = onClick,
        badge = { if (badge > 0) Text("$badge", style = AeroType.Tag, color = Aero.Red) },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Aero.Cyan.copy(alpha = 0.14f),
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            selectedIconColor = Aero.Cyan,
            selectedTextColor = Aero.Cyan,
            unselectedIconColor = Aero.TextMuted,
            unselectedTextColor = Aero.Text,
        ),
        modifier = Modifier.height(48.dp),
    )
}

/** Narration and controls for the guided demo; sits above the navigation bar. */
@Composable
private fun DemoPanel(vm: AeroViewModel) {
    val d = vm.demo ?: return
    val step = DemoScript.steps[d.step]
    Surface(color = Aero.PanelRaised, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (d.finished) "Demo complete" else "Step ${d.step + 1} of ${DemoScript.steps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Aero.Cyan,
                    )
                    Text(
                        if (d.finished) "End-to-end workflow shown" else step.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Aero.Text,
                    )
                }
                IconButton(onClick = { vm.exitDemo() }) { Icon(Icons.Filled.Close, contentDescription = "Exit demo", tint = Aero.TextMuted) }
            }
            if (d.finished) {
                Text(
                    "Telemetry, anomaly score, risk, remaining useful life, recommendation, alert and work order: all generated from the simulated data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Aero.TextMuted,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Generate report", onClick = {
                        vm.exitDemo()
                        vm.navigate(Screen.Reports)
                        vm.generateReport()
                    }, icon = Icons.Filled.Description, modifier = Modifier.weight(1f))
                    SecondaryButton("Replay", onClick = { vm.startDemo() }, modifier = Modifier.height(52.dp))
                }
            } else {
                Text(
                    step.narration(vm.demoResult),
                    style = MaterialTheme.typography.bodySmall,
                    color = Aero.TextMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { d.progress },
                        modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = Aero.Cyan,
                        trackColor = Aero.Hairline,
                    )
                    IconButton(onClick = { vm.demoPrev() }, enabled = d.step > 0) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous step", tint = if (d.step > 0) Aero.Text else Aero.TextFaint)
                    }
                    IconButton(onClick = { vm.demoTogglePause() }) {
                        Icon(
                            if (d.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (d.paused) "Resume" else "Pause",
                            tint = Aero.Cyan,
                        )
                    }
                    IconButton(onClick = { vm.demoNext() }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next step", tint = Aero.Text)
                    }
                }
            }
        }
    }
}
