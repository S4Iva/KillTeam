package com.example.killteam.data

import killteam.shared.generated.resources.Res
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonTransformingSerializer

/** The importer (PowerShell) emits a single element as a plain value instead of an array. */
abstract class LenientList<T>(element: KSerializer<T>) : JsonTransformingSerializer<List<T>>(ListSerializer(element)) {
    override fun transformDeserialize(element: JsonElement): JsonElement = when (element) {
        is JsonArray -> element
        is JsonNull -> JsonArray(emptyList())
        else -> JsonArray(listOf(element))
    }
}

object LenientStringList : LenientList<String>(String.serializer())
object LenientItemList : LenientList<NamedText>(NamedText.serializer())
object LenientTokenList : LenientList<TokenInfo>(TokenInfo.serializer())
object LenientOperativeList : LenientList<Operative>(Operative.serializer())
object LenientWeaponList : LenientList<Weapon>(Weapon.serializer())
object LenientTeamList : LenientList<KillTeam>(KillTeam.serializer())
object LenientBlockList : LenientList<AbilityBlock>(AbilityBlock.serializer())
object LenientParaList : LenientList<AbilityPara>(AbilityPara.serializer())

/** One paragraph of an ability: kind is effect, condition, text or bullet. */
@Serializable
class AbilityPara(val kind: String = "text", val text: String = "")

/** An action (orange bar with AP) or a named ability of a datacard. */
@Serializable
class AbilityBlock(
    val kind: String = "ability",
    val name: String = "",
    val ap: String = "",
    @Serializable(with = LenientParaList::class) val paras: List<AbilityPara> = emptyList(),
) {
    val isAction get() = kind == "action"
}

@Serializable
class NamedText(
    val name: String = "",
    val text: String = "",
    val cost: String = "",
    @Serializable(with = LenientParaList::class) val paras: List<AbilityPara> = emptyList(),
)

@Serializable
class TokenInfo(val name: String = "", val image: String = "")

@Serializable
class Weapon(
    val name: String = "",
    val type: String = "ranged",
    val atk: String = "",
    val hit: String = "",
    val dmg: String = "",
    val wr: String = "-",
) {
    val isMelee get() = type == "melee"
}

@Serializable
class Operative(
    val name: String = "",
    val apl: String = "",
    val move: String = "",
    val save: String = "",
    val wounds: String = "",
    @Serializable(with = LenientWeaponList::class) val weapons: List<Weapon> = emptyList(),
    @Serializable(with = LenientStringList::class) val rules: List<String> = emptyList(),
    val keywords: String = "",
    val flavor: String = "",
    val baseSize: String = "",
    @Serializable(with = LenientBlockList::class) val blocks: List<AbilityBlock> = emptyList(),
) {
    val woundsValue get() = wounds.filter { it.isDigit() }.toIntOrNull() ?: 0
}

@Serializable
class KillTeam(
    val slug: String,
    val name: String = slug,
    val description: String = "",
    @Serializable(with = LenientStringList::class) val archetypes: List<String> = emptyList(),
    @Serializable(with = LenientStringList::class) val composition: List<String> = emptyList(),
    val factionRules: String = "",
    @Serializable(with = LenientItemList::class) val factionRuleItems: List<NamedText> = emptyList(),
    @Serializable(with = LenientItemList::class) val strategyPloys: List<NamedText> = emptyList(),
    @Serializable(with = LenientItemList::class) val firefightPloys: List<NamedText> = emptyList(),
    @Serializable(with = LenientItemList::class) val equipment: List<NamedText> = emptyList(),
    @Serializable(with = LenientTokenList::class) val tokens: List<TokenInfo> = emptyList(),
    @Serializable(with = LenientOperativeList::class) val operatives: List<Operative> = emptyList(),
) {
    val archetypeSet: Set<Archetype>
        get() = archetypes.mapNotNull { a -> Archetype.entries.firstOrNull { it.label == a.trim() } }.toSet()
}

@Serializable
class KillTeamData(
    val source: String = "",
    val importedAt: String = "",
    @Serializable(with = LenientTeamList::class) val teams: List<KillTeam> = emptyList(),
)

val AppJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

fun parseKillTeamData(text: String): KillTeamData = AppJson.decodeFromString(KillTeamData.serializer(), text)

suspend fun loadKillTeamData(): KillTeamData =
    parseKillTeamData(Res.readBytes("files/killteams.json").decodeToString())
