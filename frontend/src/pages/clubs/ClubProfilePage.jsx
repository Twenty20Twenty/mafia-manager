// src/pages/clubs/ClubProfilePage.jsx
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import {
    ActionIcon, Avatar, Badge, Button, Container, Grid, Group, Paper, Stack,
    Tabs, Text, TextInput, ThemeIcon, Title, Tooltip, Center, Loader, Box
} from '@mantine/core';
import {
    IconArrowLeft, IconBan, IconBrandVk, IconCalendar, IconCheck, IconCrown,
    IconDoorExit, IconEdit, IconMapPin, IconPlus, IconSearch, IconTrophy,
    IconTrophyOff, IconUserPlus, IconUsers, IconX,
} from '@tabler/icons-react';
import dayjs from 'dayjs';
import api from '../../api/axios';
import { useAuth } from '../../context/AuthContext';
import { TOURNAMENT_STATUSES, TOURNAMENT_TYPES } from '../../mocks/tournamentData';
import { notifications } from '@mantine/notifications';
import { useThemeColors } from '../../hooks/useThemeColors';

export default function ClubProfilePage() {
    const { id } = useParams();
    const { user, refreshUser } = useAuth();
    const navigate = useNavigate();
    const c = useThemeColors();

    const [club, setClub]                       = useState(null);
    const [clubTournaments, setClubTournaments] = useState([]);
    const [applications, setApplications]       = useState([]);
    const [loading, setLoading]                 = useState(true);
    const [memberSearch, setMemberSearch]       = useState('');

    const clubId = Number(id);

    const fetchData = async () => {
        try {
            const [clubRes, toursRes] = await Promise.all([
                api.get(`/clubs/${clubId}`),
                api.get(`/tournaments?clubId=${clubId}`)
            ]);
            setClub(clubRes.data);
            setClubTournaments(toursRes.data);

            if (user && user.id === clubRes.data.presidentId) {
                const reqsRes = await api.get(`/clubs/${clubId}/requests`);
                setApplications(reqsRes.data);
            }
        } catch (error) {
            console.error('Ошибка загрузки данных клуба', error);
            alert('Клуб не найден или произошла ошибка');
            navigate('/clubs');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchData(); }, [clubId, user]); // eslint-disable-line

    const handleJoinRequest = async () => {
        try {
            await api.post(`/clubs/${clubId}/join`);
            notifications.show({ color: 'green', title: 'Успех', message: 'Заявка отправлена!' });
            await fetchData();
        } catch (error) {
            notifications.show({ color: 'red', title: 'Ошибка', message: error.response?.data?.message || 'Ошибка отправки заявки' });
        }
    };

    const handleLeaveClub = async () => {
        const isPresident = user?.id === club?.presidentId;
        if (isPresident) {
            if (club.members.length > 1) { alert('Вы не можете покинуть клуб, так как в нем состоят другие игроки.'); return; }
            if (window.confirm('Вы единственный участник. При выходе клуб будет НАВСЕГДА УДАЛЕН. Продолжить?')) {
                try {
                    await api.delete(`/clubs/${clubId}`);
                    alert('Клуб успешно удален.');
                    await refreshUser();
                    navigate('/clubs');
                } catch (error) { alert(error.response?.data?.message || 'Ошибка при удалении клуба'); }
            }
        } else {
            if (window.confirm('Вы уверены, что хотите покинуть клуб?')) {
                try {
                    await api.delete(`/clubs/${clubId}/leave`);
                    notifications.show({ color: 'green', title: 'Успех', message: 'Вы покинули клуб' });
                    await refreshUser();
                    navigate('/clubs');
                } catch (error) {
                    notifications.show({ color: 'red', title: 'Ошибка', message: error.response?.data?.message || 'Ошибка выхода из клуба' });
                }
            }
        }
    };

    const handleApplication = async (playerId, approve) => {
        try {
            await api.post(`/clubs/${clubId}/requests/${playerId}/approve?approve=${approve}`);
            await fetchData();
        } catch { alert('Ошибка при обработке заявки'); }
    };

    const handleKickMember = async (memberId, memberName) => {
        if (window.confirm(`Исключить игрока ${memberName} из клуба?`)) {
            try { await api.delete(`/clubs/${clubId}/members/${memberId}`); await fetchData(); }
            catch { alert('Ошибка исключения'); }
        }
    };

    const handleToggleTournamentRights = async (memberId, currentStatus) => {
        try {
            if (currentStatus) await api.put(`/clubs/${clubId}/members/${memberId}/revoke`);
            else                await api.put(`/clubs/${clubId}/members/${memberId}/grant`);
            fetchData();
        } catch (error) { alert(error.response?.data?.message || 'Ошибка изменения прав'); }
    };

    if (loading) return <Center py="xl"><Loader color="brandRed" /></Center>;
    if (!club)   return <Container py="xl">Клуб не найден</Container>;

    const members              = (club.members || []).filter(p => p.nickname.toLowerCase().includes(memberSearch.toLowerCase()));
    const president            = club.members?.find(m => m.id === club.presidentId);
    const isUserLoggedIn       = !!user;
    const isMember             = club.members?.some(m => m.id === user?.id);
    const isPresident          = user?.id === club.presidentId;
    const isClubOperator       = club.isTournamentOperator;
    const currentUserMemberObj = club.members?.find(m => m.id === user?.id);
    const hasPersonalRight     = currentUserMemberObj?.canCreateTournaments;
    const canCreateTournament  = isMember && isClubOperator && (isPresident || hasPersonalRight);
    const hasPendingRequest    = applications.some(a => a.id === user?.id);
    const canJoin              = isUserLoggedIn && !isMember && !isPresident && !hasPendingRequest && !user?.clubId;

    return (
        <Container size="lg" py="xl">
            <Group justify="space-between" mb="md">
                <Button component={Link} to="/clubs" variant="subtle" color="gray" leftSection={<IconArrowLeft size={16} />}>
                    Все клубы
                </Button>
                {isPresident && (
                    <Button component={Link} to={`/clubs/${club.id}/edit`} variant="default" leftSection={<IconEdit size={16} />} size="sm">
                        Редактировать
                    </Button>
                )}
            </Group>

            {/* ── Шапка клуба ── */}
            <Paper withBorder radius="md" p={{ base: 'md', sm: 'xl' }} mb="xl"
                   style={{ backgroundColor: c.surface2 }}>
                <Grid gutter="xl" align="flex-start">
                    <Grid.Col span={{ base: 12, md: 8 }}>
                        <Group align="flex-start" wrap="nowrap" gap={{ base: 'sm', sm: 'md' }}>
                            <Avatar src={club.logoUrl} size={{ base: 72, sm: 120 }} radius="md" bg="white" p={5} style={{ flexShrink: 0 }} />
                            <Stack gap="xs" style={{ flex: 1, minWidth: 0 }}>
                                <Group wrap="wrap" gap="xs">
                                    <Title order={1} size={{ base: 'h2', sm: 'h1' }}>{club.name}</Title>
                                    {isClubOperator && (
                                        <Tooltip label="Клуб имеет лицензию на проведение турниров">
                                            <Badge color="teal" variant="light" size="md" leftSection={<IconTrophy size={14} />}>
                                                Турнирный оператор
                                            </Badge>
                                        </Tooltip>
                                    )}
                                </Group>
                                <Group gap={5} c="dimmed">
                                    <IconMapPin size={16} />
                                    <Text size="sm">{club.city || 'Не указан'}</Text>
                                </Group>
                                <Text size="sm" mt={4}>{club.description}</Text>
                                <Group mt="xs" wrap="wrap" gap="xs">
                                    {club.socialLink && (
                                        <Button component="a" href={club.socialLink.startsWith('http') ? club.socialLink : `https://${club.socialLink}`}
                                                target="_blank" leftSection={<IconBrandVk size={18} />} variant="default" size="xs">
                                            Мы ВКонтакте
                                        </Button>
                                    )}
                                    {isUserLoggedIn && (
                                        <>
                                            {isMember && !isPresident && (
                                                <Button color="red" variant="subtle" size="xs" leftSection={<IconDoorExit size={16} />} onClick={handleLeaveClub}>
                                                    Покинуть клуб
                                                </Button>
                                            )}
                                            {canJoin && (
                                                <Button color="brandRed" variant="filled" size="xs" leftSection={<IconPlus size={16} />} onClick={handleJoinRequest}>
                                                    Вступить в клуб
                                                </Button>
                                            )}
                                            {hasPendingRequest && (
                                                <Button color="teal" variant="light" size="xs" leftSection={<IconCheck size={16} />} disabled>
                                                    Заявка отправлена
                                                </Button>
                                            )}
                                            {canCreateTournament && (
                                                <Button component={Link} to="/create-tournament" variant="gradient"
                                                        gradient={{ from: 'orange', to: 'red' }} size="xs" leftSection={<IconTrophy size={16} />}>
                                                    Создать турнир
                                                </Button>
                                            )}
                                        </>
                                    )}
                                </Group>
                            </Stack>
                        </Group>
                    </Grid.Col>

                    {/* Президент */}
                    <Grid.Col span={{ base: 12, md: 4 }}>
                        <Paper withBorder p="md" radius="md" style={{ backgroundColor: c.surface3 }}>
                            <Text size="xs" c="dimmed" tt="uppercase" fw={700} mb="xs">Президент клуба</Text>
                            {club.presidentId ? (
                                <Group component={Link} to={`/players/${club.presidentId}`}
                                       style={{ textDecoration: 'none', cursor: 'pointer' }} wrap="nowrap">
                                    <Avatar src={president?.avatarUrl} size="lg" radius="xl" color="brandRed" />
                                    <div>
                                        <Group gap={5}>
                                            <Text fw={700} size="lg">{club.presidentName}</Text>
                                            <IconCrown size={16} color="gold" fill="gold" />
                                        </Group>
                                        <Text size="xs" c="dimmed">Перейти в профиль</Text>
                                    </div>
                                </Group>
                            ) : (
                                <Text c="dimmed">Не назначен</Text>
                            )}
                        </Paper>
                    </Grid.Col>
                </Grid>
            </Paper>

            {/* ── Табы ── */}
            <Tabs defaultValue="members" variant="outline" radius="md">
                <Tabs.List mb="md" grow justify="flex-start">
                    <Tabs.Tab value="members" leftSection={<IconUsers size={16} />}>
                        <Box style={{ textAlign: 'left' }}>Участники ({club.members?.length || 0})</Box>
                    </Tabs.Tab>
                    <Tabs.Tab value="tournaments" leftSection={<IconTrophy size={16} />}>
                        <Box style={{ textAlign: 'left' }}>Турниры</Box>
                    </Tabs.Tab>
                    {isPresident && (
                        <Tabs.Tab value="applications" leftSection={<IconUserPlus size={16} />}
                                  color={applications.length > 0 ? 'red' : 'gray'}>
                            <Group gap={5} justify="flex-start" style={{ width: '100%' }}>
                                <Text size="sm">Заявки</Text>
                                {applications.length > 0 && <Badge size="xs" circle color="red">{applications.length}</Badge>}
                            </Group>
                        </Tabs.Tab>
                    )}
                </Tabs.List>

                {/* Участники */}
                <Tabs.Panel value="members">
                    <Stack gap="sm">
                        <TextInput placeholder="Поиск участника..." leftSection={<IconSearch size={14} />}
                                   size="sm" value={memberSearch} onChange={e => setMemberSearch(e.currentTarget.value)} mb="sm" />
                        {members.length > 0 ? (
                            members.map(member => (
                                <Paper key={member.id} withBorder p="sm" radius="sm" style={{ backgroundColor: c.surface2 }}>
                                    <Group justify="space-between" wrap="nowrap">
                                        <Group component={Link} to={`/players/${member.id}`}
                                               style={{ textDecoration: 'none', color: 'inherit', cursor: 'pointer', minWidth: 0 }}
                                               wrap="nowrap">
                                            <Avatar src={member.avatarUrl} size="sm" radius="xl" style={{ flexShrink: 0 }} />
                                            <Text fw={500} truncate>{member.nickname}</Text>
                                            {member.id === club.presidentId && <IconCrown size={14} color="gold" fill="gold" style={{ flexShrink: 0 }} />}
                                            {member.canCreateTournaments && (
                                                <Tooltip label="Может создавать турниры от лица клуба">
                                                    <Badge size="sm" variant="light" color="orange" circle style={{ width: 24, height: 24, padding: 0, display: 'flex', justifyContent: 'center', flexShrink: 0 }}>
                                                        <IconTrophy size={14} />
                                                    </Badge>
                                                </Tooltip>
                                            )}
                                        </Group>
                                        <Group gap="xs" style={{ flexShrink: 0 }}>
                                            {isPresident && member.id !== user.id && (
                                                <>
                                                    {isClubOperator && (
                                                        <Tooltip label={member.canCreateTournaments ? 'Забрать право' : 'Выдать право'}>
                                                            <ActionIcon variant="light" color={member.canCreateTournaments ? 'orange' : 'gray'}
                                                                        size="md" radius="xl"
                                                                        onClick={() => handleToggleTournamentRights(member.id, member.canCreateTournaments)}>
                                                                {member.canCreateTournaments ? <IconTrophyOff size={16} /> : <IconTrophy size={16} />}
                                                            </ActionIcon>
                                                        </Tooltip>
                                                    )}
                                                    <Tooltip label="Исключить из клуба">
                                                        <ActionIcon variant="light" color="red" size="md" radius="xl"
                                                                    onClick={() => handleKickMember(member.id, member.nickname)}>
                                                            <IconBan size={16} />
                                                        </ActionIcon>
                                                    </Tooltip>
                                                </>
                                            )}
                                        </Group>
                                    </Group>
                                </Paper>
                            ))
                        ) : (
                            <Text c="dimmed" ta="center" py="xl">Участники не найдены</Text>
                        )}
                    </Stack>
                </Tabs.Panel>

                {/* Турниры */}
                <Tabs.Panel value="tournaments">
                    <Stack gap="md" mt="md">
                        {clubTournaments.length > 0 ? (
                            clubTournaments.map(tour => {
                                const typeInfo   = TOURNAMENT_TYPES[tour.type]      || { color: 'gray', label: 'Турнир' };
                                const statusInfo = tour.status ? TOURNAMENT_STATUSES[tour.status] : null;
                                const dateRange  = tour.startDate && tour.endDate
                                    ? `${dayjs(tour.startDate).format('D MMM')} - ${dayjs(tour.endDate).format('D MMM YYYY')}`
                                    : 'Даты не указаны';
                                return (
                                    <Paper key={tour.id} component={Link} to={`/tournaments/${tour.id}`}
                                           withBorder p="md" radius="md"
                                           style={{ textDecoration: 'none', color: 'inherit', backgroundColor: c.surface3 }}>
                                        <Group justify="space-between" wrap="nowrap">
                                            <Group wrap="nowrap" style={{ minWidth: 0 }}>
                                                <ThemeIcon size="lg" color={typeInfo.color} variant="light" style={{ flexShrink: 0 }}>
                                                    <IconTrophy size={20} />
                                                </ThemeIcon>
                                                <div style={{ minWidth: 0 }}>
                                                    <Text fw={700} truncate>{tour.title}</Text>
                                                    <Group gap={5} c="dimmed">
                                                        <IconCalendar size={14} />
                                                        <Text size="xs">{dateRange}</Text>
                                                    </Group>
                                                </div>
                                            </Group>
                                            {statusInfo && <Badge color={statusInfo.color} style={{ flexShrink: 0 }}>{statusInfo.label}</Badge>}
                                        </Group>
                                    </Paper>
                                );
                            })
                        ) : (
                            <Paper withBorder p={50} ta="center" bg="transparent" style={{ borderStyle: 'dashed' }}>
                                <Text c="dimmed">В этом клубе пока не проводились турниры.</Text>
                            </Paper>
                        )}
                    </Stack>
                </Tabs.Panel>

                {/* Заявки */}
                {isPresident && (
                    <Tabs.Panel value="applications">
                        <Stack gap="sm" mt="md">
                            {applications.length > 0 ? (
                                applications.map(app => (
                                    <Paper key={app.id} withBorder p="sm" radius="sm" style={{ backgroundColor: c.surface2 }}>
                                        <Group justify="space-between" wrap="nowrap">
                                            <Group component={Link} to={`/players/${app.id}`}
                                                   style={{ textDecoration: 'none', color: 'inherit', cursor: 'pointer', minWidth: 0 }}
                                                   wrap="nowrap">
                                                <Avatar src={app.avatarUrl} size="md" radius="xl" style={{ flexShrink: 0 }} />
                                                <div style={{ minWidth: 0 }}>
                                                    <Text fw={700} truncate>{app.nickname}</Text>
                                                    <Group gap={5} c="dimmed">
                                                        <IconMapPin size={12} />
                                                        <Text size="xs">{app.city || 'Не указан'}</Text>
                                                    </Group>
                                                </div>
                                            </Group>
                                            <Group gap="sm" style={{ flexShrink: 0 }}>
                                                <Tooltip label="Отклонить">
                                                    <ActionIcon variant="light" color="red" size="lg" radius="xl" onClick={() => handleApplication(app.id, false)}>
                                                        <IconX size={18} />
                                                    </ActionIcon>
                                                </Tooltip>
                                                <Tooltip label="Принять в клуб">
                                                    <ActionIcon variant="filled" color="teal" size="lg" radius="xl" onClick={() => handleApplication(app.id, true)}>
                                                        <IconCheck size={18} />
                                                    </ActionIcon>
                                                </Tooltip>
                                            </Group>
                                        </Group>
                                    </Paper>
                                ))
                            ) : (
                                <Text c="dimmed" ta="center" py="xl">Новых заявок нет</Text>
                            )}
                        </Stack>
                    </Tabs.Panel>
                )}
            </Tabs>
        </Container>
    );
}
