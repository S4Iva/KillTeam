package com.example.killteam.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.killteam.data.Archetype

enum class Tab(val label: String) { Generator("Generator"), Game("Game"), Teams("Teams"), Rules("Rules"), More("More") }

enum class TeamsPage { List, Rosters }

enum class MorePage(val label: String) {
    History("Game history and statistics"),
    Tournament("Tournament mode"),
    Dice("Dice and damage calculator"),
    Favorites("Favorites and notes"),
    About("Data sources"),
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

    var rulesPage: String? by mutableStateOf(null)
    var rulesTarget: String? by mutableStateOf(null)
    var rulesHighlight: String? by mutableStateOf(null)
    var rulesQuery: String by mutableStateOf("")

    var morePage: MorePage? by mutableStateOf(null)
    var tournamentOpen: Long? by mutableStateOf(null)

    /** Tab to return to when the back button is pressed in the Rules tab (set when a keyword arrow opened it). */
    var returnTab: Tab? by mutableStateOf(null)

    private val lists = mutableMapOf<String, LazyListState>()

    fun list(key: String): LazyListState = lists.getOrPut(key) { LazyListState() }

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
