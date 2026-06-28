package com.codex.data.repository

import android.content.Context
import com.codex.data.db.CodexDatabase
import com.codex.data.db.entity.*
import com.codex.data.model.ExerciseItem
import com.codex.data.model.SkillTree
import com.codex.domain.SrsEngine
import com.codex.domain.XpEngine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

class CodexRepository(private val ctx: Context) {

    private val db = CodexDatabase.get(ctx)
    private val gson = Gson()

    // ── Contenuto JSON ────────────────────────────────────────────────────────

    private val _exercises: List<ExerciseItem> by lazy {
        val json = ctx.assets.open("data/content.json").bufferedReader().readText()
        val type = object : TypeToken<List<ExerciseItem>>() {}.type
        gson.fromJson(json, type)
    }

    private val _skillTree: SkillTree by lazy {
        val json = ctx.assets.open("data/skill_tree.json").bufferedReader().readText()
        gson.fromJson(json, SkillTree::class.java)
    }

    fun eserciziPerGiorno(day: Int): List<ExerciseItem> =
        _exercises.filter { it.day == day }

    fun eserciziSRS(ids: List<String>): List<ExerciseItem> =
        _exercises.filter { it.id in ids }

    fun skillTree() = _skillTree

    // ── Progresso ─────────────────────────────────────────────────────────────

    fun tuttoIlProgresso(): Flow<List<ProgressEntity>> = db.progressDao().tutti()

    suspend fun progressoGiorno(day: Int): ProgressEntity? = db.progressDao().perGiorno(day)

    suspend fun completaGiorno(day: Int, xp: Int, aurei: Int) {
        db.progressDao().upsert(
            ProgressEntity(
                day = day,
                completato = true,
                xpGuadagnato = xp,
                aureiGuadagnati = aurei,
                dataCompletamento = System.currentTimeMillis()
            )
        )
    }

    // ── Maestria ──────────────────────────────────────────────────────────────

    fun tuttaMaestria(): Flow<List<MasteryEntity>> = db.masteryDao().tutti()

    suspend fun aggiornaMaestria(skillId: String, corretta: Boolean) {
        val existing = db.masteryDao().perSkill(skillId)
        val delta = if (corretta) 5f else -2f
        val nuovoPunteggio = ((existing?.punteggio ?: 0f) + delta).coerceIn(0f, 100f)
        db.masteryDao().upsert(
            MasteryEntity(
                skillId = skillId,
                punteggio = nuovoPunteggio,
                ultimaRevisione = System.currentTimeMillis(),
                totaleRisposte = (existing?.totaleRisposte ?: 0) + 1,
                risposteCorrette = (existing?.risposteCorrette ?: 0) + (if (corretta) 1 else 0)
            )
        )
    }

    // ── SRS ───────────────────────────────────────────────────────────────────

    suspend fun carteInScadenza(): List<SrsCardEntity> = db.srsDao().inScadenza()

    suspend fun aggiornaCartaSRS(cardId: String, qualita: Int) {
        val card = db.srsDao().perId(cardId) ?: SrsCardEntity(cardId)
        val (nuovoIntervallo, nuovoEase, nuoveRip) =
            SrsEngine.aggiorna(card.intervallo, card.easeFactor, card.ripetizioni, qualita)
        db.srsDao().upsert(
            card.copy(
                intervallo = nuovoIntervallo,
                easeFactor = nuovoEase,
                ripetizioni = nuoveRip,
                prossimaRevisione = System.currentTimeMillis() + nuovoIntervallo * 86_400_000L
            )
        )
    }

    // ── Stats giocatore ───────────────────────────────────────────────────────

    fun osservaStats(): Flow<PlayerStatsEntity?> = db.playerStatsDao().osserva()

    suspend fun getStats(): PlayerStatsEntity =
        db.playerStatsDao().get() ?: PlayerStatsEntity()

    suspend fun statsEsistono(): Boolean = db.playerStatsDao().get() != null

    suspend fun aggiungiXPAurei(xp: Int, aurei: Int) {
        val s = getStats()
        val nuovoXP = s.xpTotale + xp
        val (livello, titolo) = XpEngine.livello(nuovoXP)
        db.playerStatsDao().upsert(
            s.copy(
                xpTotale = nuovoXP,
                aureiTotali = s.aureiTotali + aurei,
                livello = livello,
                titolo = titolo
            )
        )
    }

    suspend fun aggiornaStreak() {
        val s = getStats()
        val adesso = System.currentTimeMillis()
        val ieri = adesso - 86_400_000L
        val nuovoStreak = if (s.ultimaSessione > ieri) s.streakCorrente + 1
                          else if (s.gettonigelo > 0 && s.ultimaSessione > (ieri - 86_400_000L)) {
                              db.playerStatsDao().upsert(s.copy(gettonigelo = s.gettonigelo - 1)); s.streakCorrente
                          } else 1
        db.playerStatsDao().upsert(
            s.copy(
                streakCorrente = nuovoStreak,
                streakMassimo = maxOf(s.streakMassimo, nuovoStreak),
                ultimaSessione = adesso
            )
        )
    }

    suspend fun impostaTheme(scuro: Boolean) {
        val s = getStats()
        db.playerStatsDao().upsert(s.copy(temaScuro = scuro))
    }

    suspend fun spendiAurei(quanti: Int): Boolean {
        val s = getStats()
        return if (s.aureiTotali >= quanti) {
            db.playerStatsDao().upsert(s.copy(aureiTotali = s.aureiTotali - quanti)); true
        } else false
    }

    // ── Achievement ───────────────────────────────────────────────────────────

    fun tuttiAchievement(): Flow<List<AchievementEntity>> = db.achievementDao().tutti()

    suspend fun sbloccaAchievement(id: String) = db.achievementDao().sblocca(id)

    // ── Session log ───────────────────────────────────────────────────────────

    suspend fun registraSessione(day: Int, durata: Int, xp: Int, aurei: Int,
                                  accuratezza: Float, round: Int) {
        db.sessionLogDao().inserisci(
            SessionLogEntity(
                day = day, timestamp = System.currentTimeMillis(),
                durata = durata, xp = xp, aurei = aurei,
                accuratezza = accuratezza, roundCompletati = round
            )
        )
    }

    fun sessioniRecenti() = db.sessionLogDao().recenti()
    suspend fun accuratezzaMedia() = db.sessionLogDao().accuratezzaMedia() ?: 0f
    suspend fun durataTotal() = db.sessionLogDao().durataTotal() ?: 0
}
