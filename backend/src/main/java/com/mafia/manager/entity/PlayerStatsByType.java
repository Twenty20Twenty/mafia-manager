package com.mafia.manager.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Агрегированная статистика игрока за период, разбитая по типу турнира.
 *
 * <ul>
 *   <li>{@code tournamentType} — "individual" | "team" | "season"</li>
 *   <li>{@code periodYear}    — null = «за всё время», число = конкретный год</li>
 * </ul>
 *
 * Позволяет фронтенду фильтровать статистику:
 * «только личные», «только командные» или «только рейтинговые» турниры.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "player_stats_by_type",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_player_stats_by_type",
        columnNames = {"user_id", "tournament_type", "period_year"}
    )
)
public class PlayerStatsByType extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Тип турнира: "individual", "team" или "season".
     * Хранится как VARCHAR — не зависит от PostgreSQL-enum,
     * совместим с Java-enum TournamentType.name().
     */
    @Column(name = "tournament_type", nullable = false, length = 20)
    private String tournamentType;

    /** null = за всё время; 2025, 2026, ... = за год */
    @Column(name = "period_year")
    private Short periodYear;

    // ─── Общее ───────────────────────────────────────────────────────────────

    @Column(name = "total_games", nullable = false)
    private int totalGames = 0;

    // ─── Игры по ролям ───────────────────────────────────────────────────────

    @Column(name = "games_civilian", nullable = false)
    private int gamesCivilian = 0;

    @Column(name = "games_sheriff", nullable = false)
    private int gamesSheriff = 0;

    @Column(name = "games_mafia", nullable = false)
    private int gamesMafia = 0;

    @Column(name = "games_don", nullable = false)
    private int gamesDon = 0;

    // ─── Победы по ролям ─────────────────────────────────────────────────────

    @Column(name = "wins_civilian", nullable = false)
    private int winsCivilian = 0;

    @Column(name = "wins_sheriff", nullable = false)
    private int winsSheriff = 0;

    @Column(name = "wins_mafia", nullable = false)
    private int winsMafia = 0;

    @Column(name = "wins_don", nullable = false)
    private int winsDon = 0;

    // ─── Лучшие ходы ─────────────────────────────────────────────────────────

    @Column(name = "best_moves_total", nullable = false)
    private int bestMovesTotal = 0;

    @Column(name = "best_moves_perfect", nullable = false)
    private int bestMovesPerfect = 0;

    // ─── Первый убитый ───────────────────────────────────────────────────────

    @Column(name = "first_killed_count", nullable = false)
    private int firstKilledCount = 0;

    // ─── Дисциплина ──────────────────────────────────────────────────────────

    @Column(name = "total_fouls", nullable = false)
    private int totalFouls = 0;

    @Column(name = "last_recalculated_at", nullable = false)
    private LocalDateTime lastRecalculatedAt = LocalDateTime.now();
}
