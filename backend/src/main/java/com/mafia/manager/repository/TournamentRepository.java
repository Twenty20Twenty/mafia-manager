package com.mafia.manager.repository;

import com.mafia.manager.dto.LeaderboardEntryDto;
import com.mafia.manager.dto.NominationDto;
import com.mafia.manager.dto.TeamLeaderboardEntryDto;
import com.mafia.manager.entity.Tournament;
import com.mafia.manager.entity.enums.TournamentStatus;
import com.mafia.manager.entity.enums.TournamentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    // --- Базовые фильтры ---
    List<Tournament> findByClubId(Long clubId);

    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findByType(TournamentType type);

    List<Tournament> findByTypeAndStatus(TournamentType type, TournamentStatus status);

    List<Tournament> findByTitleContainingIgnoreCase(String title);

    @Query("""
                SELECT t FROM Tournament t
                WHERE (cast(:type as string) IS NULL OR t.type = :type)
                  AND (cast(:status as string) IS NULL OR t.status = :status)
                  AND (:clubId IS NULL OR t.club.id = :clubId)
                  AND (cast(:search as string) IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', COALESCE(cast(:search as string), ''), '%')))
                ORDER BY t.id DESC
            """)
    List<Tournament> findWithFilters(
            @Param("type") TournamentType type,
            @Param("status") TournamentStatus status,
            @Param("clubId") Long clubId,
            @Param("search") String search
    );

    @Query(value = """
        SELECT
              tp.tournament_id          AS tournamentId,
              tp.user_id                AS userId,
              u.nickname                AS nickname,
              u.avatar_url              AS avatarUrl,
              COALESCE(SUM(
                  CASE
                      WHEN :includeFinals = true AND g.stage = 'final_round'
                          THEN gs.computed_score * t.final_coefficient
                      ELSE gs.computed_score
                  END
              ), 0)                     AS totalScore,
              COUNT(gs.id)              AS gamesCount,
              COUNT(CASE
                  WHEN ((gs.role IN ('civilian', 'sheriff') AND g.winner = 'red')
                     OR (gs.role IN ('mafia', 'don')        AND g.winner = 'black'))
                  THEN 1
              END)                      AS totalWins,
              COUNT(CASE WHEN gs.role = 'sheriff' AND g.winner = 'red'   THEN 1 END) AS sheriffWins,
              COUNT(CASE WHEN gs.role = 'don'     AND g.winner = 'black' THEN 1 END) AS donWins,
              COUNT(CASE WHEN gs.is_first_killed = TRUE THEN 1 END)                  AS firstKilledCount,
              COALESCE(SUM(
                  CASE
                      WHEN :includeFinals = true AND g.stage = 'final_round'
                          THEN gs.extra_points_positive * t.final_coefficient
                      ELSE gs.extra_points_positive
                  END
              ), 0)                     AS extraPointsPositive,
              COALESCE(SUM(
                  CASE
                      WHEN :includeFinals = true AND g.stage = 'final_round'
                          THEN gs.extra_points_negative * t.final_coefficient
                      ELSE gs.extra_points_negative
                  END
              ), 0)                     AS extraPointsNegative,
              COALESCE(SUM(
                  CASE
                      WHEN :includeFinals = true AND g.stage = 'final_round'
                          THEN gs.penalty_points * t.final_coefficient
                      ELSE gs.penalty_points
                  END
              ), 0)                     AS penaltyPoints,
              COALESCE(SUM(
                  CASE
                      WHEN :includeFinals = true AND g.stage = 'final_round'
                          THEN gs.compensation_points * t.final_coefficient
                      ELSE gs.compensation_points
                  END
              ), 0)                     AS compensationPoints,
              COALESCE(SUM(
                  CASE
                      WHEN :includeFinals = true AND g.stage = 'final_round'
                          THEN bm.points * t.final_coefficient
                      ELSE bm.points
                  END
              ), 0)                     AS bestMovePoints
          FROM tournament_participants tp
          JOIN (
              SELECT
                  id,
                  COALESCE(CAST(settings->>'finalCoefficient' AS NUMERIC), 1.0) AS final_coefficient,
                  CASE 
                      WHEN :sortBy = 'avg' 
                          THEN COALESCE(CAST(settings->>'ratingThreshold'  AS INTEGER),  0) 
                          ELSE 0 
                      END AS rating_threshold
              FROM tournaments
          ) t ON t.id = tp.tournament_id
          JOIN users u ON u.id = tp.user_id
          -- Подзапрос: общее число завершённых игр турнира с учётом фильтра по стадии
          JOIN (
              SELECT
                  tournament_id,
                  COUNT(*) AS total_games
              FROM games
              WHERE status = 'completed'
                AND (:includeFinals = true OR stage = 'qualifying')
              GROUP BY tournament_id
          ) tg ON tg.tournament_id = tp.tournament_id
          LEFT JOIN games g ON g.tournament_id = tp.tournament_id
              AND g.status = 'completed'
              AND (:includeFinals = true OR g.stage = 'qualifying')
          LEFT JOIN game_slots gs ON gs.game_id = g.id AND gs.user_id = tp.user_id
          LEFT JOIN best_moves bm ON bm.game_id = g.id AND bm.author_slot_id = gs.id
          WHERE tp.tournament_id = :tournamentId
            AND tp.status = 'approved'
          GROUP BY
              tp.tournament_id,
              tp.user_id,
              u.nickname,
              u.avatar_url,
              t.final_coefficient,
              t.rating_threshold,
              tg.total_games
          -- Игрок должен сыграть >= (rating_threshold% от total_games) игр
          -- Пример: threshold=70, total=10 → FLOOR(10 * 70 / 100.0) = 7
          HAVING COUNT(gs.id) >= FLOOR( tg.total_games * t.rating_threshold / 100.0)
          ORDER BY
              CASE WHEN :sortBy = 'avg'
                  THEN CASE WHEN COUNT(gs.id) = 0 THEN 0
                       ELSE SUM(gs.computed_score) / COUNT(gs.id) END
              END DESC NULLS LAST,
              CASE WHEN :sortBy != 'avg' THEN SUM(gs.computed_score) END DESC NULLS LAST,
              totalWins DESC;
            """, nativeQuery = true)
    List<LeaderboardEntryDto> getLeaderboard(
            @Param("tournamentId") Long tournamentId,
            @Param("includeFinals") boolean includeFinals,
            @Param("sortBy") String sortBy
    );

    // ──────────────────────────────────────────────────────────────
    // КОМАНДНЫЙ ЛИДЕРБОРД — те же правила snake_case алиасов
    // ──────────────────────────────────────────────────────────────
    @Query(value = """
                SELECT
                    tt.id                                        AS team_id,
                    tt.name                                      AS team_name,
                    COALESCE(SUM(gs.computed_score), 0)          AS total_score,
                    COUNT(gs.id)                                 AS games_played,
                    COUNT(CASE
                        WHEN ((gs.role IN ('civilian','sheriff') AND g.winner = 'red')
                           OR (gs.role IN ('mafia','don')        AND g.winner = 'black'))
                        THEN 1
                    END)                                         AS total_wins
                FROM tournament_teams tt
                JOIN tournament_participants tp ON tp.team_id = tt.id
                LEFT JOIN games g       ON g.tournament_id = tt.tournament_id
                    AND g.status = 'completed'
                    AND g.stage  = 'qualifying'
                LEFT JOIN game_slots gs ON gs.game_id = g.id AND gs.user_id = tp.user_id
                WHERE tt.tournament_id = :tournamentId
                  AND tp.status = 'approved'
                GROUP BY tt.id, tt.name
                ORDER BY total_score DESC
            """, nativeQuery = true)
    List<TeamLeaderboardEntryDto> getTeamLeaderboard(@Param("tournamentId") Long tournamentId);

    @Query(value = """
        SELECT
            tp.tournament_id                                                                        AS tournamentId,
            tp.user_id                                                                              AS userId,
            u.nickname                                                                              AS nickname,
            u.avatar_url                                                                            AS avatarUrl,
            COUNT(gs.id)                                                                            AS gamesPlayed,

            CASE WHEN :calcMode = 'avg'
                THEN COALESCE(SUM(
                    gs.extra_points_positive
                    - gs.extra_points_negative
                    + COALESCE(bm.points, 0)
                ), 0) / NULLIF(COUNT(gs.id), 0)
                ELSE COALESCE(SUM(
                    gs.extra_points_positive
                    - gs.extra_points_negative
                    + COALESCE(bm.points, 0)
                ), 0)
            END                                                                                     AS mvpScore,

            CASE WHEN :calcMode = 'avg'
                THEN SUM(CASE WHEN gs.role = 'civilian'
                    THEN (gs.extra_points_positive - gs.extra_points_negative + COALESCE(bm.points, 0))
                END) / NULLIF(COUNT(CASE WHEN gs.role = 'civilian' THEN 1 END), 0)
                ELSE SUM(CASE WHEN gs.role = 'civilian'
                    THEN (gs.extra_points_positive - gs.extra_points_negative + COALESCE(bm.points, 0))
                END)
            END                                                                                     AS NominationScoreCivilian,

            CASE WHEN :calcMode = 'avg'
                THEN SUM(CASE WHEN gs.role = 'sheriff'
                    THEN (gs.extra_points_positive - gs.extra_points_negative + COALESCE(bm.points, 0))
                END) / NULLIF(COUNT(CASE WHEN gs.role = 'sheriff' THEN 1 END), 0)
                ELSE SUM(CASE WHEN gs.role = 'sheriff'
                    THEN (gs.extra_points_positive - gs.extra_points_negative + COALESCE(bm.points, 0))
                END)
            END                                                                                     AS NominationScoreSheriff,

            CASE WHEN :calcMode = 'avg'
                THEN SUM(CASE WHEN gs.role = 'mafia'
                    THEN (gs.extra_points_positive - gs.extra_points_negative)
                END) / NULLIF(COUNT(CASE WHEN gs.role = 'mafia' THEN 1 END), 0)
                ELSE SUM(CASE WHEN gs.role = 'mafia'
                    THEN (gs.extra_points_positive - gs.extra_points_negative)
                END)
            END                                                                                     AS NominationScoreMafia,

            CASE WHEN :calcMode = 'avg'
                THEN SUM(CASE WHEN gs.role = 'don'
                    THEN (gs.extra_points_positive - gs.extra_points_negative)
                END) / NULLIF(COUNT(CASE WHEN gs.role = 'don' THEN 1 END), 0)
                ELSE SUM(CASE WHEN gs.role = 'don'
                    THEN (gs.extra_points_positive - gs.extra_points_negative)
                END)
            END                                                                                     AS NominationScoreDon

        FROM tournament_participants tp
        JOIN (
            SELECT
                id,
                COALESCE(CAST(settings->>'ratingThreshold' AS INTEGER), 0) AS rating_threshold
            FROM tournaments
        ) t ON t.id = tp.tournament_id
        JOIN users u
            ON u.id = tp.user_id
        JOIN (
            SELECT
                tournament_id,
                COUNT(*) AS total_games
            FROM games
            WHERE status = 'completed'
              AND stage  = 'qualifying'
            GROUP BY tournament_id
        ) tg ON tg.tournament_id = tp.tournament_id
        LEFT JOIN games g
            ON  g.tournament_id = tp.tournament_id
            AND g.status        = 'completed'
            AND g.stage         = 'qualifying'
        LEFT JOIN game_slots gs
            ON  gs.game_id = g.id
            AND gs.user_id = tp.user_id
        LEFT JOIN best_moves bm
            ON  bm.game_id        = g.id
            AND bm.author_slot_id = gs.id
        WHERE tp.tournament_id = :tournamentId
          AND tp.status        = 'approved'
        GROUP BY tp.tournament_id, tp.user_id, u.nickname, u.avatar_url,
                 t.rating_threshold, tg.total_games
        HAVING COUNT(gs.id) >= FLOOR( t.rating_threshold * tg.total_games / 100.0)
    """, nativeQuery = true)
    List<NominationDto> getNominations(
            @Param("tournamentId") Long tournamentId,
            @Param("calcMode") String calcMode   // "sum" | "avg"
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE tournaments SET settings = jsonb_set(settings, '{isSeedingGenerated}', 'true') WHERE id = :id", nativeQuery = true)
    void setSeedingStatusTrue(@Param("id") Long id);
}
