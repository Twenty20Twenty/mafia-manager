// src/pages/players/components/PlayerTournamentsList.jsx
import { useState, useEffect, useMemo } from 'react';
import {
    Stack, Paper, Text, Group, Badge, Center, Loader,
    ThemeIcon, Pagination
} from '@mantine/core';
import { Link } from 'react-router-dom';
import { IconTrophy, IconCalendar, IconMedal, IconUsers } from '@tabler/icons-react';
import { getPlayerTournaments } from '../../../api/playerTournaments';
import { formatTournamentDates } from '../../tournaments/utils/tournamentDateUtils';
import { useThemeColors } from '../../../hooks/useThemeColors';

// ── Константы ────────────────────────────────────────────────────────────────

const PAGE_SIZE = 8;

const FINISHED_STATUSES = new Set(['completed', 'archived']);

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

// ── Утилита сортировки ───────────────────────────────────────────────────────

function sortTournaments(tours) {
    return [...tours].sort((a, b) => {
        const aFinished = FINISHED_STATUSES.has(a.status) ? 1 : 0;
        const bFinished = FINISHED_STATUSES.has(b.status) ? 1 : 0;

        // незавершённые выше завершённых
        if (aFinished !== bFinished) return aFinished - bFinished;

        // внутри группы — новые первее (по startDate убывающая)
        const dateA = a.startDate ? new Date(a.startDate) : null;
        const dateB = b.startDate ? new Date(b.startDate) : null;
        if (!dateA && !dateB) return 0;
        if (!dateA) return 1;
        if (!dateB) return -1;
        return dateB - dateA;
    });
}

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
    const [activePage, setActivePage]   = useState(1);

    useEffect(() => {
        if (!userId) return;
        setLoading(true);
        setError(null);
        setActivePage(1);

        getPlayerTournaments(userId)
            .then(data => setTournaments(sortTournaments(data)))
            .catch(err => {
                console.error('Ошибка загрузки турниров игрока', err);
                setError('Не удалось загрузить турниры');
            })
            .finally(() => setLoading(false));
    }, [userId]);

    const totalPages = Math.ceil(tournaments.length / PAGE_SIZE);
    const paginated  = useMemo(
        () => tournaments.slice((activePage - 1) * PAGE_SIZE, activePage * PAGE_SIZE),
        [tournaments, activePage]
    );

    if (loading) return <Center py="xl"><Loader color="brandRed" size="sm" /></Center>;

    if (error) return (
        <Text c="red" size="sm" ta="center" py="md">{error}</Text>
    );

    if (tournaments.length === 0) return (
        <Paper withBorder p="xl" ta="center" bg="transparent" style={{ borderStyle: 'dashed' }}>
            <Text c="dimmed" size="sm">Игрок ещё не участвовал в турнирах</Text>
        </Paper>
    );

    return (
        <Stack gap="xs" mx={{ base: '-20px', sm: 0 }}>
            {paginated.map(t => (
                <TournamentRow key={t.tournamentId} tour={t} c={c} />
            ))}

            {totalPages > 1 && (
                <Center mt="sm">
                    <Pagination
                        total={totalPages}
                        value={activePage}
                        onChange={setActivePage}
                        color="brandRed"
                        size={{ base: 'sm', sm: 'md' }}
                    />
                </Center>
            )}
        </Stack>
    );
}