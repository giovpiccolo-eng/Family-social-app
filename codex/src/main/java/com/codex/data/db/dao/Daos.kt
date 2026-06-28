package com.codex.data.db.dao

import androidx.room.*
import com.codex.data.db.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress ORDER BY day")
    fun tutti(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE day = :day")
    suspend fun perGiorno(day: Int): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ProgressEntity)
}

@Dao
interface MasteryDao {
    @Query("SELECT * FROM mastery")
    fun tutti(): Flow<List<MasteryEntity>>

    @Query("SELECT * FROM mastery WHERE skillId = :id")
    suspend fun perSkill(id: String): MasteryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(m: MasteryEntity)
}

@Dao
interface SrsDao {
    @Query("SELECT * FROM srs_cards WHERE prossimaRevisione <= :adesso ORDER BY prossimaRevisione")
    suspend fun inScadenza(adesso: Long = System.currentTimeMillis()): List<SrsCardEntity>

    @Query("SELECT * FROM srs_cards WHERE cardId = :id")
    suspend fun perId(id: String): SrsCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: SrsCardEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun tutti(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE sblocato = 1")
    fun sbloccati(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(a: AchievementEntity)

    @Query("UPDATE achievements SET sblocato = 1, dataSblocco = :ts WHERE id = :id")
    suspend fun sblocca(id: String, ts: Long = System.currentTimeMillis())
}

@Dao
interface PlayerStatsDao {
    @Query("SELECT * FROM player_stats WHERE id = 1")
    fun osserva(): Flow<PlayerStatsEntity?>

    @Query("SELECT * FROM player_stats WHERE id = 1")
    suspend fun get(): PlayerStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: PlayerStatsEntity)
}

@Dao
interface SessionLogDao {
    @Query("SELECT * FROM session_log ORDER BY timestamp DESC LIMIT 30")
    fun recenti(): Flow<List<SessionLogEntity>>

    @Insert
    suspend fun inserisci(s: SessionLogEntity)

    @Query("SELECT AVG(accuratezza) FROM session_log")
    suspend fun accuratezzaMedia(): Float?

    @Query("SELECT SUM(durata) FROM session_log")
    suspend fun durataTotal(): Int?
}
