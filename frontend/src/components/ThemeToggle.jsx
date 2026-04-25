// src/components/ThemeToggle.jsx
import { ActionIcon, Tooltip } from '@mantine/core';
import { IconSun, IconMoon } from '@tabler/icons-react';
import { useColorScheme } from '../hooks/useColorScheme';

/**
 * Кнопка переключения тёмной/светлой темы.
 * Используется в шапке приложения (MainLayout).
 */
export default function ThemeToggle() {
    const { isDark, toggle } = useColorScheme();

    return (
        <Tooltip
            label={isDark ? 'Светлая тема' : 'Тёмная тема'}
            withArrow
            position="bottom"
        >
            <ActionIcon
                onClick={toggle}
                variant="subtle"
                color={isDark ? 'yellow' : 'blue'}
                size="lg"
                radius="xl"
                aria-label="Переключить тему"
            >
                {isDark
                    ? <IconSun size={20} />
                    : <IconMoon size={20} />
                }
            </ActionIcon>
        </Tooltip>
    );
}
