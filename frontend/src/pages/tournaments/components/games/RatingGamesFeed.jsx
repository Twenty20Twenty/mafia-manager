// src/pages/tournaments/components/games/RatingGamesFeed.jsx
import { Stack, Group, Button, Text, ThemeIcon, Divider, Paper } from '@mantine/core';
import { IconPlus, IconCalendar } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { useMemo } from 'react';
import dayjs from 'dayjs';
import 'dayjs/locale/ru';
import GameTableCard from './GameTableCard';
import { useThemeColors } from '../../../../hooks/useThemeColors';

function DayBlock({ dateKey, games, tournament, user, onRefresh }) {
    const c = useThemeColors();

    const dateTitle = dateKey === 'unknown'
        ? 'Дата не указана'
        : dayjs(dateKey).locale('ru').format('D MMMM, dddd');

    const sortedGames = [...games].sort((a, b) => (a.tableNumber ?? 0) - (b.tableNumber ?? 0));

    return (
        <Paper withBorder p="md" radius="md" style={{ backgroundColor: c.surface0 }}>
            <Group align="center" mb="md">
                <ThemeIcon variant="light" color="gray" radius="xl" size="sm">
                    <IconCalendar size={14} />
                </ThemeIcon>
                <Text fw={700} c="dimmed" tt="capitalize">{dateTitle}</Text>
                <Divider style={{ flex: 1 }} />
            </Group>

            <Group align="stretch" wrap="wrap" gap="md">
                {sortedGames.map(game => (
                    <GameTableCard
                        key={game.id}
                        game={game}
                        tournament={tournament}
                        user={user}
                        isRating={true}
                        onRefresh={onRefresh}
                    />
                ))}
            </Group>
        </Paper>
    );
}

export default function RatingGamesFeed({ tournament, canManage, user, games, onRefresh }) {
    const groupedGames = useMemo(() => {
        const groups = {};
        games.forEach(game => {
            const dateKey = game.date ? dayjs(game.date).format('YYYY-MM-DD') : 'unknown';
            if (!groups[dateKey]) groups[dateKey] = [];
            groups[dateKey].push(game);
        });
        return groups;
    }, [games]);

    const sortedDates = useMemo(() =>
        Object.keys(groupedGames).sort((a, b) => {
            if (a === 'unknown') return 1;
            if (b === 'unknown') return -1;
            return dayjs(b).diff(dayjs(a));
        }),
        [groupedGames]
    );

    return (
        <Stack gap="md">
            {canManage && (
                <Group justify="flex-end" mb="sm">
                    <Button
                        component={Link}
                        to={`/tournaments/${tournament.id}/games/new`}
                        leftSection={<IconPlus size={16} />}
                        color="green"
                    >
                        Добавить игру
                    </Button>
                </Group>
            )}

            {sortedDates.length === 0 ? (
                <Text c="dimmed" ta="center" py="xl">Игры еще не проводились</Text>
            ) : (
                sortedDates.map(dateKey => (
                    <DayBlock
                        key={dateKey}
                        dateKey={dateKey}
                        games={groupedGames[dateKey]}
                        tournament={tournament}
                        user={user}
                        onRefresh={onRefresh}
                    />
                ))
            )}
        </Stack>
    );
}
