// src/pages/tournaments/utils/tournamentDateUtils.js
import dayjs from 'dayjs';
import 'dayjs/locale/ru';

/**
 * Форматирует диапазон дат турнира.
 * Если startDate === endDate (или только одна дата) — показывает один день.
 * Иначе — диапазон.
 *
 * @param {string|null} startDate
 * @param {string|null} endDate
 * @param {string} [locale='ru']
 * @returns {string}
 */
export function formatTournamentDates(startDate, endDate, locale = 'ru') {
    if (!startDate && !endDate) return 'Даты не указаны';

    const start = startDate ? dayjs(startDate).locale(locale) : null;
    const end   = endDate   ? dayjs(endDate).locale(locale)   : null;

    if (!start) return end.format('D MMMM YYYY');
    if (!end)   return start.format('D MMMM YYYY');

    // Один и тот же день
    if (start.isSame(end, 'day')) return start.format('D MMMM YYYY');

    // Одинаковый год — не повторяем год в начале
    if (start.year() === end.year()) {
        return `${start.format('D MMM')} — ${end.format('D MMM YYYY')}`;
    }

    return `${start.format('D MMM YYYY')} — ${end.format('D MMM YYYY')}`;
}

/**
 * Возвращает true, если startDate и endDate — один и тот же день.
 */
export function isSingleDay(startDate, endDate) {
    if (!startDate || !endDate) return false;
    return dayjs(startDate).isSame(dayjs(endDate), 'day');
}
