// src/pages/tournaments/TournamentNominations.jsx
import { useEffect, useState } from 'react';
import { Grid, Card, Avatar, Text, Group, Badge, Loader, Center, Stack, Divider } from '@mantine/core';
import { tournamentsApi } from '../../api/tournaments';
import { useThemeColors } from '../../hooks/useThemeColors';

function NominationCard({ title, players, scoreKey, scoreLabel, color, c }) {
    if (!players || players.length === 0 || !players[0][scoreKey] || players[0][scoreKey] <= 0) return null;

    const [first, second, third] = players;

    return (
        <Grid.Col span={{ base: 12, sm: 6, md: 3 }}>
            <Card shadow="sm" padding="lg" radius="md" withBorder style={{ backgroundColor: c.surface3 }}>
                <Center mb="md">
                    <Badge size="lg" color={color} variant="light">{title}</Badge>
                </Center>
                <Group justify="center" gap="sm" style={{ display: 'flex', flexDirection: 'column' }}>
                    <Avatar src={first.avatarUrl} size={80} radius={80} color={color}>
                        {first.nickname.substring(0, 2).toUpperCase()}
                    </Avatar>
                    <Text fw={700} size="lg" ta="center">{first.nickname}</Text>
                    <Text size="sm" c="dimmed">
                        {scoreLabel}: <Text span fw={700}>{Number(first[scoreKey]).toFixed(2)}</Text>
                    </Text>
                </Group>

                {(second || third) && (
                    <>
                        <Divider my="sm" opacity={0.3} />
                        <Stack gap={6}>
                            {second && second[scoreKey] > 0 && (
                                <Group justify="space-between" px={4}>
                                    <Group gap={6}>
                                        <Text size="xs" c="dimmed" w={14} ta="center">2</Text>
                                        <Text size="xs" c="dimmed">{second.nickname}</Text>
                                    </Group>
                                    <Text size="xs" c="dimmed">{Number(second[scoreKey]).toFixed(2)}</Text>
                                </Group>
                            )}
                            {third && third[scoreKey] > 0 && (
                                <Group justify="space-between" px={4}>
                                    <Group gap={6}>
                                        <Text size="xs" c="dimmed" w={14} ta="center">3</Text>
                                        <Text size="xs" c="dimmed">{third.nickname}</Text>
                                    </Group>
                                    <Text size="xs" c="dimmed">{Number(third[scoreKey]).toFixed(2)}</Text>
                                </Group>
                            )}
                        </Stack>
                    </>
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
        <Grid>
            <NominationCard c={c} title="MVP Турнира"    players={top3('mvpScore')}                scoreKey="mvpScore"                scoreLabel={label('доп. + ЛХ')} color="orange" />
            <NominationCard c={c} title="Лучший Красный" players={top3('nominationScoreCivilian')} scoreKey="nominationScoreCivilian" scoreLabel={label('доп (Мир)')}  color="red"    />
            <NominationCard c={c} title="Лучший Шериф"   players={top3('nominationScoreSheriff')}  scoreKey="nominationScoreSheriff"  scoreLabel={label('доп (Ш)')}    color="yellow" />
            <NominationCard c={c} title="Лучший Черный"  players={top3('nominationScoreMafia')}    scoreKey="nominationScoreMafia"    scoreLabel={label('доп (Маф)')}  color="dark"   />
            <NominationCard c={c} title="Лучший Дон"     players={top3('nominationScoreDon')}      scoreKey="nominationScoreDon"      scoreLabel={label('доп (Д)')}    color="grape"  />
        </Grid>
    );
}
