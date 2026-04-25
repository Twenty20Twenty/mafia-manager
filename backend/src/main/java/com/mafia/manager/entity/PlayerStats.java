// java/com/mafia/manager/entity/PlayerStats.java
package com.mafia.manager.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Агрегированная статистика игрока за период.
 * periodYear = null  → "за всё время"
 * periodYear = 2025  → за 2025 год
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "player_stats")
public class PlayerStats extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** null = за всё время; 2025, 2026, ... = за год */
    @Column(name = "period_year")
    private Short periodYear;

    @Column(name = "total_games", nullable = false)
    private int totalGames = 0;

    // --- Игры по ролям ---
    @Column(name = "games_civilian", nullable = false)
    private int gamesCivilian = 0;

    @Column(name = "games_sheriff", nullable = false)
    private int gamesSheriff = 0;

    @Column(name = "games_mafia", nullable = false)
    private int gamesMafia = 0;

    @Column(name = "games_don", nullable = false)
    private int gamesDon = 0;

    // --- Победы по ролям ---
    @Column(name = "wins_civilian", nullable = false)
    private int winsCivilian = 0;

    @Column(name = "wins_sheriff", nullable = false)
    private int winsSheriff = 0;

    @Column(name = "wins_mafia", nullable = false)
    private int winsMafia = 0;

    @Column(name = "wins_don", nullable = false)
    private int winsDon = 0;

    // --- Лучшие ходы ---
    @Column(name = "best_moves_total", nullable = false)
    private int bestMovesTotal = 0;

    @Column(name = "best_moves_perfect", nullable = false)
    private int bestMovesPerfect = 0;

    // --- Первый убитый ---
    @Column(name = "first_killed_count", nullable = false)
    private int firstKilledCount = 0;

    // --- Фолы ---
    @Column(name = "total_fouls", nullable = false)
    private int totalFouls = 0;

    @Column(name = "last_recalculated_at", nullable = false)
    private LocalDateTime lastRecalculatedAt = LocalDateTime.now();
}
