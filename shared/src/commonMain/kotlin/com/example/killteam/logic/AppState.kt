package com.example.killteam.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.killteam.data.KillTeam
import com.example.killteam.data.KillTeamData
import com.example.killteam.data.LookupTerm
import com.example.killteam.data.RulesData
import com.example.killteam.data.TermMatcher
import com.example.killteam.data.loadKillTeamData
import com.example.killteam.data.loadLookupData
import com.example.killteam.data.loadRulesData
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer

data class RuleRef(val page: String, val anchor: String)

class AppState(settings: Settings = Settings()) {
    val mission = Persisted(settings, "mission", MissionState.serializer(), MissionState())
    val favorites = Persisted(settings, "favorites", ListSerializer(String.serializer()), emptyList())
    val notes = Persisted(settings, "notes", MapSerializer(String.serializer(), String.serializer()), emptyMap())
    val rosters = Persisted(settings, "rosters", ListSerializer(Roster.serializer()), emptyList())
    val game = Persisted(settings, "game", GameState.serializer().nullable, null)
    val history = Persisted(settings, "history", ListSerializer(GameResult.serializer()), emptyList())
    val tournaments = Persisted(settings, "tournaments", ListSerializer(Tournament.serializer()), emptyList())
    val measure = Persisted(settings, "measure", MeasureSettings.serializer(), MeasureSettings())
    val rulesRecent = Persisted(settings, "rulesRecent", ListSerializer(String.serializer()), emptyList())
    val rulesPinned = Persisted(settings, "rulesPinned", ListSerializer(String.serializer()), emptyList())

    var teamData: KillTeamData? by mutableStateOf(null)
        private set
    var rules: RulesData? by mutableStateOf(null)
        private set
    var matcher: TermMatcher? by mutableStateOf(null)
        private set
    var lookup: List<LookupTerm> by mutableStateOf(emptyList())
        private set
    var loadError: String? by mutableStateOf(null)
        private set
    var loading: Boolean by mutableStateOf(true)
        private set

    /** Rule section the Rules tab should open next (set by the arrow in a keyword popup). */
    var pendingRule: RuleRef? by mutableStateOf(null)

    /** Reads and parses the bundled data off the main thread. */
    suspend fun load() {
        loading = true
        loadError = null
        try {
            coroutineScope {
                val teams = async(Dispatchers.Default) { loadKillTeamData() }
                val rulesData = async(Dispatchers.Default) { loadRulesData() }
                val termData = async(Dispatchers.Default) { loadLookupData().terms }
                val matcherData = async(Dispatchers.Default) { TermMatcher(termData.await()) }
                teamData = teams.await().let { d -> KillTeamData(d.source, d.importedAt, d.teams.sortedBy { it.name.lowercase() }) }
                rules = rulesData.await()
                lookup = termData.await()
                matcher = matcherData.await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError = e.message ?: e.toString()
        } finally {
            loading = false
        }
    }

    val teams: List<KillTeam> get() = teamData?.teams.orEmpty()

    fun team(slug: String?): KillTeam? = slug?.let { s -> teams.firstOrNull { it.slug == s } }

    fun isFavorite(key: String) = key in favorites.value

    fun toggleFavorite(key: String) =
        favorites.update(if (isFavorite(key)) favorites.value - key else favorites.value + key)

    fun note(key: String): String = notes.value[key].orEmpty()

    fun setNote(key: String, text: String) =
        notes.update(if (text.isBlank()) notes.value - key else notes.value + (key to text))

    /** Remembers a rules search (newest first, no duplicates, at most [MAX_RECENT]). */
    fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.length < 3) return
        rulesRecent.update((listOf(q) + rulesRecent.value.filterNot { it.equals(q, ignoreCase = true) }).take(MAX_RECENT))
    }

    /** Pins or unpins a keyword (by title) for the start page of the Rules tab. */
    fun togglePinned(title: String) =
        rulesPinned.update(if (title in rulesPinned.value) rulesPinned.value - title else rulesPinned.value + title)

    companion object {
        const val MAX_RECENT = 6
    }
}
