// src/pages/players/PlayersPage.jsx
import { useState, useEffect } from 'react';
import {
    Title, TextInput, SimpleGrid, Card, Text, Group, Avatar,
    Badge, Container, Pagination, Center, Loader, Stack
} from '@mantine/core';
import { IconSearch, IconMapPin } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import api from '../../api/axios';
import { useThemeColors } from '../../hooks/useThemeColors';

export default function PlayersPage() {
    const [search, setSearch]         = useState('');
    const [players, setPlayers]       = useState([]);
    const [loading, setLoading]       = useState(true);
    const [activePage, setPage]       = useState(1);
    const [totalPages, setTotalPages] = useState(1);

    useEffect(() => {
        const fetchPlayers = async () => {
            setLoading(true);
            try {
                const res = await api.get('/users', { params: { search, page: activePage - 1, size: 12 } });
                setPlayers(res.data.content);
                setTotalPages(res.data.totalPages);
            } catch (error) {
                console.error('Ошибка загрузки игроков', error);
            } finally {
                setLoading(false);
            }
        };
        const timer = setTimeout(fetchPlayers, 300);
        return () => clearTimeout(timer);
    }, [search, activePage]);

    return (
        <Container size="xl" py="xl">
            <Group justify="space-between" mb="xl" visibleFrom="sm">
                <Title order={2}>Игроки</Title>
                <TextInput
                    placeholder="Поиск по нику..."
                    leftSection={<IconSearch size={16} />}
                    value={search}
                    onChange={(e) => { setSearch(e.currentTarget.value); setPage(1); }}
                    w={300}
                />
            </Group>

            <Stack gap="sm" mb="xl" hiddenFrom="sm">
                <Title order={2}>Игроки</Title>
                <TextInput
                    placeholder="Поиск по нику..."
                    leftSection={<IconSearch size={16} />}
                    value={search}
                    onChange={(e) => { setSearch(e.currentTarget.value); setPage(1); }}
                />
            </Stack>

            {loading ? (
                <Center mt="xl"><Loader color="brandRed" /></Center>
            ) : players.length === 0 ? (
                <Text c="dimmed" ta="center" mt="xl">Игроки не найдены</Text>
            ) : (
                <>
                    <SimpleGrid cols={{ base: 2, sm: 2, md: 3, lg: 4 }} spacing="md">
                        {players.map((player) => (
                            <PlayerCard key={player.id} player={player} />
                        ))}
                    </SimpleGrid>

                    {totalPages > 1 && (
                        <Center mt="xl">
                            <Pagination total={totalPages} value={activePage} onChange={setPage} color="brandRed" size={{ base: 'sm', sm: 'md' }} />
                        </Center>
                    )}
                </>
            )}
        </Container>
    );
}

function PlayerCard({ player }) {
    const c = useThemeColors();

    return (
        <Card
            shadow="sm" padding={{ base: 'sm', sm: 'lg' }} radius="md" withBorder
            component={Link} to={`/players/${player.id}`}
            style={{
                cursor: 'pointer',
                transition: 'transform 0.2s, background-color 0.15s',
                textDecoration: 'none',
                color: 'inherit',
                backgroundColor: c.surface2,
            }}
            onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'scale(1.02)';
                e.currentTarget.style.backgroundColor = c.surface3;
            }}
            onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
                e.currentTarget.style.backgroundColor = c.surface2;
            }}
        >
            <Group justify="center" mt="xs" mb="xs">
                <Avatar src={player.avatarUrl} size={{ base: 60, sm: 80 }} radius={80} color="brandRed">
                    {player.nickname[0]}
                </Avatar>
            </Group>

            <Text fw={700} ta="center" size={{ base: 'md', sm: 'xl' }} mt="xs">
                {player.nickname}
            </Text>

            <Group justify="center" gap={4} mt={4} mb="xs">
                <IconMapPin size={13} style={{ opacity: 0.5 }} />
                <Text c="dimmed" size="xs">{player.city || 'Не указан'}</Text>
            </Group>

            <Card.Section inheritPadding py="xs" withBorder style={{ backgroundColor: c.surface3 }}>
                <Group justify="center">
                    <Badge color={player.role === 'admin' ? 'orange' : 'blue'} variant="light" size="sm">
                        {player.role === 'admin' ? 'Администратор' : 'Игрок'}
                    </Badge>
                </Group>
            </Card.Section>
        </Card>
    );
}
