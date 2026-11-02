package com.example.killteam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.killteam.logic.AccessGate
import com.example.killteam.logic.AccessResult
import com.example.killteam.logic.AccessStatus
import com.example.killteam.logic.formatLocalDateTime
import com.example.killteam.logic.formatRemaining
import com.example.killteam.logic.AppState
import com.example.killteam.ui.GameScreen
import com.example.killteam.ui.GeneratorScreen
import com.example.killteam.ui.KeywordContext
import com.example.killteam.ui.LocalKeywords
import com.example.killteam.ui.LocalUi
import com.example.killteam.ui.LockScreen
import com.example.killteam.ui.MoreScreen
import com.example.killteam.ui.RulesScreen
import com.example.killteam.ui.Tab
import com.example.killteam.ui.TeamsScreen
import com.example.killteam.ui.UiState
import com.example.killteam.ui.Palette
import com.example.killteam.ui.isDesktop
import com.russhwolf.settings.Settings
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

private val KillTeamColors = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Color.White,
    secondary = Palette.Link,
    background = Palette.Background,
    surface = Palette.Background,
    surfaceVariant = Palette.Card,
    onSurface = Palette.Text,
    onSurfaceVariant = Palette.Text,
    onBackground = Palette.Text,
    error = Color(0xFFFF5252),
)

@Composable
@Preview
fun App() {
    val gate = remember { AccessGate(Settings()) }
    var unlocked by remember { mutableStateOf(gate.isUnlocked()) }
    var notice by remember { mutableStateOf<AccessStatus?>(null) }
    // A limited code can run out while the app stays open.
    LaunchedEffect(unlocked) {
        while (unlocked) {
            delay(60_000)
            if (!gate.isUnlocked()) unlocked = false
        }
    }
    if (unlocked) {
        KillTeamApp(gate)
        notice?.let { n ->
            MaterialTheme(colorScheme = KillTeamColors) {
                AlertDialog(
                    onDismissRequest = { notice = null },
                    title = { Text("Access code accepted") },
                    text = {
                        Text(
                            if (n is AccessStatus.Temporary) {
                                "The app can be used for ${formatRemaining(n.remainingMillis)} (until ${formatLocalDateTime(n.expiresAt)}, device time). " +
                                    "After that it asks for a new code. The remaining time is shown under More."
                            } else "Unlimited access. This is shown under More.",
                        )
                    },
                    confirmButton = { TextButton({ notice = null }) { Text("OK") } },
                )
            }
        }
    } else {
        MaterialTheme(colorScheme = KillTeamColors) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                LockScreen(gate.lockReason()) { code ->
                    gate.attempt(code).also {
                        if (it == AccessResult.Unlocked) {
                            notice = gate.status().takeIf { s -> s is AccessStatus.Unlimited || s is AccessStatus.Temporary }
                            unlocked = true
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun KillTeamApp(gate: AccessGate) {
    val app = remember { AppState() }
    val ui = remember { UiState() }
    var tab by remember { mutableStateOf(Tab.Generator) }
    LaunchedEffect(Unit) { app.load() }
    LaunchedEffect(ui.requestedTab) { ui.requestedTab?.let { tab = it; ui.requestedTab = null } }
    LaunchedEffect(tab) { if (ui.favoritesOrigin != tab) ui.favoritesOrigin = null }

    // Back steps out of detail pages first, then returns to where a keyword arrow came from, then to the generator.
    BackHandler(enabled = tab != Tab.Generator || ui.canGoBack(tab)) {
        val returnTo = ui.returnTab
        when {
            ui.favoritesBack(tab) -> ui.leaveToFavorites()
            tab == Tab.Rules && returnTo != null && ui.rulesPage != null -> {
                ui.closeRulesPage()
                ui.returnTab = null
                tab = returnTo
            }
            ui.back(tab) -> Unit
            else -> {
                ui.returnTab = null
                tab = Tab.Generator
            }
        }
    }

    MaterialTheme(colorScheme = KillTeamColors) {
        val touchTarget = if (isDesktop) 56.dp else 48.dp
        val keywords = remember(app.matcher) {
            KeywordContext(app.matcher) { ref ->
                ui.returnTab = tab
                app.pendingRule = ref
                tab = Tab.Rules
            }
        }
        CompositionLocalProvider(LocalKeywords provides keywords, LocalUi provides ui, LocalMinimumInteractiveComponentSize provides touchTarget) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().imePadding()) {
                    // The status bar area gets the header colour; the navigation bar draws itself under the gesture bar.
                    Spacer(Modifier.fillMaxWidth().background(Palette.Header).windowInsetsTopHeight(WindowInsets.statusBars))
                    if (app.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Palette.Accent)
                    Column(Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
                        key(ui.homeTick) {
                            when (tab) {
                                Tab.Generator -> GeneratorScreen(app, onStartGame = { tab = Tab.Game })
                                Tab.Game -> GameScreen(app, onOpenGenerator = { tab = Tab.Generator })
                                Tab.Teams -> TeamsScreen(app)
                                Tab.Rules -> RulesScreen(app)
                                Tab.More -> MoreScreen(app, gate)
                            }
                        }
                    }
                    NavigationBar(containerColor = Palette.Header) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = t == tab, onClick = { ui.goHome(t); tab = t },
                                icon = { Icon(painterResource(t.icon), contentDescription = t.label) },
                                label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Palette.Accent,
                                    unselectedIconColor = Palette.Text,
                                    selectedTextColor = Palette.Accent,
                                    unselectedTextColor = Palette.Text,
                                    indicatorColor = Color(0xFF3C3C3C),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
