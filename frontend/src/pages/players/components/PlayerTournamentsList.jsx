// src/pages/players/components/PlayerTournamentsList.jsx
import { useState, useEffect } from 'react';
import {
    Stack, Paper, Text, Group, Badge, Center, Loader,
    ThemeIcon, Anchor
} from '@mantine/core';
import { Link } from 'react-router-dom';
import { IconTrophy, IconCalendar, IconMedal, IconUsers } from '@tabler/icons-react';
import { getPlayerTournaments } from '../../../api/playerTournaments';
import { formatTournamentDates } from '../../tournaments/utils/tournamentDateUtils';
import { useThemeColors } from '../../../hooks/useThemeColors';

// ── Константы ────────────────────────────────────────────────────────────────

const TYPE_CONFIG = {
    individual: { label: 'Личный',    color: 'blue'   },
    team:       { label: 'Командный', color: 'violet' },
    season:     { label: 'Рейтинг',   color: 'teal'   },
};

const STATUS_CONFIG = {
    registration: { label: 'Регистрация', color: 'green'  },
    active:       { label: 'Идёт',        color: 'yellow' },
    completed:    { label: 'Завершён',     color: 'gray'   },
    archived:     { label: 'Архив',        color: 'dark'   },
};

// ── Вспомогательный компонент: строка места ───────────────────────────────────

function RankBadge({ rank, label, color }) {
    if (rank == null) return null;
    return (
        <Badge
            size="sm"
            color={rank <= 3 ? 'orange' : color}
            variant={rank <= 3 ? 'filled' : 'light'}
            leftSection={<IconMedal size={10} />}
        >
            {label}: {rank} место
        </Badge>
    );
}

// ── Карточка одного турнира ───────────────────────────────────────────────────

function TournamentRow({ tour, c }) {
    const typeInfo   = TYPE_CONFIG[tour.type]      || { label: tour.type,   color: 'gray' };
    const statusInfo = STATUS_CONFIG[tour.status]  || { label: tour.status, color: 'gray' };
    const dateLabel  = formatTournamentDates(tour.startDate, tour.endDate);

    return (
        <Paper
            component={Link}
            to={`/tournaments/${tour.tournamentId}`}
            withBorder p="sm" radius="md"
            style={{
                textDecoration: 'none',
                color: 'inherit',
                transition: 'background-color 0.15s',
                backgroundColor: c.surface3,
            }}
            onMouseEnter={e => e.currentTarget.style.backgroundColor = c.surface4}
            onMouseLeave={e => e.currentTarget.style.backgroundColor = c.surface3}
        >
            <Group justify="space-between" wrap="nowrap" gap="sm">
                {/* Иконка типа */}
                <ThemeIcon
                    size="md" radius="md"
                    color={typeInfo.color}
                    variant="light"
                    style={{ flexShrink: 0 }}
                >
                    {tour.type === 'team' ? <IconUsers size={14} /> : <IconTrophy size={14} />}
                </ThemeIcon>

                {/* Название + дата */}
                <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
                    <Text size="sm" fw={600} truncate>{tour.title}</Text>
                    <Group gap={4} c="dimmed">
                        <IconCalendar size={12} />
                        <Text size="xs">{dateLabel}</Text>
                        {tour.clubName && (
                            <Text size="xs" c="dimmed">· {tour.clubName}</Text>
                        )}
                    </Group>
                </Stack>

                {/* Бейджи */}
                <Stack gap="xs" style={{ flexShrink: 0 }} wrap="nowrap" align="flex-end">
                    <Badge size="xs" color={typeInfo.color} variant="outline">
                        {typeInfo.label}
                    </Badge>
                    <Badge size="xs" color={statusInfo.color} variant="dot">
                        {statusInfo.label}
                    </Badge>
                    <RankBadge rank={tour.individualRank} label="Лич." color="blue" />
                    {tour.type === 'team' && (
                        <RankBadge rank={tour.teamRank} label="Ком." color="violet" />
                    )}
                </Stack>
            </Group>
        </Paper>
    );
}

// ── Главный компонент ─────────────────────────────────────────────────────────

export default function PlayerTournamentsList({ userId }) {
    const c = useThemeColors();
    const [tournaments, setTournaments] = useState([]);
    const [loading, setLoading]         = useState(true);
    const [error, setError]             = useState(null);

    useEffect(() => {
        if (!userId) return;
        setLoading(true);
        setError(null);

        getPlayerTournaments(userId)
            .then(setTournaments)
            .catch(err => {
                console.error('Ошибка загрузки турниров игрока', err);
                setError('Не удалось загрузить турниры');
            })
            .finally(() => setLoading(false));
    }, [userId]);

    if (loading) return <Center py="xl"><Loader color="brandRed" size="sm" /></Center>;

    if (error) return (
        <Text c="red" size="sm" ta="center" py="md">{error}</Text>
    );

    if (tournaments.length === 0) return (
        <Paper withBorder p="xl" ta="center" bg="transparent" style={{ borderStyle: 'dashed' }}>
            <Text c="dimmed" size="sm">Игрок ещё не участвовал в турнирах</Text>
        </Paper>
    );

    // Разделяем на активные и завершённые для визуального разделения
    const active    = tournaments.filter(t => t.status !== 'completed' && t.status !== 'archived');
    const finished  = tournaments.filter(t => t.status === 'completed' || t.status === 'archived');

    return (
        <Stack gap="xs" mx={{ base: '-20px', sm: 0 }}>
            {active.length > 0 && (
                <>
                    <Text size="xs" c="dimmed" tt="uppercase" fw={700} mt="xs">
                        Активные ({active.length})
                    </Text>
                    {active.map(t => (
                        <TournamentRow key={t.tournamentId} tour={t} c={c} />
                    ))}
                </>
            )}

            {finished.length > 0 && (
                <>
                    <Text size="xs" c="dimmed" tt="uppercase" fw={700} mt="md">
                        Завершённые ({finished.length})
                    </Text>
                    {finished.map(t => (
                        <TournamentRow key={t.tournamentId} tour={t} c={c} />
                    ))}
                </>
            )}
        </Stack>
    );
}
