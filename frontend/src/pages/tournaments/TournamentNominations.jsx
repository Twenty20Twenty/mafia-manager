// src/pages/tournaments/TournamentNominations.jsx
import { useEffect, useState } from 'react';
import { Grid, Card, Avatar, Text, Group, Badge, Loader, Center, Stack, Divider } from '@mantine/core';
import { tournamentsApi } from '../../api/tournaments';
import { useThemeColors } from '../../hooks/useThemeColors';

function NominationCard({ title, players, scoreKey, scoreLabel, color, c }) {
    if (!players || players.length === 0 || !players[0][scoreKey] || players[0][scoreKey] <= 0) return null;

    const [first, second, third] = players;

    return (
        <Grid.Col span={{ base: 12, sm: 6, md: 4 }}>
            <Card
                shadow="xs"
                padding="xs"
                radius="md"
                withBorder
                h="100%"
                style={{ backgroundColor: c.surface3, display: 'flex', flexDirection: 'column' }}
            >
                {/* Компактный заголовок */}
                <Group justify="space-between" mb="xs" wrap="nowrap">
                    <Badge size="sm" color={color} variant="light">{title}</Badge>
                    <Text size="10px" c="dimmed" ta="right">{scoreLabel}</Text>
                </Group>

                {/* Победитель */}
                <Group gap="sm" wrap="nowrap" mb={(second || third) ? 'xs' : 0}>
                    <Avatar src={first.avatarUrl} size={44} radius="xl" color={color}>
                        {first.nickname.substring(0, 2).toUpperCase()}
                    </Avatar>
                    <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
                        <Text fw={700} size="sm" truncate>{first.nickname}</Text>
                        <Text size="xs" fw={500}>
                            {Number(first[scoreKey]).toFixed(2)}
                        </Text>
                    </Stack>
                </Group>

                {(second || third) && (
                    <Stack gap={4} mt="auto" style={{ width: '100%' }}>
                        <Divider opacity={0.2} mb={2} />
                        {second && second[scoreKey] > 0 && (
                            <Group justify="space-between" wrap="nowrap">
                                <Text size="xs" c="dimmed" truncate style={{ maxWidth: '70%' }}>
                                    <Text span fw={500} mr={4}>2.</Text>
                                    {second.nickname}
                                </Text>
                                <Text size="xs" c="dimmed">{Number(second[scoreKey]).toFixed(2)}</Text>
                            </Group>
                        )}
                        {third && third[scoreKey] > 0 && (
                            <Group justify="space-between" wrap="nowrap">
                                <Text size="xs" c="dimmed" truncate style={{ maxWidth: '70%' }}>
                                    <Text span fw={500} mr={4}>3.</Text>
                                    {third.nickname}
                                </Text>
                                <Text size="xs" c="dimmed">{Number(third[scoreKey]).toFixed(2)}</Text>
                            </Group>
                        )}
                    </Stack>
                )}
            </Card>
        </Grid.Col>
    );
}

export default function TournamentNominations({ tournament, isRating }) {
    const [nominations, setNominations] = useState([]);
    const [loading, setLoading]         = useState(true);
    const c = useThemeColors();

    const areResultsHidden = tournament?.settings?.areResultsHidden || false;

    useEffect(() => {
        if (!tournament.id) return;
        const fetchNominations = async () => {
            try {
                if (!areResultsHidden) {
                    const data = await tournamentsApi.getNominations(tournament.id, isRating ? 'avg' : 'sum');
                    setNominations(data);
                }
            } catch (error) {
                console.error('Ошибка загрузки номинаций', error);
            } finally {
                setLoading(false);
            }
        };
        fetchNominations();
    }, [tournament.id]);

    if (loading) return <Center p="xl"><Loader color="brandRed" /></Center>;

    if (nominations.length === 0) {
        return <Center p="md">
            <Text c="dimmed">
                {areResultsHidden
                    ? 'Результаты скрыты организатором'
                    : 'Номинанты пока не определены (недостаточно игр)'}
            </Text>
        </Center>
    }

    const top3 = (key) =>
        [...nominations]
            .filter(p => p[key] != null && p[key] > 0)
            .sort((a, b) => b[key] - a[key])
            .slice(0, 3);

    const label = (base) => `${isRating ? 'Ср.' : 'Сум.'} ${base}`;

    return (
        <Grid align="stretch">
            <NominationCard c={c} title="MVP"           players={top3('mvpScore')}                scoreKey="mvpScore"                scoreLabel={label('доп. + ЛХ')} color="orange" />
            <NominationCard c={c} title="Лучший Красный" players={top3('nominationScoreCivilian')} scoreKey="nominationScoreCivilian" scoreLabel={label('доп. Мир')}     color="red"    />
            <NominationCard c={c} title="Лучший Шериф"   players={top3('nominationScoreSheriff')}  scoreKey="nominationScoreSheriff"  scoreLabel={label('доп. Шериф')}   color="yellow" />
            <NominationCard c={c} title="Лучший Черный"  players={top3('nominationScoreMafia')}    scoreKey="nominationScoreMafia"    scoreLabel={label('доп. Маф')}     color="black"   />
            <NominationCard c={c} title="Лучший Дон"     players={top3('nominationScoreDon')}      scoreKey="nominationScoreDon"      scoreLabel={label('доп. Дон')}     color="grape"  />
        </Grid>
    );
}