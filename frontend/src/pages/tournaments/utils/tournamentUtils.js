// src/pages/tournaments/utils/tournamentUtils.js
import dayjs from 'dayjs';
import { TOURNAMENT_STATUS_LABELS } from '../../../api/tournaments';

// ── Права ────────────────────────────────────────────────────────────────────

export function resolveCanManage(user, tournament) {
    if (!user || !tournament) return false;
    if (user.role === 'admin') return true;
    return (
        user.id === tournament.organizerId ||
        user.id === tournament.headJudgeId
    );
}

export function resolveCanEdit(user, tournament, game) {
    if (!user) return false;
    if (user.isAdmin)                        return true;
    if (user.id === tournament?.organizerId) return true;
    if (user.id === tournament?.headJudgeId) return true;
    if (user.id === game?.judgeId)           return true;
    return false;
}

export function resolveGameResult(game) {
    if (game.status === 'draft')                              return { color: 'orange', text: 'Черновик' };
    if (!game.winner)                                         return { color: 'gray',   text: 'Не завершена' };
    if (game.winner === 'red')                                return { color: 'red',    text: 'Победа Мирных' };
    if (game.winner === 'black' || game.winner === 'mafia')   return { color: 'dark',   text: 'Победа Мафии' };
    if (game.winner === 'draw')                               return { color: 'orange', text: 'Ничья' };
    return { color: 'gray', text: 'Не завершена' };
}

// ── Маппинг исключений ───────────────────────────────────────────────────────

export function mapExceptions(data) {
    if (!Array.isArray(data)) return [];
    return data.map(e => ({
        id:      e.id,
        p1:      e.player1Id,
        p1Label: e.player1Nickname,
        p2:      e.player2Id,
        p2Label: e.player2Nickname,
    }));
}

// ── Инициализация формы настроек ─────────────────────────────────────────────

/**
 * Инициализирует локальный state настроек из DTO турнира.
 * cityId нужен для городского селекта в SettingsTab.
 * title копируется, чтобы можно было переименовывать.
 */
export function initSettings(t) {
    return {
        title:              t.title                        || '',
        status:             t.status                       || 'registration',
        description:        t.description                  || '',
        link:               t.settings?.socialLink         || '',
        cityId:             t.cityId                       || null,   // нужен отдельный cityId в DTO
        // Общие
        maxParticipants:    t.settings?.maxParticipants    || 10,
        areResultsHidden:   t.settings?.areResultsHidden   || false,
        // Личный / Командный
        roundsCount:        t.settings?.roundsCount        || 10,
        finalRoundsCount:   t.settings?.finalRoundsCount   || 0,
        finalCoefficient:   t.settings?.finalCoefficient   || 1.0,
        areQualifiersFixed: t.settings?.areQualifiersFixed || false,
        isSeedingGenerated: t.settings?.isSeedingGenerated || false,
        teamSize:           t.settings?.teamSize           || null,
        // Швейцарка
        isSwissSystem:      t.settings?.isSwissSystem      || false,
        swissRoundsStart:   t.settings?.swissRoundsStart   || null,
        swissTiers:         t.settings?.swissTiers         || null,
        // Рейтинг
        ratingThreshold:    t.settings?.ratingThreshold    ?? 0,
        // Судьи
        finalJudgeId:       t.settings?.finalJudgeId       || null,
        // Даты
        dates: [
            t.startDate ? new Date(t.startDate) : null,
            t.endDate   ? new Date(t.endDate)   : null,
        ],
    };
}

// ── Формирование payload для PUT /api/tournaments/{id} ───────────────────────

/**
 * Собирает payload для обновления турнира.
 * cityId передаётся на верхнем уровне (бэкенд читает из request.getCityId()).
 * title тоже на верхнем уровне — бэкенд вызывает t.setTitle().
 */
export function buildUpdatePayload(tournament, settings, headJudgeId) {
    return {
        title:        settings.title || tournament.title,
        description:  settings.description,
        type:         tournament.type,
        status:       settings.status,
        headJudgeId:  headJudgeId ? Number(headJudgeId) : null,
        finalJudgeId: settings.finalJudgeId ? Number(settings.finalJudgeId) : null,
        cityId:       settings.cityId ? Number(settings.cityId) : null,
        startDate:    settings.dates[0] ? dayjs(settings.dates[0]).format('YYYY-MM-DD') : null,
        endDate:      settings.dates[1] ? dayjs(settings.dates[1]).format('YYYY-MM-DD') : null,
        settings: {
            ...tournament.settings,
            maxParticipants:    settings.maxParticipants,
            roundsCount:        settings.roundsCount,
            finalRoundsCount:   settings.finalRoundsCount,
            finalCoefficient:   settings.finalCoefficient,
            ratingThreshold:    settings.ratingThreshold,
            teamSize:           settings.teamSize,
            areResultsHidden:   settings.areResultsHidden,
            areQualifiersFixed: settings.areQualifiersFixed,
            socialLink:         settings.link,
            isSwissSystem:      settings.isSwissSystem,
            swissRoundsStart:   settings.swissRoundsStart,
            swissTiers:         settings.swissTiers,
        },
    };
}

// ── Вспомогательные форматтеры ───────────────────────────────────────────────

export function formatTournamentStatus(status) {
    return TOURNAMENT_STATUS_LABELS[status] ?? status;
}

export function displayScore(value, decimals = 2) {
    if (value === null || value === undefined) return '—';
    return Number(value).toFixed(decimals);
}

export function displayInt(value) {
    if (value === null || value === undefined) return '—';
    return value;
}
