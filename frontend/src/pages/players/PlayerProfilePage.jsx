// src/pages/players/PlayerProfilePage.jsx
import { useParams, Link } from 'react-router-dom';
import { useState, useEffect, useMemo } from 'react';
import {
    Container, Paper, Avatar, Title, Text, Group, Grid,
    Stack, Badge, Button, Center, Loader, Select,
    RingProgress, Tooltip, Tabs, Box, Popover, Checkbox, ActionIcon
} from '@mantine/core';
import {
    IconMapPin, IconArrowLeft, IconDeviceGamepad2,
    IconTrophy, IconChartBar, IconSwords, IconFilter
} from '@tabler/icons-react';
import dayjs from 'dayjs';
import 'dayjs/locale/ru';
import api from '../../api/axios';
import { useThemeColors } from '../../hooks/useThemeColors';
import PlayerTournamentsList from './components/PlayerTournamentsList';

// ── Конфигурация ролей ────────────────────────────────────────────────────────

const ROLE_CONFIG = {
    civilian: { label: 'Мирный', color: 'red',    winsFor: 'red'   },
    sheriff:  { label: 'Шериф',  color: 'yellow', winsFor: 'red'   },
    mafia:    { label: 'Мафия',  color: 'blue',   winsFor: 'black' },
    don:      { label: 'Дон',    color: 'grape',  winsFor: 'black' },
};

const TOURNAMENT_TYPES = ['individual', 'team', 'season'];

const TYPE_CONFIG = {
    individual: { label: 'Личный',    color: 'blue'   },
    team:       { label: 'Командный', color: 'violet' },
    season:     { label: 'Рейтинг',   color: 'teal'   },
};

// ── Утилиты ───────────────────────────────────────────────────────────────────

function pct(wins, games) {
    if (!games) return 0;
    return Math.round((wins / games) * 100);
}

function buildPeriodOptions(statsList) {
    return statsList.map(s => ({
        value: s.periodYear === null ? 'all' : String(s.periodYear),
        label: s.periodYear === null ? 'За всё время' : String(s.periodYear),
    }));
}

function isValidId(id) {
    if (!id || id === 'null' || id === 'undefined') return false;
    const n = Number(id);
    return !isNaN(n) && n > 0;
}

/**
 * Агрегирует записи statsByType по выбранным типам и конкретному периоду.
 * Возвращает объект в том же формате что и PlayerStatsDto.
 */
function aggregateStatsByType(statsByType, selectedTypes, periodYear) {
    // Фильтруем по выбранным типам и нужному периоду
    const filtered = statsByType.filter(s => {
        const typeOk = selectedTypes.includes(s.tournamentType);
        const periodOk = periodYear === 'all'
            ? s.periodYear === null
            : String(s.periodYear) === periodYear;
        return typeOk && periodOk;
    });

    if (filtered.length === 0) return null;

    // Суммируем все поля
    const agg = {
        totalGames:        0,
        gamesCivilian:     0, gamesSheriff: 0, gamesMafia: 0, gamesDon: 0,
        winsCivilian:      0, winsSheriff:  0, winsMafia:  0, winsDon:  0,
        bestMovesTotal:    0, bestMovesPerfect: 0,
        firstKilledCount:  0,
        totalFouls:        0,
        periodYear:        periodYear === 'all' ? null : Number(periodYear),
    };

    filtered.forEach(s => {
        agg.totalGames       += s.totalGames       || 0;
        agg.gamesCivilian    += s.gamesCivilian    || 0;
        agg.gamesSheriff     += s.gamesSheriff     || 0;
        agg.gamesMafia       += s.gamesMafia       || 0;
        agg.gamesDon         += s.gamesDon         || 0;
        agg.winsCivilian     += s.winsCivilian     || 0;
        agg.winsSheriff      += s.winsSheriff      || 0;
        agg.winsMafia        += s.winsMafia        || 0;
        agg.winsDon          += s.winsDon          || 0;
        agg.bestMovesTotal   += s.bestMovesTotal   || 0;
        agg.bestMovesPerfect += s.bestMovesPerfect || 0;
        agg.firstKilledCount += s.firstKilledCount || 0;
        agg.totalFouls       += s.totalFouls       || 0;
    });

    return agg;
}

/**
 * Собирает список доступных периодов из statsByType с учётом выбранных типов.
 */
function buildPeriodOptionsFromByType(statsByType, selectedTypes) {
    const periods = new Set();
    statsByType
        .filter(s => selectedTypes.includes(s.tournamentType))
        .forEach(s => {
            periods.add(s.periodYear === null ? 'all' : String(s.periodYear));
        });

    const options = [];
    if (periods.has('all')) options.push({ value: 'all', label: 'За всё время' });

    const years = Array.from(periods)
        .filter(p => p !== 'all')
        .sort((a, b) => Number(b) - Number(a));
    years.forEach(y => options.push({ value: y, label: y }));

    return options;
}

// ── Главный компонент ─────────────────────────────────────────────────────────

export default function PlayerProfilePage() {
    const { id } = useParams();
    const c = useThemeColors();

    const [player, setPlayer]                       = useState(null);
    const [club, setClub]                           = useState(null);
    const [statsByType, setStatsByType]             = useState([]);
    const [playerTournaments, setPlayerTournaments] = useState([]);
    const [selectedPeriod, setSelectedPeriod]       = useState('all');
    const [loading, setLoading]                     = useState(true);
    const [notFound, setNotFound]                   = useState(false);
    const [activeTab, setActiveTab]                 = useState('stats');

    // Фильтр по типу турнира — все выбраны по умолчанию
    const [filterOpen, setFilterOpen]       = useState(false);
    const [selectedTypes, setSelectedTypes] = useState([...TOURNAMENT_TYPES]);

    useEffect(() => {
        if (!isValidId(id)) { setNotFound(true); setLoading(false); return; }

        const fetchAll = async () => {
            try {
                setLoading(true);
                setNotFound(false);
                const [playerRes, statsByTypeRes, toursRes] = await Promise.all([
                    api.get(`/users/${id}`),
                    api.get(`/users/${id}/stats-by-type`),
                    api.get(`/users/${id}/tournaments`),
                ]);
                setPlayer(playerRes.data);
                setStatsByType(statsByTypeRes.data || []);
                setPlayerTournaments(toursRes.data || []);

                if (playerRes.data.clubId) {
                    try {
                        const clubRes = await api.get(`/clubs/${playerRes.data.clubId}`);
                        setClub(clubRes.data);
                    } catch (err) {
                        console.error('Ошибка загрузки клуба', err);
                    }
                }
            } catch (err) {
                console.error('Ошибка загрузки профиля', err);
                setNotFound(true);
            } finally {
                setLoading(false);
            }
        };
        fetchAll();
    }, [id]);

    // Период сбрасываем при изменении типов, если текущий период недоступен
    const periodOptions = useMemo(
        () => buildPeriodOptionsFromByType(statsByType, selectedTypes),
        [statsByType, selectedTypes]
    );

    useEffect(() => {
        if (periodOptions.length > 0) {
            const exists = periodOptions.some(o => o.value === selectedPeriod);
            if (!exists) setSelectedPeriod(periodOptions[0].value);
        }
    }, [periodOptions]); // eslint-disable-line

    // Агрегированная статистика для текущего фильтра
    const currentStats = useMemo(
        () => aggregateStatsByType(statsByType, selectedTypes, selectedPeriod),
        [statsByType, selectedTypes, selectedPeriod]
    );

    const allSelected  = selectedTypes.length === TOURNAMENT_TYPES.length;
    const filterActive = !allSelected;

    const toggleType = (type) => {
        setSelectedTypes(prev => {
            if (prev.includes(type)) {
                if (prev.length === 1) return prev; // не снимаем последний
                return prev.filter(t => t !== type);
            }
            return [...prev, type];
        });
    };

    if (loading) return <Center py="xl" mt="xl"><Loader color="brandRed" size="lg" /></Center>;

    if (notFound || !player) {
        return (
            <Container py="xl">
                <Button variant="subtle" leftSection={<IconArrowLeft size={16} />}
                        component={Link} to="/players" mb="md" color="gray">
                    Назад к списку
                </Button>
                <Text c="red">Игрок не найден</Text>
            </Container>
        );
    }

    const regDate = player.createdAt
        ? dayjs(player.createdAt).locale('ru').format('MMMM YYYY')
        : null;

    return (
        <Container size="md" py="xl">
            <Button variant="subtle" leftSection={<IconArrowLeft size={16} />}
                    component={Link} to="/players" mb="md" color="gray">
                Назад к списку
            </Button>

            <Stack gap="xs" mx={{ base: "-20px", sm: 0 }}>
                {/* ── Шапка профиля ────────────────────────────────────────────── */}
                <Paper radius="md" p={{ base: 'md', sm: 'xl' }} withBorder mb="xl"
                       style={{ backgroundColor: c.surface2 }}>
                    <Group align="flex-start" wrap="nowrap">
                        <Avatar
                            src={player.avatarUrl}
                            w={100} h={100} radius="50%"
                            color="brandRed"
                            style={{ flexShrink: 0 }}
                        >
                            {player.nickname?.substring(0, 2).toUpperCase()}
                        </Avatar>
                        <Stack gap="xs" style={{ flex: 1, minWidth: 0 }}>
                            <Title order={2} size={{ base: 'h3', sm: 'h2' }}>{player.nickname}</Title>

                            {player.city && (
                                <Group gap={5} c="dimmed">
                                    <IconMapPin size={16} />
                                    <Text size="sm">{player.city}</Text>
                                </Group>
                            )}

                            {regDate && (
                                <Text size="xs" c="dimmed">В системе с {regDate}</Text>
                            )}

                            {club && (
                                <Badge
                                    component={Link} to={`/clubs/${club.id}`}
                                    color="blue" variant="light"
                                    style={{ cursor: 'pointer', width: 'fit-content' }}
                                >
                                    {club.name}
                                </Badge>
                            )}
                        </Stack>
                    </Group>
                </Paper>
            </Stack>

            {/* ── Табы ─────────────────────────────────────────────────────── */}
            <Tabs value={activeTab} onChange={setActiveTab} variant="outline" radius="md">
                <Tabs.List mb="xl">
                    <Tabs.Tab value="stats" leftSection={<IconChartBar size={16} />}>
                        <Box style={{ textAlign: 'left' }}>Статистика</Box>
                    </Tabs.Tab>
                    <Tabs.Tab value="tournaments" leftSection={<IconSwords size={16} />}>
                        <Box style={{ textAlign: 'left' }}>Турниры</Box>
                    </Tabs.Tab>
                </Tabs.List>

                {/* ── Вкладка: Статистика ───────────────────────────────────── */}
                <Tabs.Panel value="stats">
                    <Stack gap="xs" mx={{ base: "-20px", sm: 0 }}>
                        {/* Строка управления: фильтр типов + период */}
                        <Group mb="xl" justify="space-between" wrap="wrap" gap="xs" align="flex-end">

                            {/* Фильтр по типу турнира */}
                            <Popover
                                opened={filterOpen}
                                onChange={setFilterOpen}
                                position="bottom-start"
                                withArrow
                                shadow="md"
                            >
                                <Popover.Target>
                                    <Button
                                        variant={filterActive ? 'filled' : 'default'}
                                        color={filterActive ? 'brandRed' : 'gray'}
                                        leftSection={<IconFilter size={16} />}
                                        size="sm"
                                        onClick={() => setFilterOpen(o => !o)}
                                    >
                                        {filterActive
                                            ? selectedTypes.map(t => TYPE_CONFIG[t]?.label).join(', ')
                                            : 'Все типы турниров'}
                                    </Button>
                                </Popover.Target>
                                <Popover.Dropdown>
                                    <Stack gap="sm" p="xs" style={{ minWidth: 200 }}>
                                        <Text size="xs" c="dimmed" fw={600} tt="uppercase">
                                            Типы турниров
                                        </Text>
                                        {TOURNAMENT_TYPES.map(type => {
                                            const cfg   = TYPE_CONFIG[type];
                                            const count = statsByType.filter(s => s.tournamentType === type && s.periodYear === null).reduce((a, s) => a + (s.totalGames || 0), 0);
                                            return (
                                                <Checkbox
                                                    key={type}
                                                    label={
                                                        <Group gap="xs">
                                                            <Badge size="xs" color={cfg.color} variant="light">
                                                                {cfg.label}
                                                            </Badge>
                                                            <Text size="xs" c="dimmed">{count} игр</Text>
                                                        </Group>
                                                    }
                                                    checked={selectedTypes.includes(type)}
                                                    onChange={() => toggleType(type)}
                                                    color={cfg.color}
                                                />
                                            );
                                        })}
                                        {!allSelected && (
                                            <Button
                                                size="xs" variant="subtle" color="gray"
                                                onClick={() => setSelectedTypes([...TOURNAMENT_TYPES])}
                                            >
                                                Сбросить фильтр
                                            </Button>
                                        )}
                                    </Stack>
                                </Popover.Dropdown>
                            </Popover>

                            {/* Период */}
                            {periodOptions.length > 1 ? (
                                <Select
                                    label="Период"
                                    data={periodOptions}
                                    value={selectedPeriod}
                                    onChange={v => setSelectedPeriod(v ?? 'all')}
                                    style={{ width: 180 }}
                                    size="sm"
                                />
                            ) : <Box />}
                        </Group>

                        {/* Активные бейджи фильтра */}
                        {filterActive && (
                            <Group gap="xs" mb="md">
                                <Text size="xs" c="dimmed">Показано:</Text>
                                {selectedTypes.map(t => (
                                    <Badge
                                        key={t}
                                        size="sm"
                                        color={TYPE_CONFIG[t]?.color}
                                        variant="light"
                                        rightSection={
                                            selectedTypes.length > 1 ? (
                                                <ActionIcon
                                                    size={10}
                                                    variant="transparent"
                                                    color={TYPE_CONFIG[t]?.color}
                                                    onClick={() => toggleType(t)}
                                                >
                                                    ×
                                                </ActionIcon>
                                            ) : null
                                        }
                                    >
                                        {TYPE_CONFIG[t]?.label}
                                    </Badge>
                                ))}
                            </Group>
                        )}

                        {currentStats ? (
                            <StatsSection stats={currentStats} c={c} />
                        ) : (
                            <Paper withBorder p="xl" ta="center" bg="transparent"
                                   style={{ borderStyle: 'dashed' }}>
                                <Text c="dimmed">Нет данных за выбранный период и тип турнира</Text>
                            </Paper>
                        )}
                    </Stack>
                </Tabs.Panel>

                {/* ── Вкладка: Турниры ──────────────────────────────────────── */}
                <Tabs.Panel value="tournaments">
                    <PlayerTournamentsList userId={Number(id)} />
                </Tabs.Panel>
            </Tabs>
        </Container>
    );
}

// ── StatsSection ─────────────────────────────────────────────────────────────

function StatsSection({ stats, c }) {
    const totalWins = (stats.winsCivilian || 0) + (stats.winsSheriff || 0)
        + (stats.winsMafia    || 0) + (stats.winsDon    || 0);

    return (
        <Stack gap="xl">
            <section>
                <Title order={4} mb="md">Общая статистика</Title>
                <Grid>
                    <Grid.Col span={{ base: 6, sm: 3 }}>
                        <StatCard
                            c={c} color="brandRed"
                            value={String(stats.totalGames)}
                            label="Всего игр"
                            icon={<IconDeviceGamepad2 size={16} />}
                        />
                    </Grid.Col>
                    <Grid.Col span={{ base: 6, sm: 3 }}>
                        <StatCard
                            c={c} color="green"
                            value={`${pct(totalWins, stats.totalGames)}%`}
                            subValue={`${totalWins} побед`}
                            label="Общий Win Rate"
                        />
                    </Grid.Col>
                    <Grid.Col span={{ base: 6, sm: 3 }}>
                        <StatCard
                            c={c} color="yellow"
                            value={String(stats.bestMovesTotal ?? 0)}
                            subValue={stats.bestMovesTotal > 0
                                ? `Идеальных: ${stats.bestMovesPerfect ?? 0}` : '—'}
                            label="Лучших ходов"
                            icon={<IconTrophy size={16} />}
                        />
                    </Grid.Col>
                    <Grid.Col span={{ base: 6, sm: 3 }}>
                        <StatCard
                            c={c} color="red"
                            value={String(stats.firstKilledCount ?? 0)}
                            label="Первых убитых"
                        />
                    </Grid.Col>
                </Grid>
            </section>

            <section>
                <Title order={4} mb="md">Win Rate по ролям</Title>
                <Grid>
                    {Object.entries(ROLE_CONFIG).map(([role, cfg]) => {
                        const cap     = role.charAt(0).toUpperCase() + role.slice(1);
                        const games   = stats[`games${cap}`] ?? 0;
                        const wins    = stats[`wins${cap}`]  ?? 0;
                        const winRate = pct(wins, games);
                        return (
                            <Grid.Col key={role} span={{ base: 6, sm: 3 }}>
                                <WinRateCard
                                    c={c}
                                    color={cfg.color}
                                    winRate={winRate}
                                    wins={wins}
                                    games={games}
                                    label={cfg.label}
                                />
                            </Grid.Col>
                        );
                    })}
                </Grid>
            </section>
        </Stack>
    );
}

// ── Карточка статистики ───────────────────────────────────────────────────────

function StatCard({ c, color, value, subValue, label }) {
    return (
        <Paper
            p="md" radius="md" withBorder h="100%"
            style={{
                borderLeft: `4px solid var(--mantine-color-${color}-filled)`,
                backgroundColor: c.surface3,
            }}
        >
            <Text size="xl" fw={700} c={`${color}.3`}>{value}</Text>
            {subValue && <Text size="xs" c="dimmed">{subValue}</Text>}
            <Text size="xs" c="dimmed" mt={4} fw={500}>{label}</Text>
        </Paper>
    );
}

// ── Карточка win rate ─────────────────────────────────────────────────────────

function WinRateCard({ c, color, winRate, wins, games, label }) {
    return (
        <Paper
            p="md" radius="md" withBorder h="100%"
            style={{
                display: 'flex', flexDirection: 'column',
                alignItems: 'center', gap: 8,
                backgroundColor: c.surface3,
            }}
        >
            <Tooltip label={`${wins} побед из ${games} игр`} withArrow>
                <div>
                    <RingProgress
                        size={72} thickness={6} roundCaps
                        sections={[{ value: winRate, color }]}
                        label={
                            <Text ta="center" fw={700} size="sm">{winRate}%</Text>
                        }
                    />
                </div>
            </Tooltip>
            <Text size="sm" c="dimmed" ta="center">{label}</Text>
            <Text size="xs" c="dimmed">{wins} из {games} игр</Text>
        </Paper>
    );
}