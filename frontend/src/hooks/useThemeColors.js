// src/hooks/useThemeColors.js
/**
 * Хук возвращает семантические цвета, которые корректно переключаются
 * между тёмной и светлой темами.
 *
 * Использование:
 *   const c = useThemeColors();
 *   <Paper bg={c.surface2}>...</Paper>
 *   <Table.Thead bg={c.tableHeader}>...</Table.Thead>
 */
import { useComputedColorScheme } from '@mantine/core';

export function useThemeColors() {
    const scheme = useComputedColorScheme('dark');
    const isDark = scheme === 'dark';

    return {
        isDark,

        // Фоны
        appBg:    isDark ? '#1a1b1e' : '#f0f2f5',
        surface0: isDark ? '#141517' : '#e8eaed',  // глубокий фон
        surface1: isDark ? '#1a1b1e' : '#f0f2f5',  // основной фон
        surface2: isDark ? '#25262b' : '#ffffff',   // карточки, Paper
        surface3: isDark ? '#2c2e33' : '#f4f5f7',  // шапки таблиц
        surface4: isDark ? '#373a40' : '#ebedf0',  // активные строки

        // Текст
        textPrimary:   isDark ? '#C1C2C5' : '#1c1e26',
        textSecondary: isDark ? '#909296' : '#4a5568',
        textDimmed:    isDark ? '#5c5f66' : '#718096',
        textBright:    isDark ? '#ffffff' : '#0d0f14',

        // Границы
        border:       isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.15)',
        borderStrong: isDark ? 'rgba(255,255,255,0.14)' : 'rgba(0,0,0,0.15)',

        // Таблицы
        tableHeader: isDark ? '#25262b' : '#f4f5f7',
        tableHover:  isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.04)',
        tableStripe: isDark ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)',

        // Специальные
        slotFkBg:      isDark ? 'rgba(34,139,230,0.15)' : 'rgba(34,139,230,0.10)',
        winnerRedBg:   isDark ? '#7b1e1e' : '#f8caca',
        winnerBlackBg: isDark ? '#1a1a1a' : '#9e9d9d',
        winnerDrawBg:  isDark ? '#25262b' : '#f4f5f7',
    };
}

/**
 * Возвращает bg-строку для bg prop Mantine Paper/Table в зависимости от темы.
 * Используется там, где хук не нужен — только цвет.
 */
export function getThemeSurface(isDark, level = 2) {
    const map = {
        dark:  { 0: '#141517', 1: '#1a1b1e', 2: '#25262b', 3: '#2c2e33', 4: '#373a40' },
        light: { 0: '#e8eaed', 1: '#f0f2f5', 2: '#ffffff',  3: '#f4f5f7', 4: '#ebedf0' },
    };
    return map[isDark ? 'dark' : 'light'][level] ?? map[isDark ? 'dark' : 'light'][2];
}
