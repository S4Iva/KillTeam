package com.example.killteam.logic

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
    }.updatePlayer(1 - player) { it.copy(kills = (it.kills + if (incapacitated) 1 else -1).coerceAtLeast(0)) }
}

/** End of the battle: primary ops are revealed (their bonus counts) and the kill op comparison applies. */
fun GameState.finish(): GameState =
    copy(ended = true, players = players.map { it.copy(primaryRevealed = it.primaryRevealed || it.primary != null) })
