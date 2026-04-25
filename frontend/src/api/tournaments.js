// src/api/tournaments.js
import api from './axios';


export const TOURNAMENT_STATUSES = {
    REGISTRATION: 'registration',
    ACTIVE:       'active',
    COMPLETED:    'completed',
    ARCHIVED:     'archived',
};

// Метки для отображения в UI
export const TOURNAMENT_STATUS_LABELS = {
    registration: 'Регистрация',
    active:       'Идёт',
    completed:    'Завершён',
    archived:     'Архив',
};

export const tournamentsApi = {

    // ── Лидерборды ───────────────────────────────────────────────

    /** areResultsHidden обрабатывается на сервере;
     *  клиент просто отображает то, что пришло (null = скрыто) */
    getLeaderboard: (tournamentId, includeFinals = false, sortBy = 'total') =>
        api.get(`/tournaments/${tournamentId}/leaderboard`, {
            params: { includeFinals, sortBy }
        }).then(r => r.data),

    getTeamLeaderboard: async (id) => {
        const response = await api.get(`/tournaments/${id}/teams-leaderboard`);
        return response.data;
    },

    getNominations: async (id, calcMode) => {
        const response = await api.get(`/tournaments/${id}/nominations`, {
            params: {calcMode}
        });

        return response.data;
    },

    // ── Судьи столов ─────────────────────────────────────────────

    /**
     * GET /api/tournaments/{id}/table-judges
     * Загружает текущий map судей при монтировании страницы.
     * Больше не теряется при перезагрузке.
     */
    getTableJudges: async (id) => {
        const response = await api.get(`/tournaments/${id}/table-judges`);
        return response.data; // Map<String, Long>: { "1": 42, "2": 55 }
    },

    updateTableJudges: async (id, judgesMap) => {
        const response = await api.put(`/tournaments/${id}/table-judges`, judgesMap);
        return response.data;
    },

    // ── Рассадка ─────────────────────────────────────────────────

    generateSeeding: async (id) => {
        const response = await api.post(`/tournaments/${id}/generate-seeding`);
        return response.data;
    },
};
