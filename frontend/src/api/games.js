// src/api/games.js
import api from './axios';

export const gamesApi = {
    /**
     * Создать новую пустую игру в турнире.
     * POST /api/tournaments/{tournamentId}/games
     *
     * @param {string|number} tournamentId
     * @param {{ date?: string|Date, judgeId?: number }} [options]
     * @returns {Promise<GameProtocolDto>}
     */
    createGame: async (tournamentId, options = {}) => {
        const payload = {
            date:    options.date    ?? null,
            judgeId: options.judgeId ?? null,
        };
        const response = await api.post(`/tournaments/${tournamentId}/games`, payload);
        return response.data;
    },

    /**
     * Получить протокол игры по её ID.
     * GET /api/games/{gameId}
     * @returns {Promise<GameProtocolDto>}
     */
    getGame: async (gameId) => {
        const response = await api.get(`/games/${gameId}`);
        return response.data;
    },

    /**
     * Сохранить / обновить протокол игры.
     * PUT /api/games/{gameId}
     * @param {number} gameId
     * @param {GameProtocolDto} dto
     */
    saveProtocol: async (gameId, dto) => {
        const response = await api.put(`/games/${gameId}/protocol`, dto);
        return response.data;
    },

    /**
     * Удалить игру.
     * DELETE /api/games/{gameId}
     */
    deleteGame: async (gameId) => {
        const response = await api.delete(`/games/${gameId}`);
        return response.data;
    },
};
