package com.example.killteam.logic

import kotlin.random.Random

data class DiceCounts(val crit: Int, val normal: Int, val miss: Int, val probability: Double)

/**
 * Simplified shooting/fighting model: attack dice hit on `hit`+ (6 is a crit), defence dice save on `save`+ (6 is a crit save).
 * Crit saves block crit hits first, then normal hits; normal saves only block normal hits. Check the rulebook for special rules.
 */
object DamageCalc {
    data class Result(
        val expectedDamage: Double,
        val damageDistribution: Map<Int, Double>,
        val expectedHits: Double,
    ) {
        fun chanceAtLeast(damage: Int) = damageDistribution.filterKeys { it >= damage }.values.sum()
    }

    private fun factorial(n: Int): Double = (2..n).fold(1.0) { a, b -> a * b }

    fun outcomes(dice: Int, threshold: Int): List<DiceCounts> {
        val t = threshold.coerceIn(2, 6)
        val pc = 1.0 / 6
        val pn = (6 - t) / 6.0
        val pm = 1.0 - pc - pn
        val list = mutableListOf<DiceCounts>()
        for (c in 0..dice) for (n in 0..(dice - c)) {
            val m = dice - c - n
            val p = factorial(dice) / (factorial(c) * factorial(n) * factorial(m)) * pow(pc, c) * pow(pn, n) * pow(pm, m)
            if (p > 0) list += DiceCounts(c, n, m, p)
        }
        return list
    }

    private fun pow(x: Double, n: Int): Double { var r = 1.0; repeat(n) { r *= x }; return r }

    fun calculate(attackDice: Int, hit: Int, normalDmg: Int, critDmg: Int, defenceDice: Int, save: Int): Result {
        val dist = mutableMapOf<Int, Double>()
        var expectedHits = 0.0
        for (a in outcomes(attackDice.coerceAtLeast(0), hit)) {
            expectedHits += (a.crit + a.normal) * a.probability
            for (d in outcomes(defenceDice.coerceAtLeast(0), save)) {
                val critHits = (a.crit - d.crit).coerceAtLeast(0)
                val leftoverCritSaves = (d.crit - a.crit).coerceAtLeast(0)
                val normalHits = (a.normal - leftoverCritSaves - d.normal).coerceAtLeast(0)
                val damage = critHits * critDmg + normalHits * normalDmg
                dist[damage] = (dist[damage] ?: 0.0) + a.probability * d.probability
            }
        }
        return Result(dist.entries.sumOf { it.key * it.value }, dist.entries.sortedBy { it.key }.associate { it.key to it.value }, expectedHits)
    }

    fun rollD6(random: Random = Random.Default) = random.nextInt(1, 7)
}
