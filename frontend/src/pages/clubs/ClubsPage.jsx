// src/pages/clubs/ClubsPage.jsx
import { useState, useMemo, useEffect } from 'react';
import { Container, Title, Paper, Group, Avatar, Text, Stack, ThemeIcon, TextInput, Button, Center, Loader } from '@mantine/core';
import { Link } from 'react-router-dom';
import { IconChevronRight, IconMapPin, IconSearch, IconPlus } from '@tabler/icons-react';
import { useAuth } from '../../context/AuthContext.jsx';
import api from '../../api/axios';
import { useThemeColors } from '../../hooks/useThemeColors';

export default function ClubsPage() {
    const [search, setSearch]                         = useState('');
    const [clubs, setClubs]                           = useState([]);
    const [currentUserProfile, setCurrentUserProfile] = useState(null);
    const [loading, setLoading]                       = useState(true);
    const { user } = useAuth();
    const c = useThemeColors();

    useEffect(() => {
        const fetchData = async () => {
            try {
                if (user) {
                    const [clubsRes, userRes] = await Promise.all([
                        api.get('/clubs'),
                        api.get(`/users/${user.id}`)
                    ]);
                    setClubs(clubsRes.data);
                    setCurrentUserProfile(userRes.data);
                } else {
                    const clubsRes = await api.get('/clubs');
                    setClubs(clubsRes.data);
                }
            } catch (error) {
                console.error('Ошибка загрузки данных', error);
            } finally {
                setLoading(false);
            }
        };
        const timer = setTimeout(fetchData, 300);
        return () => clearTimeout(timer);
    }, [user]);

    const filteredClubs = useMemo(() =>
        clubs.filter(club =>
            club.name.toLowerCase().includes(search.toLowerCase()) ||
            (club.city && club.city.toLowerCase().includes(search.toLowerCase()))
        ),
        [search, clubs]
    );

    const canCreateClub = user && currentUserProfile && !currentUserProfile.clubId;

    return (
        <Container size="lg" py="xl">
            <Group justify="space-between" mb="xl" visibleFrom="sm">
                <Title order={2}>Клубы</Title>
                <Group>
                    {canCreateClub && (
                        <Button component={Link} to="/create-club" leftSection={<IconPlus size={16} />} color="brandRed">
                            Создать клуб
                        </Button>
                    )}
                    <TextInput placeholder="Поиск клуба..." leftSection={<IconSearch size={16} />}
                        value={search} onChange={e => setSearch(e.currentTarget.value)} w={250} />
                </Group>
            </Group>

            <Stack gap="sm" mb="xl" hiddenFrom="sm">
                <Group justify="space-between">
                    <Title order={2}>Клубы</Title>
                    {canCreateClub && (
                        <Button component={Link} to="/create-club" leftSection={<IconPlus size={16} />} color="brandRed" size="sm">
                            Создать клуб
                        </Button>
                    )}
                </Group>
                <TextInput placeholder="Поиск клуба..." leftSection={<IconSearch size={16} />}
                    value={search} onChange={e => setSearch(e.currentTarget.value)} />
            </Stack>

            {loading ? (
                <Center><Loader color="brandRed" /></Center>
            ) : filteredClubs.length === 0 ? (
                <Text c="dimmed" ta="center" py="xl">Клубы не найдены</Text>
            ) : (
                <Stack gap="md">
                    {filteredClubs.map(club => (
                        <Paper
                            key={club.id}
                            component={Link} to={`/clubs/${club.id}`}
                            withBorder p="md" radius="md"
                            style={{
                                textDecoration: 'none', color: 'inherit',
                                transition: 'background-color 0.2s',
                                backgroundColor: c.surface2,
                            }}
                            onMouseEnter={e => e.currentTarget.style.backgroundColor = c.surface3}
                            onMouseLeave={e => e.currentTarget.style.backgroundColor = c.surface2}
                        >
                            <Group justify="space-between" wrap="nowrap">
                                <Group wrap="nowrap" style={{ minWidth: 0 }}>
                                    <Avatar src={club.logoUrl} size="md" radius="md" bg="white" p={2} style={{ flexShrink: 0 }} />
                                    <div style={{ minWidth: 0 }}>
                                        <Text size="md" fw={700} truncate>{club.name}</Text>
                                        <Group gap={5} c="dimmed" mt={0}>
                                            <IconMapPin size={14} />
                                            <Text size="xs">{club.city || 'Не указан'}</Text>
                                        </Group>
                                    </div>
                                </Group>
                                <ThemeIcon variant="transparent" color="gray" style={{ flexShrink: 0 }}>
                                    <IconChevronRight size={20} />
                                </ThemeIcon>
                            </Group>
                        </Paper>
                    ))}
                </Stack>
            )}
        </Container>
    );
}
