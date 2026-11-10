package com.vislupus.dream_records.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.ui.AppViewModel
import com.vislupus.dream_records.ui.components.Lucide
import com.vislupus.dream_records.ui.components.LucideIcon
import com.vislupus.dream_records.ui.screens.CalendarScreen
import com.vislupus.dream_records.ui.screens.LibraryScreen
import com.vislupus.dream_records.ui.screens.NightScreen
import com.vislupus.dream_records.ui.screens.RecordScreen
import com.vislupus.dream_records.ui.screens.RecordingDetailScreen
import com.vislupus.dream_records.ui.screens.SettingsScreen
import com.vislupus.dream_records.ui.screens.TagsScreen

/** Route names. Tabs map 1:1 to AppConfig.Tab. */
object Routes {
    const val RECORD = "record"
    const val LIBRARY = "library"
    const val TAGS = "tags"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val RECORDING = "recording/{id}"
    const val NIGHT = "night/{date}"
    fun recording(id: String) = "recording/$id"
    fun night(date: String) = "night/$date"

    fun forTab(tab: AppConfig.Tab) = when (tab) {
        AppConfig.Tab.RECORD -> RECORD
        AppConfig.Tab.LIBRARY -> LIBRARY
        AppConfig.Tab.TAGS -> TAGS
        AppConfig.Tab.CALENDAR -> CALENDAR
        AppConfig.Tab.SETTINGS -> SETTINGS
    }
}

private data class TabItem(val route: String, val icon: Int, val label: String)

@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val s = LocalStrings.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val tabs = listOf(
        TabItem(Routes.RECORD, Lucide.Mic, s.tabRecord),
        TabItem(Routes.LIBRARY, Lucide.Library, s.tabLibrary),
        TabItem(Routes.TAGS, Lucide.Tags, s.tabTags),
        TabItem(Routes.CALENDAR, Lucide.Calendar, s.tabCalendar),
        TabItem(Routes.SETTINGS, Lucide.Settings, s.tabSettings),
    )

    Scaffold(
        containerColor = AppConfig.Theme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { BottomBar(nav, tabs) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            NavHost(nav, startDestination = Routes.forTab(AppConfig.App.START_TAB)) {
                composable(Routes.RECORD) { RecordScreen(vm, onOpenRecording = { nav.navigate(Routes.recording(it)) }) }
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        vm,
                        onOpenRecording = { nav.navigate(Routes.recording(it)) },
                        onOpenNight = { nav.navigate(Routes.night(it)) },
                    )
                }
                composable(Routes.TAGS) { TagsScreen(vm) }
                composable(Routes.CALENDAR) {
                    CalendarScreen(
                        vm,
                        onOpenRecording = { nav.navigate(Routes.recording(it)) },
                        onOpenNight = { nav.navigate(Routes.night(it)) },
                    )
                }
                composable(Routes.SETTINGS) { SettingsScreen(vm) }
                composable(Routes.RECORDING, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    RecordingDetailScreen(vm, id = it.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
                }
                composable(Routes.NIGHT, arguments = listOf(navArgument("date") { type = NavType.StringType })) {
                    NightScreen(
                        vm,
                        nightDate = it.arguments?.getString("date").orEmpty(),
                        onBack = { nav.popBackStack() },
                        onOpenRecording = { id -> nav.navigate(Routes.recording(id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, tabs: List<TabItem>) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    Column(Modifier.background(AppConfig.Theme.background).windowInsetsPadding(WindowInsets.navigationBars)) {
        HorizontalDivider(color = AppConfig.Theme.surfaceBorder, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(AppConfig.Layout.bottomBarHeight)) {
            tabs.forEach { tab ->
                val selected = current == tab.route
                val tint = if (selected) AppConfig.Theme.accent else AppConfig.Theme.onSurfaceMuted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (!selected) nav.navigate(tab.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    LucideIcon(tab.icon, tint = tint)
                    Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
