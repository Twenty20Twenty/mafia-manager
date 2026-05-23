// src/api/teams.js
import api from './axios';

export const teamsApi = {
    /** Получить все команды турнира */
    getTeams: (tournamentId) =>
        api.get(`/tournaments/${tournamentId}/teams`).then(r => r.data),

    /** Создать команду */
    createTeam: (tournamentId, name = '') =>
        api.post(`/tournaments/${tournamentId}/teams`, { name }).then(r => r.data),

    /** Переименовать команду */
    renameTeam: (tournamentId, teamId, name) =>
        api.patch(`/tournaments/${tournamentId}/teams/${teamId}/name`, { name }).then(r => r.data),

    /** Удалить команду */
    deleteTeam: (tournamentId, teamId) =>
        api.delete(`/tournaments/${tournamentId}/teams/${teamId}`),

    /** Назначить состав команды (массовая замена) */
    assignMembers: (tournamentId, teamId, memberIds) =>
        api.put(`/tournaments/${tournamentId}/teams/${teamId}/members`, { memberIds }).then(r => r.data),

    /** Открепить участника от любой команды */
    removeMember: (tournamentId, userId) =>
        api.delete(`/tournaments/${tournamentId}/teams/members/${userId}`),

    /** Получить N случайных названий */
    generateNames: (tournamentId, count = 10) =>
        api.get(`/tournaments/${tournamentId}/teams/generate-names`, { params: { count } }).then(r => r.data),
};
