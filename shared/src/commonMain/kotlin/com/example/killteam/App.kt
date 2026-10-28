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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.killteam.logic.AppState
import com.example.killteam.ui.GameScreen
import com.example.killteam.ui.GeneratorScreen
import com.example.killteam.ui.KeywordContext
import com.example.killteam.ui.LocalKeywords
import com.example.killteam.ui.LocalUi
import com.example.killteam.ui.MoreScreen
import com.example.killteam.ui.RulesScreen
import com.example.killteam.ui.Tab
import com.example.killteam.ui.TeamsScreen
import com.example.killteam.ui.UiState
import com.example.killteam.ui.Waha

private val KillTeamColors = darkColorScheme(
    primary = Waha.Orange,
    onPrimary = Color.White,
    secondary = Waha.Link,
    background = Waha.Background,
    surface = Waha.Background,
    surfaceVariant = Waha.Card,
    onSurface = Waha.Text,
    onSurfaceVariant = Waha.Text,
    onBackground = Waha.Text,
    error = Color(0xFFFF5252),
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    val app = remember { AppState() }
    val ui = remember { UiState() }
    var tab by remember { mutableStateOf(Tab.Generator) }
    LaunchedEffect(Unit) { app.load() }

    // Back steps out of detail pages first, then returns to where a keyword arrow came from, then to the generator.
    BackHandler(enabled = tab != Tab.Generator || ui.canGoBack(tab)) {
        val returnTo = ui.returnTab
        when {
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
        val keywords = remember(app.matcher) {
            KeywordContext(app.matcher) { ref ->
                ui.returnTab = tab
                app.pendingRule = ref
                tab = Tab.Rules
            }
        }
        CompositionLocalProvider(LocalKeywords provides keywords, LocalUi provides ui) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().imePadding()) {
                    // The status bar area gets the header colour; the navigation bar draws itself under the gesture bar.
                    Spacer(Modifier.fillMaxWidth().background(Waha.Black).windowInsetsTopHeight(WindowInsets.statusBars))
                    if (app.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Waha.Orange)
                    Column(Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
                        when (tab) {
                            Tab.Generator -> GeneratorScreen(app, onStartGame = { tab = Tab.Game })
                            Tab.Game -> GameScreen(app, onOpenGenerator = { tab = Tab.Generator })
                            Tab.Teams -> TeamsScreen(app)
                            Tab.Rules -> RulesScreen(app)
                            Tab.More -> MoreScreen(app)
                        }
                    }
                    NavigationBar(containerColor = Waha.Black) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = t == tab, onClick = { ui.returnTab = null; tab = t }, icon = {}, label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedTextColor = Waha.Orange,
                                    unselectedTextColor = Waha.Text,
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
