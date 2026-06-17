// src/pages/clubs/ClubProfilePage.jsx
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useState, useEffect, useCallback } from 'react';
import {
    ActionIcon, Avatar, Badge, Button, Container, Grid, Group, Paper, Stack,
    Tabs, Text, TextInput, ThemeIcon, Title, Tooltip, Center, Loader, Box, Pagination
} from '@mantine/core';
import {
    IconArrowLeft, IconBan, IconBrandVk, IconCalendar, IconCheck, IconCrown,
    IconDoorExit, IconEdit, IconMapPin, IconPlus, IconSearch, IconTrophy,
    IconTrophyOff, IconUserPlus, IconUsers, IconX, IconClock,
} from '@tabler/icons-react';
import dayjs from 'dayjs';
import api from '../../api/axios';
import { useAuth } from '../../context/AuthContext';
import { TOURNAMENT_STATUSES, TOURNAMENT_TYPES } from '../tournaments/constants/tournamentConstants';
import { notifications } from '@mantine/notifications';
import { useThemeColors } from '../../hooks/useThemeColors';

const TOURS_PAGE_SIZE = 8;
const FINISHED_STATUSES = new Set(['completed', 'archived']);

function sortTournamentsForClub(tours) {
    return [...tours].sort((a, b) => {
        const aFinished = FINISHED_STATUSES.has(a.status) ? 1 : 0;
        const bFinished = FINISHED_STATUSES.has(b.status) ? 1 : 0;
        if (aFinished !== bFinished) return aFinished - bFinished;
        // внутри группы — новые первее
        const dateA = a.startDate ? new Date(a.startDate) : null;
        const dateB = b.startDate ? new Date(b.startDate) : null;
        if (!dateA && !dateB) return 0;
        if (!dateA) return 1;
        if (!dateB) return -1;
        return dateB - dateA;
    });
}

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
    const [joinLoading, setJoinLoading]         = useState(false);
    const [toursPage, setToursPage]             = useState(1);

    const [requestStatus, setRequestStatus] = useState(null);

    const clubId = Number(id);

    // ── Загрузка статуса заявки ──────────────────────────────────────────────
    const fetchRequestStatus = useCallback(async () => {
        if (!user) return;
        try {
            const res = await api.get(`/clubs/${clubId}/my-request-status`);
            setRequestStatus(res.data);
        } catch (err) {
            console.warn('Не удалось получить статус заявки', err);
            setRequestStatus(null);
        }
    }, [clubId, user]);

    // ── Основная загрузка страницы ───────────────────────────────────────────
    const fetchData = useCallback(async () => {
        try {
            const [clubRes, toursRes] = await Promise.all([
                api.get(`/clubs/${clubId}`),
                api.get(`/tournaments?clubId=${clubId}`)
            ]);
            setClub(clubRes.data);
            setClubTournaments(sortTournamentsForClub(toursRes.data));

            if (user && user.id === clubRes.data.presidentId) {
                const reqsRes = await api.get(`/clubs/${clubId}/requests`);
                setApplications(reqsRes.data);
            }
        } catch (error) {
            console.error('Ошибка загрузки данных клуба', error);
            notifications.show({ color: 'red', message: 'Клуб не найден или произошла ошибка' });
            navigate('/clubs');
        } finally {
            setLoading(false);
        }
    }, [clubId, user, navigate]);

    useEffect(() => {
        fetchData();
        fetchRequestStatus();
    }, [fetchData, fetchRequestStatus]);

    // ── Обработчики ──────────────────────────────────────────────────────────

    const handleJoinRequest = async () => {
        setJoinLoading(true);
        try {
            await api.post(`/clubs/${clubId}/join`);
            notifications.show({ color: 'green', title: 'Успех', message: 'Заявка отправлена!' });
            await Promise.all([fetchData(), fetchRequestStatus()]);
        } catch (error) {
            notifications.show({
                color: 'red',
                title: 'Ошибка',
                message: error.response?.data?.message || 'Ошибка отправки заявки'
            });
        } finally {
            setJoinLoading(false);
        }
    };

    const handleLeaveClub = async () => {
        const isPresident = user?.id === club?.presidentId;
        if (isPresident) {
            if (club.members.length > 1) {
                alert('Вы не можете покинуть клуб, так как в нём состоят другие игроки.');
                return;
            }
            if (window.confirm('Вы единственный участник. При выходе клуб будет НАВСЕГДА УДАЛЁН. Продолжить?')) {
                try {
                    await api.delete(`/clubs/${clubId}`);
                    await refreshUser();
                    navigate('/clubs');
                } catch (error) {
                    alert(error.response?.data?.message || 'Ошибка при удалении клуба');
                }
            }
        } else {
            if (window.confirm('Вы уверены, что хотите покинуть клуб?')) {
                try {
                    await api.delete(`/clubs/${clubId}/leave`);
                    notifications.show({ color: 'green', title: 'Успех', message: 'Вы покинули клуб' });
                    await refreshUser();
                    navigate('/clubs');
                } catch (error) {
                    notifications.show({
                        color: 'red',
                        title: 'Ошибка',
                        message: error.response?.data?.message || 'Ошибка выхода из клуба'
                    });
                }
            }
        }
    };

    const handleApplication = async (playerId, approve) => {
        try {
            await api.post(`/clubs/${clubId}/requests/${playerId}/approve?approve=${approve}`);
            await fetchData();
        } catch {
            notifications.show({ color: 'red', message: 'Ошибка при обработке заявки' });
        }
    };

    const handleKickMember = async (memberId, memberName) => {
        if (window.confirm(`Исключить игрока ${memberName} из клуба?`)) {
            try {
                await api.delete(`/clubs/${clubId}/members/${memberId}`);
                await fetchData();
            } catch {
                notifications.show({ color: 'red', message: 'Ошибка исключения' });
            }
        }
    };

    const handleToggleTournamentRights = async (memberId, currentStatus) => {
        try {
            if (currentStatus) await api.put(`/clubs/${clubId}/members/${memberId}/revoke`);
            else                await api.put(`/clubs/${clubId}/members/${memberId}/grant`);
            await fetchData();
        } catch (error) {
            notifications.show({
                color: 'red',
                message: error.response?.data?.message || 'Ошибка изменения прав'
            });
        }
    };

    if (loading) return <Center py="xl"><Loader color="brandRed" /></Center>;
    if (!club)   return <Container py="xl">Клуб не найден</Container>;

    // ── Вычисляемые флаги ────────────────────────────────────────────────────
    const members              = (club.members || []).filter(p =>
        p.nickname.toLowerCase().includes(memberSearch.toLowerCase())
    );
    const president            = club.members?.find(m => m.id === club.presidentId);
    const isUserLoggedIn       = !!user;
    const isMember             = club.members?.some(m => m.id === user?.id);
    const isPresident          = user?.id === club.presidentId;
    const isClubOperator       = club.isTournamentOperator;
    const currentUserMemberObj = club.members?.find(m => m.id === user?.id);
    const hasPersonalRight     = currentUserMemberObj?.canCreateTournaments;
    const canCreateTournament  = isMember && isClubOperator && (isPresident || hasPersonalRight);

    const hasPendingRequestForThisClub  = requestStatus?.hasPendingRequestForThisClub  ?? false;
    const hasPendingRequestForOtherClub = requestStatus?.hasPendingRequestForOtherClub ?? false;

    const canJoin = isUserLoggedIn
        && !isMember
        && !isPresident
        && !hasPendingRequestForThisClub
        && !hasPendingRequestForOtherClub
        && !user?.clubId;

    // ── Пагинация турниров ───────────────────────────────────────────────────
    const toursTotalPages = Math.ceil(clubTournaments.length / TOURS_PAGE_SIZE);
    const paginatedTours  = clubTournaments.slice(
        (toursPage - 1) * TOURS_PAGE_SIZE,
        toursPage * TOURS_PAGE_SIZE
    );

    return (
        <Container size="lg" py="xl">
            <Group justify="space-between" mb="md">
                <Button component={Link} to="/clubs" variant="subtle" color="gray"
                        leftSection={<IconArrowLeft size={16} />}>
                    Все клубы
                </Button>
                {isPresident && (
                    <Button component={Link} to={`/clubs/${club.id}/edit`}
                            variant="default" leftSection={<IconEdit size={16} />} size="sm">
                        Редактировать
                    </Button>
                )}
            </Group>

            <Box mx={{ base: '-30px', sm: 0 }}>
                {/* ── Шапка клуба ─────────────────────────────────────────────── */}
                <Paper withBorder radius={{ base: 0, sm: 'md' }}
                       p={{ base: 'sm', sm: 'xl' }} mb="xl"
                       style={{ backgroundColor: c.surface2 }}>
                    <Grid gutter={{ base: 'sm', sm: 'xl' }} align="flex-start">

                        {/* Основная информация + Президент в одну строку */}
                        <Grid.Col span={{ base: 12, md: 8 }} order={1}>
                            <Group align="flex-start" wrap="nowrap" gap={{ base: 'sm', sm: 'md' }}>
                                <Avatar
                                    src={club.logoUrl}
                                    w={{ base: 72, sm: 120, md: 150 }}
                                    h={{ base: 72, sm: 120, md: 150 }}
                                    radius="50%"
                                    style={{ flexShrink: 0 }}
                                />
                                <Stack gap={4} style={{ flex: 1, minWidth: 0 }}>
                                    <Group wrap="wrap" gap="xs" align="center">
                                        <Title order={1} size={{ base: 'h3', sm: 'h2', md: 'h1' }}
                                               style={{ lineHeight: 1.2 }}>
                                            {club.name}
                                        </Title>
                                        {isClubOperator && (
                                            <Tooltip label="Клуб имеет лицензию на проведение турниров">
                                                <Badge color="teal" variant="light"
                                                       size={{ base: 'sm', sm: 'md' }}
                                                       leftSection={<IconTrophy size={12} />}>
                                                    Турнирный оператор
                                                </Badge>
                                            </Tooltip>
                                        )}
                                    </Group>
                                    <Group gap={4} c="dimmed">
                                        <IconMapPin size={14} />
                                        <Text size="sm">{club.city || 'Не указан'}</Text>
                                    </Group>
                                </Stack>
                            </Group>
                        </Grid.Col>

                        {/* Президент — справа на десктопе, сверху на мобиле */}
                        <Grid.Col span={{ base: 12, md: 4 }} order={{ base: 2, md: 1 }}>
                            <Paper withBorder p="md" radius="md"
                                   style={{ backgroundColor: c.surface3 }}>
                                <Text size="xs" c="dimmed" tt="uppercase" fw={700} mb="xs">
                                    Президент клуба
                                </Text>
                                {club.presidentId ? (
                                    <Group component={Link} to={`/players/${club.presidentId}`}
                                           style={{ textDecoration: 'none', cursor: 'pointer' }}
                                           wrap="nowrap">
                                        <Avatar src={president?.avatarUrl} size="lg" radius="xl"
                                                color="brandRed" />
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

                        {/* Описание */}
                        <Grid.Col span={12} order={3} pt={0}>
                            <Text size="sm" c="dimmed">{club.description}</Text>
                        </Grid.Col>

                        {/* Кнопки действий */}
                        <Grid.Col span={12} order={4}>
                            <Group wrap="wrap" gap="xs">
                                {club.socialLink && (
                                    <Button
                                        component="a"
                                        href={club.socialLink.startsWith('http')
                                            ? club.socialLink : `https://${club.socialLink}`}
                                        target="_blank"
                                        leftSection={<IconBrandVk size={18} />}
                                        variant="default" size="xs"
                                    >
                                        Мы ВКонтакте
                                    </Button>
                                )}
                                {isUserLoggedIn && (
                                    <>
                                        {isMember && !isPresident && (
                                            <Button color="red" variant="subtle" size="xs"
                                                    leftSection={<IconDoorExit size={16} />}
                                                    onClick={handleLeaveClub}>
                                                Покинуть клуб
                                            </Button>
                                        )}
                                        {canJoin && (
                                            <Button color="brandRed" variant="filled" size="xs"
                                                    leftSection={<IconPlus size={16} />}
                                                    loading={joinLoading}
                                                    onClick={handleJoinRequest}>
                                                Вступить в клуб
                                            </Button>
                                        )}
                                        {hasPendingRequestForThisClub && !isMember && (
                                            <Button color="yellow" variant="light" size="xs"
                                                    leftSection={<IconClock size={16} />}
                                                    disabled>
                                                Заявка отправлена
                                            </Button>
                                        )}
                                        {canCreateTournament && (
                                            <Button component={Link} to="/create-tournament"
                                                    variant="gradient"
                                                    gradient={{ from: 'orange', to: 'red' }}
                                                    size="xs"
                                                    leftSection={<IconTrophy size={16} />}>
                                                Создать турнир
                                            </Button>
                                        )}
                                    </>
                                )}
                            </Group>
                        </Grid.Col>
                    </Grid>
                </Paper>

                {/* ── Табы ────────────────────────────────────────────────────── */}
                <Tabs defaultValue="members" variant="outline" radius="md">
                    <Tabs.List mb="md" grow justify="flex-start">
                        <Tabs.Tab value="members" leftSection={<IconUsers size={16} />}>
                            <Box style={{ textAlign: 'left' }}>
                                Участники ({club.members?.length || 0})
                            </Box>
                        </Tabs.Tab>
                        <Tabs.Tab value="tournaments" leftSection={<IconTrophy size={16} />}>
                            <Box style={{ textAlign: 'left' }}>Турниры</Box>
                        </Tabs.Tab>
                        {isPresident && (
                            <Tabs.Tab value="applications" leftSection={<IconUserPlus size={16} />}
                                      color={applications.length > 0 ? 'red' : 'gray'}>
                                <Group gap={5} justify="flex-start" style={{ width: '100%' }}>
                                    <Text size="sm">Заявки</Text>
                                    {applications.length > 0 && (
                                        <Badge size="xs" circle color="red">{applications.length}</Badge>
                                    )}
                                </Group>
                            </Tabs.Tab>
                        )}
                    </Tabs.List>

                    {/* ── Участники ─────────────────────────────────────────── */}
                    <Tabs.Panel value="members">
                        <Stack gap="sm">
                            <TextInput
                                placeholder="Поиск участника..."
                                leftSection={<IconSearch size={14} />}
                                size="sm" value={memberSearch}
                                onChange={e => setMemberSearch(e.currentTarget.value)}
                                mb="sm"
                            />
                            {members.length > 0 ? (
                                members.map(member => (
                                    <Paper key={member.id} withBorder p="sm" radius="sm"
                                           style={{ backgroundColor: c.surface2 }}>
                                        <Group justify="space-between" wrap="nowrap">
                                            <Group
                                                component={Link} to={`/players/${member.id}`}
                                                style={{
                                                    textDecoration: 'none', color: 'inherit',
                                                    cursor: 'pointer', minWidth: 0
                                                }}
                                                wrap="nowrap"
                                            >
                                                <Avatar src={member.avatarUrl} size="sm" radius="xl"
                                                        style={{ flexShrink: 0 }} />
                                                <Text fw={500} truncate>{member.nickname}</Text>
                                                {member.id === club.presidentId && (
                                                    <IconCrown size={14} color="gold" fill="gold"
                                                               style={{ flexShrink: 0 }} />
                                                )}
                                                {member.canCreateTournaments && (
                                                    <Tooltip label="Может создавать турниры от лица клуба">
                                                        <Badge size="sm" variant="light" color="orange"
                                                               circle
                                                               style={{
                                                                   width: 24, height: 24, padding: 0,
                                                                   display: 'flex', justifyContent: 'center',
                                                                   flexShrink: 0
                                                               }}>
                                                            <IconTrophy size={14} />
                                                        </Badge>
                                                    </Tooltip>
                                                )}
                                            </Group>
                                            <Group gap="xs" style={{ flexShrink: 0 }}>
                                                {isPresident && member.id !== user.id && (
                                                    <>
                                                        {isClubOperator && (
                                                            <Tooltip label={member.canCreateTournaments
                                                                ? 'Забрать право' : 'Выдать право'}>
                                                                <ActionIcon
                                                                    variant="light"
                                                                    color={member.canCreateTournaments
                                                                        ? 'orange' : 'gray'}
                                                                    size="md" radius="xl"
                                                                    onClick={() => handleToggleTournamentRights(
                                                                        member.id, member.canCreateTournaments
                                                                    )}
                                                                >
                                                                    {member.canCreateTournaments
                                                                        ? <IconTrophyOff size={16} />
                                                                        : <IconTrophy size={16} />}
                                                                </ActionIcon>
                                                            </Tooltip>
                                                        )}
                                                        <Tooltip label="Исключить из клуба">
                                                            <ActionIcon variant="light" color="red"
                                                                        size="md" radius="xl"
                                                                        onClick={() => handleKickMember(
                                                                            member.id, member.nickname
                                                                        )}>
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

                    {/* ── Турниры ───────────────────────────────────────────── */}
                    <Tabs.Panel value="tournaments">
                        <Stack gap="md" mt="md">
                            {clubTournaments.length > 0 ? (
                                <>
                                    {paginatedTours.map(tour => {
                                        const typeInfo   = TOURNAMENT_TYPES[tour.type]      || { color: 'gray', label: 'Турнир' };
                                        const statusInfo = tour.status ? TOURNAMENT_STATUSES[tour.status] : null;
                                        const dateRange  = tour.startDate && tour.endDate
                                            ? tour.startDate === tour.endDate
                                                ? dayjs(tour.startDate).format('D MMM YYYY')
                                                : `${dayjs(tour.startDate).format('D MMM')} - ${dayjs(tour.endDate).format('D MMM YYYY')}`
                                            : 'Даты не указаны';
                                        return (
                                            <Paper key={tour.id} component={Link}
                                                   to={`/tournaments/${tour.id}`}
                                                   withBorder p="md" radius="md"
                                                   style={{
                                                       textDecoration: 'none', color: 'inherit',
                                                       backgroundColor: c.surface3
                                                   }}>
                                                <Group justify="space-between" wrap="nowrap">
                                                    <Group wrap="nowrap" style={{ minWidth: 0 }}>
                                                        <ThemeIcon size="lg" color={typeInfo.color}
                                                                   variant="light" style={{ flexShrink: 0 }}>
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
                                                    {statusInfo && (
                                                        <Badge color={statusInfo.color} style={{ flexShrink: 0 }}>
                                                            {statusInfo.label}
                                                        </Badge>
                                                    )}
                                                </Group>
                                            </Paper>
                                        );
                                    })}

                                    {toursTotalPages > 1 && (
                                        <Center mt="sm">
                                            <Pagination
                                                total={toursTotalPages}
                                                value={toursPage}
                                                onChange={setToursPage}
                                                color="brandRed"
                                                size="sm"
                                            />
                                        </Center>
                                    )}
                                </>
                            ) : (
                                <Paper withBorder p={50} ta="center" bg="transparent"
                                       style={{ borderStyle: 'dashed' }}>
                                    <Text c="dimmed">В этом клубе пока не проводились турниры.</Text>
                                </Paper>
                            )}
                        </Stack>
                    </Tabs.Panel>

                    {/* ── Заявки ────────────────────────────────────────────── */}
                    {isPresident && (
                        <Tabs.Panel value="applications">
                            <Stack gap="sm" mt="md">
                                {applications.length > 0 ? (
                                    applications.map(app => (
                                        <Paper key={app.id} withBorder p="sm" radius="sm"
                                               style={{ backgroundColor: c.surface2 }}>
                                            <Group justify="space-between" wrap="nowrap">
                                                <Group
                                                    component={Link} to={`/players/${app.id}`}
                                                    style={{
                                                        textDecoration: 'none', color: 'inherit',
                                                        cursor: 'pointer', minWidth: 0
                                                    }}
                                                    wrap="nowrap"
                                                >
                                                    <Avatar src={app.avatarUrl} size="md" radius="xl"
                                                            style={{ flexShrink: 0 }} />
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
                                                        <ActionIcon variant="light" color="red"
                                                                    size="lg" radius="xl"
                                                                    onClick={() => handleApplication(app.id, false)}>
                                                            <IconX size={18} />
                                                        </ActionIcon>
                                                    </Tooltip>
                                                    <Tooltip label="Принять в клуб">
                                                        <ActionIcon variant="filled" color="teal"
                                                                    size="lg" radius="xl"
                                                                    onClick={() => handleApplication(app.id, true)}>
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

            </Box>
        </Container>
    );
}