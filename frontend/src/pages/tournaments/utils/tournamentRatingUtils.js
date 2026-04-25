// src/pages/tournaments/utils/tournamentRatingUtils.js

/**
 * Считает минимальный порог игр для попадания в рейтинг.
 * threshold — процент (0–100), totalGames — всего завершённых игр.
 * Формула: FLOOR(totalGames * threshold / 100)
 */
export function calcThresholdGames(totalGames, thresholdPercent) {
    if (!totalGames || !thresholdPercent) return 0;
    return Math.floor(totalGames * thresholdPercent / 100);
}