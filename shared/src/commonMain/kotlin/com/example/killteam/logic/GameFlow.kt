package com.example.killteam.logic

import com.example.killteam.data.ApprovedOps

/** Game flow rules as pure state transitions: initiative roll-off, Ready step and the end of the battle. */

val GameState.rollOffResolved get() = rollOffWinner != null

private fun <T> List<T>.setAt(index: Int, value: T) = mapIndexed { i, x -> if (i == index) value else x }

/** Sets the base roll (1-6) of a player. Ignored once the roll-off is resolved. */
fun GameState.withRoll(player: Int, value: Int): GameState =
    if (rollOffResolved) this else copy(rolls = rolls.setAt(player, value.coerceIn(1, 6)))

/** Plays the Re-roll card: the card is spent and the player rolls again, replacing any earlier modification. */
fun GameState.useReroll(player: Int): GameState {
    if (rollOffResolved || InitiativeCard.Reroll !in players[player].cards) return this
    return updatePlayer(player) { it.copy(cards = it.cards - InitiativeCard.Reroll) }
        .copy(rolls = rolls.setAt<Int?>(player, null))
}

/** Plays a +x/-x card on the current roll (the result may leave the 1-6 range). */
fun GameState.useModifier(player: Int, card: InitiativeCard, up: Boolean): GameState {
    val current = rolls[player]
    if (rollOffResolved || card == InitiativeCard.Reroll || card !in players[player].cards || current == null) return this
    val delta = if (up) card.modifier else -card.modifier
    return updatePlayer(player) { it.copy(cards = it.cards - card) }
        .copy(rolls = rolls.setAt<Int?>(player, current + delta))
}

/** Resolves the roll-off once: the loser gains the card of this turning point. Calling it again changes nothing. */
fun GameState.resolveRollOff(): GameState {
    val a = rolls[0]
    val b = rolls[1]
    if (rollOffResolved || a == null || b == null) return this
    val winner = Initiative.rollOffWinner(listOf(a, b), initiative)
    val card = Initiative.cardForTurningPoint(turningPoint)
    val withCard = if (card != null) updatePlayer(1 - winner) { it.copy(cards = it.cards + card) } else this
    return withCard.copy(rollOffWinner = winner, gainedCard = card, initiativeBeforeRollOff = initiative)
}

/** The roll-off winner decides who has initiative. */
fun GameState.chooseInitiative(player: Int): GameState =
    if (!rollOffResolved || readyDone) this else copy(initiative = player, initiativeChosen = true)

/** Takes back a resolved roll-off (e.g. a wrong entry), including the gained card. Not possible after the Ready step. */
fun GameState.reopenRollOff(): GameState {
    val winner = rollOffWinner ?: return this
    if (readyDone) return this
    val card = gainedCard
    val restored = if (card != null) updatePlayer(1 - winner) { it.copy(cards = it.cards - card) } else this
    return restored.copy(rollOffWinner = null, gainedCard = null, initiativeChosen = false, initiative = initiativeBeforeRollOff)
}

/** CP each player gains in the Ready step: 1CP, or 2CP for the player without initiative after the first turning point. */
fun GameState.readyGains(): List<Int> = players.indices.map { i -> if (turningPoint > 1 && i != initiative) 2 else 1 }

/** Ready step: players gain CP and all operatives become ready. Needs the initiative and happens once per turning point. */
fun GameState.readyStep(): GameState {
    if (!initiativeChosen || readyDone) return this
    val gains = readyGains()
    return copy(
        readyDone = true,
        players = players.mapIndexed { i, p ->
            p.copy(cp = p.cp + gains[i], operatives = p.operatives.map { it.copy(expended = false) })
        },
    )
}

/** CP cost of a ploy from its cost text ("1CP"); 1 CP when the text has no number. */
fun ployCost(cost: String): Int = Regex("\\d+").find(cost)?.value?.toIntOrNull() ?: 1

/** Uses a strategy ploy: needs the Ready step, an open Strategy phase, enough CP and a ploy not yet used this turning point. */
fun GameState.withPloy(player: Int, name: String, cost: Int): GameState {
    val p = players[player]
    if (!readyDone || strategyDone || name in p.ployNames || p.cp < cost) return this
    return updatePlayer(player) { it.copy(cp = it.cp - cost, ployNames = it.ployNames + name) }
}

/** Takes a ploy back before the Strategy phase is confirmed and returns its CP. */
fun GameState.withoutPloy(player: Int, name: String, cost: Int): GameState {
    if (strategyDone || name !in players[player].ployNames) return this
    return updatePlayer(player) { it.copy(cp = it.cp + cost, ployNames = it.ployNames - name) }
}

fun GameState.confirmStrategy(): GameState = copy(strategyDone = true)

fun GameState.reopenStrategy(): GameState = copy(strategyDone = false, firefightDone = false)

/** Ends the Firefight phase and opens the Score step; only possible after the Strategy phase. */
fun GameState.confirmFirefight(): GameState = if (strategyDone) copy(firefightDone = true) else this

fun GameState.reopenFirefight(): GameState = copy(firefightDone = false)

/** VP a player scored with each op in the current turning point. */
fun GameState.tpCrit(player: Int): Int = (players[player].critVp - players[player].critVpBase).coerceAtLeast(0)

fun GameState.tpTac(player: Int): Int = (players[player].tacVp - players[player].tacVpBase).coerceAtLeast(0)

fun GameState.tpKill(player: Int): Int = (Scoring.killVp(players[player], players[1 - player], false) - players[player].killVpBase).coerceAtLeast(0)

/** Most Tac op VP the chosen tac op allows in one turning point; null when it has no such limit. */
fun PlayerState.tacMaxPerTurningPoint(): Int? = ApprovedOps.tacOps.firstOrNull { it.id == tacOpId }?.maxVpPerTurningPoint

/** Changes the Crit op VP of this turning point; never below what was scored before and never above the op maximum. */
fun GameState.changeTpCrit(player: Int, delta: Int): GameState = updatePlayer(player) { p ->
    p.copy(critVp = (p.critVp + delta).coerceIn(p.critVpBase, maxOf(p.critVpBase, ApprovedOps.MAX_VP_PER_OP)))
}

/** Changes the Tac op VP of this turning point; additionally capped by the tac op's limit per turning point. */
fun GameState.changeTpTac(player: Int, delta: Int): GameState = updatePlayer(player) { p ->
    val perTp = p.tacMaxPerTurningPoint()
    val top = minOf(ApprovedOps.MAX_VP_PER_OP, if (perTp != null) p.tacVpBase + perTp else ApprovedOps.MAX_VP_PER_OP)
    p.copy(tacVp = (p.tacVp + delta).coerceIn(p.tacVpBase, maxOf(p.tacVpBase, top)))
}

/** Setting up the battle: the player with initiative picks a drop zone, the opponent holds the Re-roll card. */
fun GameState.chooseSetupInitiative(player: Int): GameState =
    copy(initiative = player)
        .updatePlayer(player) { p -> p.copy(cards = p.cards.filter { it != InitiativeCard.Reroll }) }
        .updatePlayer(1 - player) { p -> p.copy(cards = p.cards.filter { it != InitiativeCard.Reroll } + InitiativeCard.Reroll) }

fun GameState.changeOperative(player: Int, operative: Int, change: (OperativeTrack) -> OperativeTrack): GameState =
    updatePlayer(player) { p -> p.copy(operatives = p.operatives.mapIndexed { i, o -> if (i == operative) change(o) else o }) }

/** Marks an operative as incapacitated (or brings it back); the opponent's kill count for the kill op follows. */
fun GameState.setIncapacitated(player: Int, operative: Int, incapacitated: Boolean): GameState {
    val current = players[player].operatives.getOrNull(operative) ?: return this
    if (current.incapacitated == incapacitated) return this
    return changeOperative(player, operative) {
        it.copy(incapacitated = incapacitated, wounds = if (incapacitated) 0 else maxOf(it.wounds, 1))
    }.updatePlayer(1 - player) { it.copy(kills = (it.kills + if (incapacitated) 1 else -1).coerceIn(0, it.enemyStartCount)) }
}

/** Sets the number of operatives of the opponent's team (row of the kill grade table); the incapacitated count never exceeds it. */
fun GameState.withEnemyStart(player: Int, count: Int): GameState =
    updatePlayer(player) { it.copy(enemyStartCount = count, kills = it.kills.coerceAtMost(count)) }

/** End of the battle: primary ops are revealed (their bonus counts) and the kill op comparison applies. */
fun GameState.finish(): GameState =
    copy(ended = true, players = players.map { it.copy(primaryRevealed = it.primaryRevealed || it.primary != null) })
