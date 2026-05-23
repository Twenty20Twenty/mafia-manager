// src/pages/tournaments/utils/tournamentSortUtils.js

/**
 * Статусы, которые считаются «завершёнными» — уходят в конец списка.
 */
const FINISHED_STATUSES = new Set(['completed', 'archived']);

/**
 * Сортирует массив турниров:
 * 1. Сначала активные / на регистрации (по убыванию startDate — новые вверху).
 * 2. Затем завершённые / архивные  (по убыванию startDate — новые вверху).
 *
 * Турниры без даты считаются самыми старыми внутри своей группы.
 *
 * @param {Array} tournaments — массив TournamentDto
 * @returns {Array} новый отсортированный массив
 */
export function sortTournaments(tournaments) {
    if (!Array.isArray(tournaments)) return [];

    return [...tournaments].sort((a, b) => {
        const aFinished = FINISHED_STATUSES.has(a.status) ? 1 : 0;
        const bFinished = FINISHED_STATUSES.has(b.status) ? 1 : 0;

        // Разные группы → завершённые в конец
        if (aFinished !== bFinished) return aFinished - bFinished;

        // Одна группа → сортировка по дате (новые первее)
        return compareDatesDesc(a.startDate, b.startDate);
    });
}

/**
 * Сравнивает две даты для сортировки по убыванию.
 * null / undefined считается самым старым значением.
 */
function compareDatesDesc(dateA, dateB) {
    if (!dateA && !dateB) return 0;
    if (!dateA) return 1;
    if (!dateB) return -1;
    return new Date(dateB) - new Date(dateA);
}
