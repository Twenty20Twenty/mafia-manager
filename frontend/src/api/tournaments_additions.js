// src/api/tournaments.js  — дополнительные методы (добавить к существующим)

import api from './axios';

// ── Игры: удаление ────────────────────────────────────────────────────────────

/**
 * Удалить все игры турнира (сброс рассадки).
 */
export const deleteAllGames = (tournamentId) =>
    api.delete(`/tournaments/${tournamentId}/games`, {
        data: { mode: 'ALL' },
    }).then(r => r.data);

/**
 * Удалить игры конкретного тура.
 */
export const deleteRoundGames = (tournamentId, round) =>
    api.delete(`/tournaments/${tournamentId}/games`, {
        data: { mode: 'ROUND', fromRound: round },
    }).then(r => r.data);

/**
 * Удалить игры в диапазоне туров [fromRound, toRound].
 */
export const deleteRangeGames = (tournamentId, fromRound, toRound) =>
    api.delete(`/tournaments/${tournamentId}/games`, {
        data: { mode: 'RANGE', fromRound, toRound },
    }).then(r => r.data);

// ── Игры: замена состава стола ────────────────────────────────────────────────

/**
 * Заменить игрока в слоте. slotNumber или oldUserId — хотя бы одно обязательно.
 * newUserId = null освобождает слот.
 */
export const swapSlot = (gameId, { oldUserId, newUserId, slotNumber }) =>
    api.patch(`/games/${gameId}/slots`, { oldUserId, newUserId, slotNumber })
       .then(r => r.data);

// ── Финалисты ─────────────────────────────────────────────────────────────────

export const getFinalists = (tournamentId) =>
    api.get(`/tournaments/${tournamentId}/finalists`).then(r => r.data);

/**
 * Задать финалистов вручную. lock=true фиксирует список.
 */
export const setFinalists = (tournamentId, playerIds, lock = false) =>
    api.put(`/tournaments/${tournamentId}/finalists`, { playerIds, lock })
       .then(r => r.data);

/**
 * Автоматически заполнить финалистов из лидерборда (без фиксации).
 */
export const autoFillFinalists = (tournamentId) =>
    api.post(`/tournaments/${tournamentId}/finalists/auto-fill`).then(r => r.data);

/**
 * Зафиксировать текущий список финалистов.
 */
export const lockFinalists = (tournamentId) =>
    api.post(`/tournaments/${tournamentId}/finalists/lock`).then(r => r.data);

/**
 * Снять фиксацию финалистов.
 */
export const unlockFinalists = (tournamentId) =>
    api.post(`/tournaments/${tournamentId}/finalists/unlock`).then(r => r.data);
