// java/com/mafia/manager/dto/PlayerStatsDto.java
package com.mafia.manager.dto;

import com.mafia.manager.entity.PlayerStats;
import lombok.Builder;
import lombok.Data;

/**
 * DTO статистики игрока за один период.
 * periodYear = null → "за всё время"
 */
@Data
@Builder
public class PlayerStatsDto {

    private Long userId;

    /** null = за всё время; 2025, 2026, ... = за год */
    private Short periodYear;

    private int totalGames;

    // --- Игры по ролям ---
    private int gamesCivilian;
    private int gamesSheriff;
    private int gamesMafia;
    private int gamesDon;

    // --- Победы по ролям ---
    private int winsCivilian;
    private int winsSheriff;
    private int winsMafia;
    private int winsDon;

    // --- Winrate (вычисляется на лету, не хранится в БД) ---
    private double winRateTotal;
    private double winRateCivilian;
    private double winRateSheriff;
    private double winRateMafia;
    private double winRateDon;

    // --- Лучшие ходы ---
    private int bestMovesTotal;
    private int bestMovesPerfect;

    // --- Первый убитый ---
    private int firstKilledCount;

    // --- Дисциплина ---
    private int totalFouls;

    // --- Вспомогательная функция для расчёта процента ---
    private static double rate(int wins, int games) {
        if (games == 0) return 0.0;
        return Math.round((wins * 100.0 / games) * 10.0) / 10.0;
    }

    public static PlayerStatsDto fromEntity(PlayerStats e) {
        int totalWins = e.getWinsCivilian() + e.getWinsSheriff() + e.getWinsMafia() + e.getWinsDon();

        return PlayerStatsDto.builder()
                .userId(e.getUser().getId())
                .periodYear(e.getPeriodYear())
                .totalGames(e.getTotalGames())
                .gamesCivilian(e.getGamesCivilian())
                .gamesSheriff(e.getGamesSheriff())
                .gamesMafia(e.getGamesMafia())
                .gamesDon(e.getGamesDon())
                .winsCivilian(e.getWinsCivilian())
                .winsSheriff(e.getWinsSheriff())
                .winsMafia(e.getWinsMafia())
                .winsDon(e.getWinsDon())
                .winRateTotal(rate(totalWins, e.getTotalGames()))
                .winRateCivilian(rate(e.getWinsCivilian(), e.getGamesCivilian()))
                .winRateSheriff(rate(e.getWinsSheriff(), e.getGamesSheriff()))
                .winRateMafia(rate(e.getWinsMafia(), e.getGamesMafia()))
                .winRateDon(rate(e.getWinsDon(), e.getGamesDon()))
                .bestMovesTotal(e.getBestMovesTotal())
                .bestMovesPerfect(e.getBestMovesPerfect())
                .firstKilledCount(e.getFirstKilledCount())
                .totalFouls(e.getTotalFouls())
                .build();
    }
}
