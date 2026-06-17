package com.mafia.manager.dto;

import com.mafia.manager.entity.PlayerStatsByType;
import lombok.Builder;
import lombok.Data;

/**
 * DTO статистики игрока за один период в разрезе типа турнира.
 *
 * <ul>
 *   <li>{@code tournamentType} — "individual" | "team" | "season"</li>
 *   <li>{@code periodYear}     — null = «за всё время», число = конкретный год</li>
 * </ul>
 */
@Data
@Builder
public class PlayerStatsByTypeDto {

    private Long   userId;
    private String tournamentType;  // "individual" | "team" | "season"

    /** null = за всё время; 2025, 2026, ... = за год */
    private Short periodYear;

    private int totalGames;

    // ─── Игры по ролям ───────────────────────────────────────────────────────
    private int gamesCivilian;
    private int gamesSheriff;
    private int gamesMafia;
    private int gamesDon;

    // ─── Победы по ролям ─────────────────────────────────────────────────────
    private int winsCivilian;
    private int winsSheriff;
    private int winsMafia;
    private int winsDon;

    // ─── Win rate (вычисляется на лету, не хранится в БД) ────────────────────
    private double winRateTotal;
    private double winRateCivilian;
    private double winRateSheriff;
    private double winRateMafia;
    private double winRateDon;

    // ─── Лучшие ходы ─────────────────────────────────────────────────────────
    private int bestMovesTotal;
    private int bestMovesPerfect;

    // ─── Первый убитый ───────────────────────────────────────────────────────
    private int firstKilledCount;

    // ─── Дисциплина ──────────────────────────────────────────────────────────
    private int totalFouls;

    // ─── Маппинг из сущности ─────────────────────────────────────────────────

    public static PlayerStatsByTypeDto fromEntity(PlayerStatsByType e) {
        int totalWins = e.getWinsCivilian() + e.getWinsSheriff()
                      + e.getWinsMafia()    + e.getWinsDon();

        return PlayerStatsByTypeDto.builder()
                .userId(e.getUser().getId())
                .tournamentType(e.getTournamentType())
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

    private static double rate(int wins, int games) {
        if (games == 0) return 0.0;
        return Math.round((wins * 100.0 / games) * 10.0) / 10.0;
    }
}
