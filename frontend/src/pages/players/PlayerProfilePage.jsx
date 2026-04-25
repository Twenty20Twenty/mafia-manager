// src/pages/players/PlayerProfilePage.jsx
import { useParams, Link } from 'react-router-dom';
import { useState, useEffect, useMemo } from 'react';
import {
    Container, Paper, Avatar, Title, Text, Group, Grid,
    Stack, Badge, Button, Center, Loader, Select,
    RingProgress, Tooltip
} from '@mantine/core';
import { IconMapPin, IconArrowLeft, IconDeviceGamepad2, IconTrophy } from '@tabler/icons-react';
import dayjs from 'dayjs';
import 'dayjs/locale/ru';
import api from '../../api/axios';
import { useThemeColors } from '../../hooks/useThemeColors';

const ROLE_CONFIG = {
    civilian: { label: 'Мирный', color: 'red',    winsFor: 'red'   },
    sheriff:  { label: 'Шериф',  color: 'yellow', winsFor: 'red'   },
    mafia:    { label: 'Мафия',  color: 'blue',   winsFor: 'black' },
    don:      { label: 'Дон',    color: 'grape',  winsFor: 'black' },
};

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

export default function PlayerProfilePage() {
    const { id } = useParams();
    const c = useThemeColors();

    const [player, setPlayer]               = useState(null);
    const [club, setClub]                   = useState(null);
    const [statsList, setStatsList]         = useState([]);
    const [selectedPeriod, setSelectedPeriod] = useState('all');
    const [loading, setLoading]             = useState(true);
    const [notFound, setNotFound]           = useState(false);

    useEffect(() => {
        if (!isValidId(id)) { setNotFound(true); setLoading(false); return; }

        const fetchAll = async () => {
            try {
                setLoading(true);
                setNotFound(false);
                const [playerRes, statsRes] = await Promise.all([
                    api.get(`/users/${id}`),
                    api.get(`/users/${id}/stats`),
                ]);
                setPlayer(playerRes.data);
                setStatsList(statsRes.data);
                if (playerRes.data.clubId) {
                    try {
                        const clubRes = await api.get(`/clubs/${playerRes.data.clubId}`);
                        setClub(clubRes.data);
                    } catch (err) { console.error('Ошибка загрузки клуба', err); }
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

    const currentStats  = useMemo(() =>
        statsList.find(s => selectedPeriod === 'all' ? s.periodYear === null : String(s.periodYear) === selectedPeriod) || null,
        [statsList, selectedPeriod]
    );
    const periodOptions = useMemo(() => buildPeriodOptions(statsList), [statsList]);

    if (loading) return <Center py="xl" mt="xl"><Loader color="brandRed" size="lg" /></Center>;

    if (notFound || !player) {
        return (
            <Container py="xl">
                <Button variant="subtle" leftSection={<IconArrowLeft size={16} />} component={Link} to="/players" mb="md" color="gray">
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
            <Button variant="subtle" leftSection={<IconArrowLeft size={16} />} component={Link} to="/players" mb="md" color="gray">
                Назад к списку
            </Button>

            {/* Шапка профиля */}
            <Paper radius="md" p={{ base: 'md', sm: 'xl' }} withBorder mb="xl"
                style={{ backgroundColor: c.surface2 }}>
                <Group align="flex-start" wrap="nowrap">
                    <Avatar src={player.avatarUrl} size={{ base: 72, sm: 120 }} radius={120} color="brandRed" style={{ flexShrink: 0 }}>
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

                        {regDate && <Text size="xs" c="dimmed">В системе с {regDate}</Text>}

                        {club && (
                            <Badge component={Link} to={`/clubs/${club.id}`} color="blue" variant="light"
                                style={{ cursor: 'pointer', width: 'fit-content' }}>
                                {club.name}
                            </Badge>
                        )}
                    </Stack>
                </Group>
            </Paper>

            {periodOptions.length > 1 && (
                <Select
                    label="Период статистики"
                    data={periodOptions}
                    value={selectedPeriod}
                    onChange={v => setSelectedPeriod(v ?? 'all')}
                    mb="xl"
                    style={{ maxWidth: 200 }}
                />
            )}

            {currentStats ? (
                <StatsSection stats={currentStats} c={c} />
            ) : (
                <Paper withBorder p="xl" ta="center" bg="transparent" style={{ borderStyle: 'dashed' }}>
                    <Text c="dimmed">Нет данных за выбранный период</Text>
                </Paper>
            )}
        </Container>
    );
}

function StatsSection({ stats, c }) {
    return (
        <Stack gap="xl">
            <section>
                <Title order={4} mb="md">Общая статистика</Title>
                <Grid>
                    <Grid.Col span={{ base: 6, sm: 3 }}>
                        <StatCard c={c} color="brandRed" value={String(stats.totalGames)} label="Всего игр" icon={<IconDeviceGamepad2 size={16} />} />
                    </Grid.Col>
                    <Grid.Col span={{ base: 6, sm: 3 }}>
                        <StatCard c={c}
                            color="green"
                            value={`${pct((stats.winsDon + stats.winsSheriff + stats.winsMafia + stats.winsCivilian), stats.totalGames)}%`}
                            subValue={`${(stats.winsDon + stats.winsSheriff + stats.winsMafia + stats.winsCivilian)} побед`}
                            label="Общий Win Rate"
                        />
                    </Grid.Col>
                    <Grid.Col span={{ base: 6, sm: 3 }}>
                        <StatCard c={c}
                            color="yellow"
                            value={String(stats.bestMovesTotal ?? 0)}
                            subValue={stats.bestMovesTotal > 0 ? `Идеальных: ${stats.bestMovesPerfect ?? 0}` : '—'}
                            label="Лучших ходов"
                            icon={<IconTrophy size={16} />}
                        />
                    </Grid.Col>
                </Grid>
            </section>

            <section>
                <Title order={4} mb="md">Win Rate по ролям</Title>
                <Grid>
                    {Object.entries(ROLE_CONFIG).map(([role, cfg]) => {
                        const games   = stats[`games${role.charAt(0).toUpperCase() + role.slice(1)}`] ?? 0;
                        const wins    = stats[`wins${role.charAt(0).toUpperCase() + role.slice(1)}`]  ?? 0;
                        const winRate = pct(wins, games);
                        return (
                            <Grid.Col key={role} span={{ base: 6, sm: 3 }}>
                                <WinRateCard c={c} color={cfg.color} winRate={winRate} wins={wins} games={games} label={cfg.label} isMain={role === 'civilian'} />
                            </Grid.Col>
                        );
                    })}
                </Grid>
            </section>
        </Stack>
    );
}

function StatCard({ c, color, value, subValue, label }) {
    return (
        <Paper p="md" radius="md" withBorder h="100%"
            style={{
                borderLeft: `4px solid var(--mantine-color-${color}-filled)`,
                backgroundColor: c.surface3,
            }}>
            <Text size="xl" fw={700} c={`${color}.3`}>{value}</Text>
            {subValue && <Text size="xs" c="dimmed">{subValue}</Text>}
            <Text size="xs" c="dimmed" mt={4} fw={500}>{label}</Text>
        </Paper>
    );
}

function WinRateCard({ c, color, winRate, wins, games, label, isMain }) {
    return (
        <Paper p="md" radius="md" withBorder h="100%"
            style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8, backgroundColor: c.surface3 }}>
            <Tooltip label={`${wins} побед из ${games} игр`} withArrow>
                <div>
                    <RingProgress
                        size={isMain ? 90 : 72} thickness={isMain ? 8 : 6}
                        roundCaps sections={[{ value: winRate, color }]}
                        label={<Text ta="center" fw={700} size={isMain ? 'md' : 'sm'}>{winRate}%</Text>}
                    />
                </div>
            </Tooltip>
            <Text size="sm" c="dimmed" ta="center">{label}</Text>
            <Text size="xs" c="dimmed">{wins} из {games} игр</Text>
        </Paper>
    );
}
