// src/pages/tournaments/TournamentTable.jsx
import {
    Table, Paper, Text, Group, Avatar, SegmentedControl,
    Center, Loader, Alert, Stack, Box, Tabs
} from '@mantine/core';
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { tournamentsApi } from '../../api/tournaments';
import { useThemeColors } from '../../hooks/useThemeColors';
import { useAuth } from '../../context/AuthContext.jsx';

function extractArray(res) {
    if (!res) return [];
    if (Array.isArray(res)) return res;
    if (Array.isArray(res.content)) return res.content;
    return [];
}

function displayVal(val, shouldHide, decimals = 2) {
    if (shouldHide) return '—';
    return Number(val ?? 0).toFixed(decimals);
}

function displayInt(val, shouldHide) {
    if (shouldHide) return '—';
    return val ?? 0;
}

// ── Личный лидерборд ─────────────────────────────────────────────────────────
function IndividualTable({ data, sortBy, shouldHide, c }) {
    return (
        <Box style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
            <Table
                striped highlightOnHover horizontalSpacing="xs" verticalSpacing="xs"
                style={{ whiteSpace: 'nowrap', minWidth: 700 }}
                styles={{
                    thead: { backgroundColor: c.tableHeader },
                    tr: { '--table-highlight-color': c.tableHover },
                }}
            >
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>#</Table.Th>
                        <Table.Th>Игрок</Table.Th>
                        <Table.Th>{sortBy === 'avg' ? 'Ср. б' : '∑'}</Table.Th>
                        <Table.Th>∑дб</Table.Th>
                        <Table.Th title="Положительные допы">∑ +</Table.Th>
                        <Table.Th title="Отрицательные допы">∑ -</Table.Th>
                        <Table.Th title="ЛХ">∑ ЛХ</Table.Th>
                        <Table.Th title="Штрафы">Штрафы</Table.Th>
                        <Table.Th title="Компенсации">Ci</Table.Th>
                        <Table.Th title="Первое убийство">ПУ</Table.Th>
                        <Table.Th>П (Ш/Д)</Table.Th>
                        <Table.Th>И</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {data.length === 0 ? (
                        <Table.Tr>
                            <Table.Td colSpan={12}>
                                <Center p="md">
                                    <Text c="dimmed">
                                        {shouldHide ? 'Результаты скрыты организатором' : 'Нет данных'}
                                    </Text>
                                </Center>
                            </Table.Td>
                        </Table.Tr>
                    ) : (
                        data.map((row, index) => (
                            <Table.Tr key={row.userId ?? index}>
                                <Table.Td><Text fw={700}>{index + 1}</Text></Table.Td>
                                <Table.Td>
                                    <Group gap="sm" wrap="nowrap"
                                           component={row.userId ? Link : 'div'}
                                           to={row.userId ? `/players/${row.userId}` : undefined}
                                           style={{ textDecoration: 'none', color: 'inherit' }}>
                                        <Avatar src={row.avatarUrl} size="sm" radius="xl" color="brandRed">
                                            {row.nickname?.substring(0, 2).toUpperCase()}
                                        </Avatar>
                                        <Text size="sm" fw={500}>{row.nickname}</Text>
                                    </Group>
                                </Table.Td>
                                <Table.Td fw={700}>
                                    {sortBy === 'avg' && row.gamesCount > 0
                                        ? displayVal(row.totalScore / row.gamesCount, shouldHide)
                                        : displayVal(row.totalScore, shouldHide)}
                                </Table.Td>
                                <Table.Td c="violet">
                                    {displayVal(
                                        (Number(row.extraPointsPositive) || 0)
                                        - (Number(row.extraPointsNegative) || 0)
                                        + (Number(row.bestMovePoints) || 0),
                                        shouldHide
                                    )}
                                </Table.Td>
                                <Table.Td c="green">{displayVal(row.extraPointsPositive, shouldHide)}</Table.Td>
                                <Table.Td c="red">{displayVal(row.extraPointsNegative, shouldHide)}</Table.Td>
                                <Table.Td>{displayVal(row.bestMovePoints, shouldHide)}</Table.Td>
                                <Table.Td c="red">{displayVal(row.penaltyPoints, shouldHide)}</Table.Td>
                                <Table.Td>{displayVal(row.compensationPoints, shouldHide)}</Table.Td>
                                <Table.Td>{displayInt(row.firstKilledCount, shouldHide)}</Table.Td>
                                <Table.Td fw={700}>
                                    {displayInt(row.totalWins, shouldHide)}
                                    <Text span fw={400} c="dimmed" size="xs">
                                        {' '}({displayInt(row.sheriffWins, shouldHide)}/{displayInt(row.donWins, shouldHide)})
                                    </Text>
                                </Table.Td>
                                <Table.Td>{displayInt(row.gamesCount, shouldHide)}</Table.Td>
                            </Table.Tr>
                        ))
                    )}
                </Table.Tbody>
            </Table>
        </Box>
    );
}

// ── Командный лидерборд ───────────────────────────────────────────────────────
function TeamTable({ data, shouldHide, c }) {
    return (
        <Box style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
            <Table
                striped highlightOnHover horizontalSpacing="xs" verticalSpacing="xs"
                style={{ whiteSpace: 'nowrap', minWidth: 700 }}
                styles={{
                    thead: { backgroundColor: c.tableHeader },
                    tr: { '--table-highlight-color': c.tableHover },
                }}
            >
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>#</Table.Th>
                        <Table.Th>Команда</Table.Th>
                        <Table.Th>∑</Table.Th>
                        <Table.Th>∑дб</Table.Th>
                        <Table.Th title="Положительные допы">∑ +</Table.Th>
                        <Table.Th title="Отрицательные допы">∑ -</Table.Th>
                        <Table.Th title="ЛХ">∑ ЛХ</Table.Th>
                        <Table.Th title="Штрафы">Штрафы</Table.Th>
                        <Table.Th title="Компенсации">Ci</Table.Th>
                        <Table.Th title="Первое убийство">ПУ</Table.Th>
                        <Table.Th>П (Ш/Д)</Table.Th>
                        <Table.Th>И</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {data.length === 0 ? (
                        <Table.Tr>
                            <Table.Td colSpan={12}>
                                <Center p="md">
                                    <Text c="dimmed">
                                        {shouldHide ? 'Результаты скрыты организатором' : 'Нет данных'}
                                    </Text>
                                </Center>
                            </Table.Td>
                        </Table.Tr>
                    ) : (
                        data.map((row, index) => (
                            <Table.Tr key={row.teamId ?? index}>
                                <Table.Td><Text fw={700}>{index + 1}</Text></Table.Td>
                                <Table.Td>
                                    <Text size="sm" fw={700}>{row.teamName}</Text>
                                </Table.Td>
                                <Table.Td fw={700} c="violet">
                                    {displayVal(row.totalScore, shouldHide)}
                                </Table.Td>
                                <Table.Td c="violet">
                                    {displayVal(
                                        (Number(row.extraPointsPositive) || 0)
                                        - (Number(row.extraPointsNegative) || 0)
                                        + (Number(row.bestMovePoints) || 0),
                                        shouldHide
                                    )}
                                </Table.Td>
                                <Table.Td c="green">{displayVal(row.extraPointsPositive, shouldHide)}</Table.Td>
                                <Table.Td c="red">{displayVal(row.extraPointsNegative, shouldHide)}</Table.Td>
                                <Table.Td>{displayVal(row.bestMovePoints, shouldHide)}</Table.Td>
                                <Table.Td c="red">{displayVal(row.penaltyPoints, shouldHide)}</Table.Td>
                                <Table.Td>{displayVal(row.compensationPoints, shouldHide)}</Table.Td>
                                <Table.Td>{displayInt(row.firstKilledCount, shouldHide)}</Table.Td>
                                <Table.Td fw={700}>
                                    {displayInt(row.totalWins, shouldHide)}
                                    <Text span fw={400} c="dimmed" size="xs">
                                        {' '}({displayInt(row.sheriffWins, shouldHide)}/{displayInt(row.donWins, shouldHide)})
                                    </Text>
                                </Table.Td>
                                <Table.Td>{displayInt(row.gamesCount, shouldHide)}</Table.Td>
                            </Table.Tr>
                        ))
                    )}
                </Table.Tbody>
            </Table>
        </Box>
    );
}

// ── Главный компонент ────────────────────────────────────────────────────────
export default function TournamentTable({ tournament }) {
    const c = useThemeColors();
    const { user } = useAuth();

    const [individualData, setIndividualData] = useState([]);
    const [teamData, setTeamData]             = useState([]);
    const [loading, setLoading]               = useState(true);
    const [error, setError]                   = useState(null);
    const [tableView, setTableView]           = useState('individual'); // 'individual' | 'team'
    const [stage, setStage]                   = useState('qualifying');
    const [finalData, setFinalData]           = useState([]);
    const [sortBy, setSortBy]                 = useState('total');

    const isTeam       = tournament?.type === 'team';
    const hasFinals    = tournament?.settings?.finalRoundsCount > 0;
    const isOrganizer  = !!user && (user.isAdmin || user.id === tournament?.organizerId);
    const shouldHide   = (tournament?.settings?.areResultsHidden || false) && !isOrganizer;

    useEffect(() => {
        if (!tournament?.id) return;
        const fetchData = async () => {
            setLoading(true);
            setError(null);
            try {
                const promises = [
                    tournamentsApi.getLeaderboard(tournament.id, false, sortBy),
                    isTeam ? tournamentsApi.getTeamLeaderboard(tournament.id) : Promise.resolve([]),
                    hasFinals ? tournamentsApi.getLeaderboard(tournament.id, true, sortBy) : Promise.resolve([]),
                ];
                const [indRes, teamRes, finalRes] = await Promise.all(promises);
                setIndividualData(extractArray(indRes));
                setTeamData(extractArray(teamRes));
                setFinalData(extractArray(finalRes).slice(0, 10));
            } catch (err) {
                console.error('Ошибка загрузки лидерборда:', err);
                setError('Не удалось загрузить таблицу результатов');
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [tournament?.id, isTeam, sortBy, hasFinals]); // eslint-disable-line

    if (loading) return <Center py="xl"><Loader color="brandRed" /></Center>;
    if (error)   return <Alert color="red" mt="md">{error}</Alert>;

    const currentIndividualData = stage === 'finals' ? finalData : individualData;

    return (
        <Paper withBorder radius="md" p="md" style={{ backgroundColor: c.surface2 }}>
            <Stack gap="xs" mb="md">
                <Group justify="space-between" wrap="wrap" gap="xs">
                    <Group gap="xs" wrap="wrap">
                        {/* Переключение личная/командная — только для командного турнира */}
                        {isTeam && (
                            <SegmentedControl
                                value={tableView}
                                onChange={setTableView}
                                size="xs"
                                color="brandRed"
                                data={[
                                    { label: 'Личная', value: 'individual' },
                                    { label: 'Командная', value: 'team' },
                                ]}
                            />
                        )}
                        {/* Переключение отборочные/финал */}
                        {hasFinals && tableView === 'individual' && (
                            <SegmentedControl
                                value={stage}
                                onChange={setStage}
                                size="xs"
                                data={[
                                    { label: 'Отборочные', value: 'qualifying' },
                                    { label: 'Финал', value: 'finals' },
                                ]}
                            />
                        )}
                    </Group>
                    {/* Сортировка — только для личного */}
                    {tableView === 'individual' && (
                        <SegmentedControl
                            value={sortBy}
                            onChange={setSortBy}
                            size="xs"
                            data={[
                                { label: 'По сумме', value: 'total' },
                                { label: 'По среднему', value: 'avg' },
                            ]}
                        />
                    )}
                </Group>
            </Stack>

            {tableView === 'team' ? (
                <TeamTable data={teamData} shouldHide={shouldHide} c={c} />
            ) : (
                <IndividualTable
                    data={currentIndividualData}
                    sortBy={sortBy}
                    shouldHide={shouldHide}
                    c={c}
                />
            )}
        </Paper>
    );
}
