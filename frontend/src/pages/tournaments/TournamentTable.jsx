import {
    Table, Paper, Text, Group, Avatar, SegmentedControl,
    Center, Loader, Alert, Stack, Box
} from '@mantine/core';
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { tournamentsApi } from '../../api/tournaments';
import { useThemeColors } from '../../hooks/useThemeColors';

// Вспомогательная функция для обработки ответа API
function extractArray(res) {
    if (!res) return [];
    if (Array.isArray(res)) return res;
    if (Array.isArray(res.content)) return res.content;
    return [];
}

export default function TournamentTable({ tournament }) {
    const c = useThemeColors();

    const [data, setData] = useState([]);           // Отборочные
    const [dataFinal, setDataFinal] = useState([]); // Финалы
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [stage, setStage] = useState('qualifying');
    const [sortBy, setSortBy] = useState('total');

    const isTeam = tournament?.type === 'team';
    const hasFinals = tournament?.settings?.finalRoundsCount > 0;
    const areResultsHidden = tournament?.settings?.areResultsHidden || false;

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            setError(null);
            try {
                if (!areResultsHidden && tournament?.id) {
                    if (isTeam) {
                        // Для командного зачета обычно только одна таблица
                        const res = await tournamentsApi.getTeamLeaderboard(tournament.id);
                        setData(extractArray(res));
                    } else {
                        // Загружаем отборочные и финалы параллельно
                        const [resQual, resFinal] = await Promise.all([
                            tournamentsApi.getLeaderboard(tournament.id, false, sortBy),
                            hasFinals
                                ? tournamentsApi.getLeaderboard(tournament.id, true, sortBy)
                                : Promise.resolve([])
                        ]);

                        setData(extractArray(resQual));
                        // Ограничиваем финал ТОП-10 согласно логике турнира
                        setDataFinal(extractArray(resFinal).slice(0, 10));
                    }
                }
            } catch (err) {
                console.error('Ошибка загрузки лидерборда:', err);
                setError('Не удалось загрузить таблицу результатов');
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [tournament?.id, isTeam, sortBy, hasFinals, areResultsHidden]);

    // Определяем, какой массив отображать в данный момент
    const currentData = (stage === 'finals' && !isTeam) ? dataFinal : data;

    const displayValue = val => areResultsHidden ? '0.00' : Number(val ?? 0).toFixed(2);
    const displayInt = val => areResultsHidden ? 0 : (val ?? 0);

    if (loading) return <Center py="xl"><Loader color="brandRed" /></Center>;
    if (error) return <Alert color="red" mt="md">{error}</Alert>;

    return (
        <Paper withBorder radius="md" p="md" style={{ backgroundColor: c.surface2 }}>
            {!isTeam && (
                <Stack gap="xs" mb="md">
                    <Group justify="space-between" wrap="wrap" gap="xs">
                        {hasFinals && (
                            <SegmentedControl
                                value={stage}
                                onChange={setStage}
                                size="xs"
                                color="brandRed"
                                data={[
                                    { label: 'Отборочные', value: 'qualifying' },
                                    { label: 'Финал', value: 'finals' },
                                ]}
                            />
                        )}
                        <SegmentedControl
                            value={sortBy}
                            onChange={setSortBy}
                            size="xs"
                            data={[
                                { label: 'По сумме', value: 'total' },
                                { label: 'По среднему', value: 'avg' },
                            ]}
                        />
                    </Group>
                </Stack>
            )}

            {isTeam && (
                <Text fw={700} mb="sm" c="dimmed">Командный зачет (Отборочные игры)</Text>
            ) /*: ()
                hasFinals && stage === 'finals' && (
                    <Text fw={700} mb="sm" c="brandRed">Финальный зачет (Топ 10 игроков)</Text>

            ))*/}

            <Box style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
                <Table
                    striped
                    highlightOnHover
                    horizontalSpacing="xs"
                    verticalSpacing="xs"
                    style={{ whiteSpace: 'nowrap', minWidth: isTeam ? 400 : 700 }}
                    styles={{
                        thead: { backgroundColor: c.tableHeader },
                        tr: { '--table-highlight-color': c.tableHover },
                    }}
                >
                    <Table.Thead>
                        {isTeam ? (
                            <Table.Tr>
                                <Table.Th>#</Table.Th>
                                <Table.Th>Команда</Table.Th>
                                <Table.Th>∑</Table.Th>
                                <Table.Th>Доп. баллы</Table.Th>
                                <Table.Th>Штрафы</Table.Th>
                                <Table.Th>П</Table.Th>
                                <Table.Th>И</Table.Th>
                            </Table.Tr>
                        ) : (
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
                        )}
                    </Table.Thead>

                    <Table.Tbody>
                        {currentData.length === 0 ? (
                            <Table.Tr>
                                <Table.Td colSpan={isTeam ? 7 : 12}>
                                    <Center p="md">
                                        <Text c="dimmed">
                                            {areResultsHidden
                                                ? 'Результаты скрыты организатором'
                                                : 'Нет данных для отображения'}
                                        </Text>
                                    </Center>
                                </Table.Td>
                            </Table.Tr>
                        ) : (
                            currentData.map((row, index) => (
                                <Table.Tr key={isTeam ? (row.teamId ?? index) : (row.userId ?? index)}>
                                    <Table.Td>
                                        <Text fw={700}>
                                            {index + 1}
                                        </Text>
                                    </Table.Td>

                                    {isTeam ? (
                                        <>
                                            <Table.Td><Text fw={700} size="sm">{row.teamName}</Text></Table.Td>
                                            <Table.Td fw={700} c="violet">{displayValue(row.totalScore)}</Table.Td>
                                            <Table.Td c="green">{displayValue(row.teamExtraPoints)}</Table.Td>
                                            <Table.Td c="red">{displayValue(row.teamPenalties)}</Table.Td>
                                            <Table.Td fw={700}>{displayInt(row.totalWins)}</Table.Td>
                                            <Table.Td>{displayInt(row.gamesCount)}</Table.Td>
                                        </>
                                    ) : (
                                        <>
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
                                                    ? displayValue(row.totalScore / row.gamesCount)
                                                    : displayValue(row.totalScore)}
                                            </Table.Td>
                                            <Table.Td c="violet">
                                                {displayValue(
                                                    (Number(row.extraPointsPositive) || 0)
                                                    - (Number(row.extraPointsNegative) || 0)
                                                    + (Number(row.bestMovePoints) || 0)
                                                )}
                                            </Table.Td>
                                            <Table.Td c="green">{displayValue(row.extraPointsPositive)}</Table.Td>
                                            <Table.Td c="red">{displayValue(row.extraPointsNegative)}</Table.Td>
                                            <Table.Td>{displayValue(row.bestMovePoints)}</Table.Td>
                                            <Table.Td c="red">{displayValue(row.penaltyPoints)}</Table.Td>
                                            <Table.Td>{displayValue(row.compensationPoints)}</Table.Td>
                                            <Table.Td>{displayInt(row.firstKilledCount)}</Table.Td>
                                            <Table.Td fw={700}>
                                                {displayInt(row.totalWins)}
                                                <Text span fw={400} c="dimmed" size="xs">
                                                    {' '}({displayInt(row.sheriffWins)}/{displayInt(row.donWins)})
                                                </Text>
                                            </Table.Td>
                                            <Table.Td>{displayInt(row.gamesCount)}</Table.Td>
                                        </>
                                    )}
                                </Table.Tr>
                            ))
                        )}
                    </Table.Tbody>
                </Table>
            </Box>
        </Paper>
    );
}