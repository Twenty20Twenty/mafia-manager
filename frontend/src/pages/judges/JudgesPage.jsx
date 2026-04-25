// src/pages/judges/JudgesPage.jsx
import { useState, useEffect } from 'react';
import {
    Container, Title, Paper, Group, Avatar, Text, Badge, Button,
    TextInput, Table, Center, Loader, Pagination, Stack, Box
} from '@mantine/core';
import { IconGavel, IconStar, IconSettings, IconSearch, IconCalendar } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import api from '../../api/axios';
import dayjs from 'dayjs';
import 'dayjs/locale/ru';
import { useThemeColors } from '../../hooks/useThemeColors';

function QualificationBadges({ judge }) {
    if (!judge.canJudgeFinals && !judge.canBeHeadJudge) {
        return <Badge color="gray" variant="light" size="sm">Судья</Badge>;
    }
    return (
        <Group gap={5}>
            {judge.canJudgeFinals && <Badge color="orange" variant="light" size="sm" leftSection={<IconStar size={10} />}>Финал</Badge>}
            {judge.canBeHeadJudge && <Badge color="violet" variant="light" size="sm" leftSection={<IconGavel size={10} />}>ГС</Badge>}
        </Group>
    );
}

export default function JudgesPage() {
    const { user } = useAuth();
    const c = useThemeColors();

    const [search, setSearch]         = useState('');
    const [judges, setJudges]         = useState([]);
    const [loading, setLoading]       = useState(true);
    const [activePage, setPage]       = useState(1);
    const [totalPages, setTotalPages] = useState(1);

    useEffect(() => {
        const fetchJudges = async () => {
            setLoading(true);
            try {
                const res = await api.get('/judges', { params: { search, page: activePage - 1, size: 20 } });
                setJudges(res.data.content);
                setTotalPages(res.data.totalPages);
            } catch (error) {
                console.error("Ошибка загрузки судей", error);
            } finally {
                setLoading(false);
            }
        };
        const timer = setTimeout(fetchJudges, 300);
        return () => clearTimeout(timer);
    }, [search, activePage]);

    return (
        <Container size="xl" py="xl">
            <Group justify="space-between" mb="xl" visibleFrom="sm">
                <Title order={2}>Судейский корпус</Title>
                <Group>
                    {user?.isAdmin && (
                        <Button component={Link} to="/judges/manage" leftSection={<IconSettings size={16} />} variant="filled" >
                            Управление
                        </Button>
                    )}
                    <TextInput placeholder="Поиск судьи..." leftSection={<IconSearch size={16} />}
                        value={search} onChange={e => { setSearch(e.currentTarget.value); setPage(1); }} />
                </Group>
            </Group>

            <Stack gap="sm" mb="xl" hiddenFrom="sm">
                <Group justify="space-between">
                    <Title order={2}>Судейский корпус</Title>
                    {user?.isAdmin && (
                        <Button component={Link} to="/judges/manage" leftSection={<IconSettings size={16} />} size="sm" color="dark">
                            Управление
                        </Button>
                    )}
                </Group>
                <TextInput placeholder="Поиск судьи..." leftSection={<IconSearch size={16} />}
                    value={search} onChange={e => { setSearch(e.currentTarget.value); setPage(1); }} />
            </Stack>

            {loading ? (
                <Center><Loader color="brandRed" /></Center>
            ) : judges.length === 0 ? (
                <Text c="dimmed" ta="center" py="xl">Судьи не найдены</Text>
            ) : (
                <>
                    {/* Десктоп */}
                    <Paper withBorder radius="md" p={0} style={{ overflow: 'hidden', backgroundColor: c.surface2 }} visibleFrom="sm">
                        <Table verticalSpacing="sm" highlightOnHover
                            styles={{ thead: { backgroundColor: c.tableHeader } }}>
                            <Table.Thead>
                                <Table.Tr>
                                    <Table.Th>Судья</Table.Th>
                                    <Table.Th>Квалификация</Table.Th>
                                    <Table.Th>Статус выдан</Table.Th>
                                </Table.Tr>
                            </Table.Thead>
                            <Table.Tbody>
                                {judges.map(judge => (
                                    <Table.Tr key={judge.userId}>
                                        <Table.Td>
                                            <Group gap="sm" component={Link} to={`/players/${judge.userId}`}
                                                style={{ textDecoration: 'none', color: 'inherit', cursor: 'pointer' }}>
                                                <Avatar src={judge.avatarUrl} size={40} radius="xl" color="brandRed">{judge.nickname[0]}</Avatar>
                                                <div>
                                                    <Text size="sm" fw={500}>{judge.nickname}</Text>
                                                    {judge.clubName && <Text size="xs" c="dimmed">{judge.clubName}</Text>}
                                                </div>
                                            </Group>
                                        </Table.Td>
                                        <Table.Td><QualificationBadges judge={judge} /></Table.Td>
                                        <Table.Td>
                                            <Group gap={5} c="dimmed">
                                                <IconCalendar size={14} />
                                                <Text size="sm">
                                                    {judge.judgeSince ? dayjs(judge.judgeSince).locale('ru').format('D MMMM YYYY') : 'Не указана'}
                                                </Text>
                                            </Group>
                                        </Table.Td>
                                    </Table.Tr>
                                ))}
                            </Table.Tbody>
                        </Table>
                    </Paper>

                    {/* Мобилка */}
                    <Stack gap="sm" hiddenFrom="sm">
                        {judges.map(judge => (
                            <Paper key={judge.userId} component={Link} to={`/players/${judge.userId}`}
                                withBorder p="sm" radius="md"
                                style={{ textDecoration: 'none', color: 'inherit', backgroundColor: c.surface2 }}>
                                <Group justify="space-between" wrap="nowrap">
                                    <Group wrap="nowrap" style={{ minWidth: 0 }}>
                                        <Avatar src={judge.avatarUrl} size={40} radius="xl" color="brandRed" style={{ flexShrink: 0 }}>
                                            {judge.nickname[0]}
                                        </Avatar>
                                        <div style={{ minWidth: 0 }}>
                                            <Text size="sm" fw={500} truncate>{judge.nickname}</Text>
                                            {judge.clubName && <Text size="xs" c="dimmed" truncate>{judge.clubName}</Text>}
                                            <Group gap={4} mt={3} c="dimmed">
                                                <IconCalendar size={12} />
                                                <Text size="xs">
                                                    {judge.judgeSince ? dayjs(judge.judgeSince).locale('ru').format('D MMM YYYY') : 'Дата не указана'}
                                                </Text>
                                            </Group>
                                        </div>
                                    </Group>
                                    <Box style={{ flexShrink: 0 }}>
                                        <QualificationBadges judge={judge} />
                                    </Box>
                                </Group>
                            </Paper>
                        ))}
                    </Stack>

                    {totalPages > 1 && (
                        <Center py="md">
                            <Pagination total={totalPages} value={activePage} onChange={setPage} color="brandRed" size={{ base: 'sm', sm: 'md' }} />
                        </Center>
                    )}
                </>
            )}
        </Container>
    );
}
