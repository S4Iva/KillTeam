package com.example.killteam.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.killteam.data.Archetype
import com.example.killteam.data.SearchEntry
import killteam.shared.generated.resources.Res
import killteam.shared.generated.resources.ic_game
import killteam.shared.generated.resources.ic_generator
import killteam.shared.generated.resources.ic_more
import killteam.shared.generated.resources.ic_rules
import killteam.shared.generated.resources.ic_teams
import org.jetbrains.compose.resources.DrawableResource

enum class Tab(val label: String, val icon: DrawableResource) {
    Generator("Generator", Res.drawable.ic_generator),
    Game("Game", Res.drawable.ic_game),
    Teams("Teams", Res.drawable.ic_teams),
    Rules("Rules", Res.drawable.ic_rules),
    More("More", Res.drawable.ic_more),
}

enum class TeamsPage { List, Rosters }

enum class MorePage(val label: String) {
    History("Game history and statistics"),
    Notes("Notes"),
    Tournament("Tournament mode"),
    Dice("Dice and damage calculator"),
    Measure("Measure distances (camera)"),
    Options("Options"),
    About("Data sources"),
    Terms("Terms of use"),
    Privacy("Privacy"),
}

/** Navigation, search and scroll state that must survive switching tabs and is used by the back button. */
class UiState {
    var teamsSelected: String? by mutableStateOf(null)
    var teamsPage: TeamsPage by mutableStateOf(TeamsPage.List)
    var rosterEditing: Long? by mutableStateOf(null)
    var teamTab: Int by mutableStateOf(0)
    var teamQuery: String by mutableStateOf("")
    var teamArchetype: Archetype? by mutableStateOf(null)
    var teamFavoritesOnly: Boolean by mutableStateOf(false)

    /** Search hit the team detail should scroll to and highlight once; cleared by the detail page. */
    var teamHit: SearchEntry? by mutableStateOf(null)

    /** Tab the app should switch to (set by pages that link into another tab); consumed by the app shell. */
    var requestedTab: Tab? by mutableStateOf(null)

    /** Favorite key ("crit:3", "tac:flank") the generator should scroll to and highlight once. */
    var generatorTarget: String? by mutableStateOf(null)

    var rulesPage: String? by mutableStateOf(null)
    var rulesTarget: String? by mutableStateOf(null)
    var rulesHighlight: String? by mutableStateOf(null)
    var rulesQuery: String by mutableStateOf("")

    /** Rules tab: switch ALL / Favorites. */
    var rulesFavoritesOnly: Boolean by mutableStateOf(false)

    /** Rules tab: 0 = Rules (start page and search), 1 = Glossary. */
    var rulesTab: Int by mutableStateOf(0)
    var rulesQuickTab: Int by mutableStateOf(0)
    var rulesQuickOpen: String? by mutableStateOf(null)
    var glossaryQuery: String by mutableStateOf("")

    var morePage: MorePage? by mutableStateOf(null)
    var tournamentOpen: Long? by mutableStateOf(null)

    /** Tab to return to when the back button is pressed in the Rules tab (set when a keyword arrow opened it). */
    var returnTab: Tab? by mutableStateOf(null)

    /** Phase and turning point the game list was last scrolled for; a change scrolls it back to the top. */
    var gameScrollKey: String? = null

    /** Changes on every tab tap; the screen is rebuilt with it so local view state (dialogs, filters, sub pages) starts fresh. */
    var homeTick: Int by mutableStateOf(0)

    private val lists = mutableMapOf<String, LazyListState>()

    fun list(key: String): LazyListState = lists.getOrPut(key) { LazyListState() }

    private fun listTab(key: String): Tab? = when {
        key == "generator" -> Tab.Generator
        key == "game" || key == "new-game" -> Tab.Game
        key == "teams" || key.startsWith("team:") -> Tab.Teams
        key == "rules-index" || key == "rules-glossary" || key.startsWith("rules:") -> Tab.Rules
        key == "more" -> Tab.More
        else -> null
    }

    /** Tab bar tap: closes sub pages and searches of the tab and starts it at the top. Saved data is not touched. */
    fun goHome(tab: Tab) {
        returnTab = null
        generatorTarget = null
        lists.keys.filter { listTab(it) == tab }.forEach { lists.remove(it) }
        when (tab) {
            Tab.Teams -> {
                teamsSelected = null
                teamsPage = TeamsPage.List
                rosterEditing = null
                teamTab = 0
                teamHit = null
                teamQuery = ""
                teamArchetype = null
                teamFavoritesOnly = false
            }
            Tab.Rules -> {
                closeRulesPage()
                rulesQuery = ""
                rulesTab = 0
                rulesQuickTab = 0
                rulesQuickOpen = null
                glossaryQuery = ""
            }
            Tab.More -> {
                morePage = null
                tournamentOpen = null
            }
            else -> Unit
        }
        homeTick++
    }

    fun canGoBack(tab: Tab): Boolean = when (tab) {
        Tab.Teams -> rosterEditing != null || teamsSelected != null || teamsPage != TeamsPage.List
        Tab.Rules -> rulesPage != null || returnTab != null
        Tab.More -> tournamentOpen != null || morePage != null
        else -> false
    }

    /** Steps back inside the tab; returns false when there is nothing left to go back to. */
    fun back(tab: Tab): Boolean {
        when (tab) {
            Tab.Teams -> when {
                rosterEditing != null -> rosterEditing = null
                teamsSelected != null -> teamsSelected = null
                teamsPage != TeamsPage.List -> teamsPage = TeamsPage.List
                else -> return false
            }
            Tab.Rules -> if (rulesPage != null) closeRulesPage() else return false
            Tab.More -> when {
                tournamentOpen != null -> tournamentOpen = null
                morePage != null -> morePage = null
                else -> return false
            }
            else -> return false
        }
        return true
    }

    fun closeRulesPage() {
        rulesPage = null
        rulesTarget = null
        rulesHighlight = null
    }
}

val LocalUi = staticCompositionLocalOf { UiState() }
