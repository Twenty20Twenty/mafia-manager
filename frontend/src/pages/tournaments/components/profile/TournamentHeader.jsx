// src/pages/tournaments/components/profile/TournamentHeader.jsx
import { Paper, Group, Badge, Title, Text, Avatar, Button, Stack } from '@mantine/core';
import { IconCalendar, IconUsers, IconChartBar, IconBrandVk, IconSettings, IconMapPin } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import dayjs from 'dayjs';
import { TOURNAMENT_TYPES, TOURNAMENT_STATUSES } from '../../constants/tournamentConstants';
import RatingThresholdInfo from './RatingThresholdInfo';
import { useThemeColors } from '../../../../hooks/useThemeColors';

export default function TournamentHeader({
    tournament, participantsCount,
    isRating, canManage,
    isRegistrationOpen, isAlreadyParticipant, isRequestSent,
    user, onRegister,
}) {
    const c = useThemeColors();

    const typeInfo        = TOURNAMENT_TYPES[tournament.type]      || { label: tournament.type, color: 'gray' };
    const statusInfo      = TOURNAMENT_STATUSES[tournament.status] || null;
    const maxParticipants = tournament.settings?.maxParticipants || '?';

    return (
        <Paper withBorder radius="md" p={{ base: 'md', sm: 'xl' }} mb="xl"
            style={{ backgroundColor: c.surface2 }}>
            <Group justify="space-between" align="flex-start">
                <div style={{ flex: 1, minWidth: 0 }}>
                    <Group mb="xs" wrap="wrap">
                        <Badge color={typeInfo.color} size="lg">{typeInfo.label}</Badge>
                        {statusInfo && <Badge color={statusInfo.color} variant="outline" size="lg">{statusInfo.label}</Badge>}
                    </Group>

                    <Title order={1}>{tournament.title}</Title>

                    {tournament.cityName && (
                        <Group gap={5} mt={6} c="dimmed">
                            <IconMapPin size={16} style={{ opacity: 0.7 }} />
                            <Text size="sm">{tournament.cityName}</Text>
                        </Group>
                    )}

                    <Text c="dimmed" mt="sm" size="md" style={{ maxWidth: 800 }}>
                        {tournament.description || 'Описание отсутствует'}
                    </Text>

                    <Group mt="md" gap={{ base: 'sm', sm: 'xl' }} wrap="wrap">
                        {(tournament.startDate || tournament.endDate) && (
                            <Group gap={5}>
                                <IconCalendar size={20} style={{ opacity: 0.7 }} />
                                <Text size="sm">
                                    {tournament.startDate ? dayjs(tournament.startDate).format('D MMM') : '...'}
                                    {tournament.endDate ? ` - ${dayjs(tournament.endDate).format('D MMM YYYY')}` : ''}
                                </Text>
                            </Group>
                        )}

                        <Group gap={5}>
                            {isRating ? (
                                <>
                                    <IconChartBar size={20} style={{ opacity: 0.7 }} />
                                    <Text size="sm">Участников: <span style={{ fontWeight: 700, color: 'var(--mantine-color-teal-4)' }}>{participantsCount}</span></Text>
                                </>
                            ) : (
                                <>
                                    <IconUsers size={20} style={{ opacity: 0.7 }} />
                                    <Text size="sm">Участников: <span style={{ fontWeight: 700, color: 'var(--mantine-color-blue-4)' }}>{participantsCount}</span> / {maxParticipants}</Text>
                                </>
                            )}
                        </Group>

                        {tournament.clubName && (
                            <Group gap={5} component={Link} to={`/clubs/${tournament.clubId}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                                <Text c="dimmed" size="sm">Клуб:</Text>
                                <Text fw={500} size="sm" style={{ borderBottom: `1px dashed ${c.border}` }}>{tournament.clubName}</Text>
                            </Group>
                        )}

                        {tournament.organizerId && (
                            <Group gap={5}>
                                <Text c="dimmed" size="sm">Орг:</Text>
                                <Group gap={5} component={Link} to={`/players/${tournament.organizerId}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                                    <Avatar src={tournament.organizerAvatar} size="sm" radius="xl" color="brandRed" />
                                    <Text fw={500} size="sm" style={{ borderBottom: `1px dashed ${c.border}` }}>{tournament.organizerName}</Text>
                                </Group>
                            </Group>
                        )}

                        {tournament.headJudgeId && (
                            <Group gap={5}>
                                <Text c="dimmed" size="sm">ГС:</Text>
                                <Group gap={5} component={Link} to={`/players/${tournament.headJudgeId}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                                    <Avatar src={tournament.headJudgeAvatar} size="sm" radius="xl" color="violet" />
                                    <Text fw={500} size="sm" style={{ borderBottom: `1px dashed ${c.border}` }}>{tournament.headJudgeName}</Text>
                                </Group>
                            </Group>
                        )}

                        {isRating && <RatingThresholdInfo tournament={tournament} />}
                    </Group>

                    <Group mt="xl" wrap="wrap" gap="sm">
                        {tournament.settings?.socialLink && (
                            <Button
                                component="a"
                                href={tournament.settings.socialLink.startsWith('http')
                                    ? tournament.settings.socialLink
                                    : `https://${tournament.settings.socialLink}`}
                                target="_blank"
                                leftSection={<IconBrandVk size={20} />}
                                variant="default"
                            >
                                Группа турнира
                            </Button>
                        )}

                        {isRegistrationOpen && !isRating && user && !isAlreadyParticipant && !isRequestSent && (
                            <Button color="green" onClick={onRegister}>Подать заявку</Button>
                        )}

                        {isRequestSent        && <Button disabled variant="light" color="yellow">Заявка на рассмотрении</Button>}
                        {isAlreadyParticipant && <Button disabled variant="light" color="blue">Вы участник</Button>}

                        {canManage && (
                            <Button component={Link} to={`/tournaments/${tournament.id}/manage`}
                                leftSection={<IconSettings size={18} />} color="orange" variant="outline">
                                Настроить турнир
                            </Button>
                        )}
                    </Group>
                </div>
            </Group>
        </Paper>
    );
}
