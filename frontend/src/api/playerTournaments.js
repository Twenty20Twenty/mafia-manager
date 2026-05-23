// src/api/playerTournaments.js
import api from './axios';

/**
 * Получает список турниров игрока.
 * Для завершённых турниров с открытыми результатами включает место в зачёте.
 *
 * @param {number} userId
 * @returns {Promise<PlayerTournamentDto[]>}
 */
export const getPlayerTournaments = (userId) =>
    api.get(`/users/${userId}/tournaments`).then(r => r.data);
