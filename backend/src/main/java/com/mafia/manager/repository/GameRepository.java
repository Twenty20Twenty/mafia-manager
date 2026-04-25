package com.mafia.manager.repository;

import com.mafia.manager.entity.Game;
import com.mafia.manager.entity.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByTournamentId(Long tournamentId);

    @Query(value = "SELECT tournament_id FROM games WHERE id = :gameId", nativeQuery = true)
    Long findTournamentIdByGameId(@Param("gameId") Long gameId);

    long countByTournamentIdAndStatus(Long tournamentId, GameStatus status);

    // ── Удаление по турниру ───────────────────────────────────────────────────

    /** Удаляет все игры турнира (сброс рассадки). */
    @Modifying
    @Query("DELETE FROM Game g WHERE g.tournament.id = :tournamentId")
    void deleteAllByTournamentId(@Param("tournamentId") Long tournamentId);

    /** Удаляет игры турнира за туры из указанного диапазона включительно. */
    @Modifying
    @Query("DELETE FROM Game g WHERE g.tournament.id = :tournamentId " +
            "AND g.roundNumber >= :fromRound AND g.roundNumber <= :toRound")
    void deleteByTournamentIdAndRoundNumberBetween(
            @Param("tournamentId") Long tournamentId,
            @Param("fromRound")    int   fromRound,
            @Param("toRound")      int   toRound);

    // ── Смена состава стола ───────────────────────────────────────────────────

    /** Возвращает все игры конкретного тура по турниру. */
    List<Game> findByTournamentIdAndRoundNumber(Long tournamentId, Integer roundNumber);
}
