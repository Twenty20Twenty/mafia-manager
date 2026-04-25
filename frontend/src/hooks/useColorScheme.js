// src/hooks/useColorScheme.js
import { useMantineColorScheme, useComputedColorScheme } from '@mantine/core';
import { useCallback } from 'react';

/**
 * Хук для управления цветовой схемой приложения.
 * Предоставляет простой интерфейс переключения тёмной/светлой темы.
 */
export function useColorScheme() {
    const { setColorScheme } = useMantineColorScheme();
    const computed = useComputedColorScheme('dark');

    const isDark = computed === 'dark';

    const toggle = useCallback(() => {
        setColorScheme(isDark ? 'light' : 'dark');
    }, [isDark, setColorScheme]);

    return { isDark, toggle, colorScheme: computed };
}
