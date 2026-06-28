package com.codex.domain

object XpEngine {
    private val soglie = listOf(0, 200, 600, 1400, 3000, 6000)
    private val titoli = listOf("Tiro", "Legionarius", "Centurio", "Tribunus", "Legatus", "Imperator")

    fun livello(xpTotale: Int): Pair<Int, String> {
        var lv = 0
        for (i in soglie.indices) {
            if (xpTotale >= soglie[i]) lv = i + 1
        }
        lv = lv.coerceIn(1, 6)
        return lv to titoli[lv - 1]
    }

    fun xpPerProssimo(xpTotale: Int): Int {
        val (lv, _) = livello(xpTotale)
        return if (lv >= soglie.size) 0 else soglie[lv] - xpTotale
    }

    fun progressoLivello(xpTotale: Int): Float {
        val (lv, _) = livello(xpTotale)
        if (lv >= soglie.size) return 1f
        val inizio = soglie[lv - 1]
        val fine = soglie[lv]
        return ((xpTotale - inizio).toFloat() / (fine - inizio)).coerceIn(0f, 1f)
    }
}
