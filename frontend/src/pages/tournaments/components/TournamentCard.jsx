// src/pages/tournaments/components/TournamentCard.jsx
import { Paper, Group, ThemeIcon, Text, Badge, Avatar, Progress } from '@mantine/core';
import { IconCalendar, IconUsers, IconTrophy, IconChevronRight } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import dayjs from 'dayjs';
import 'dayjs/locale/ru';
import { TOURNAMENT_TYPES, TOURNAMENT_STATUSES } from '../constants/tournamentConstants';
import { useThemeColors } from '../../../hooks/useThemeColors';

export default function TournamentCard({ tour }) {
    const c = useThemeColors();

    const typeInfo   = TOURNAMENT_TYPES[tour.type]      || { color: 'gray', label: tour.type };
    const statusInfo = TOURNAMENT_STATUSES[tour.status] || null;

    const currentCount = tour.participantsCount || 0;
    const maxCount     = tour.settings?.maxParticipants || 100;
    const progress     = Math.min((currentCount / maxCount) * 100, 100);

    const dateRange = tour.startDate && tour.endDate
        ? `${dayjs(tour.startDate).locale('ru').format('D MMM')} — ${dayjs(tour.endDate).locale('ru').format('D MMM YYYY')}`
        : 'Даты не указаны';

    return (
        <Paper
            component={Link}
            to={`/tournaments/${tour.id}`}
            withBorder p="md" radius="md"
            style={{
                textDecoration: 'none',
                color: 'inherit',
                transition: 'background-color 0.15s',
                backgroundColor: c.surface2,
            }}
            onMouseEnter={e => e.currentTarget.style.backgroundColor = c.surface3}
            onMouseLeave={e => e.currentTarget.style.backgroundColor = c.surface2}
        >
            <Group justify="space-between" wrap="nowrap" align="flex-start">
                <Group wrap="nowrap" gap={{ base: 'sm', sm: 'xl' }} align="flex-start" style={{ flex: 1, minWidth: 0 }}>
                    <ThemeIcon size={48} radius="md" color={typeInfo.color} variant="light" style={{ flexShrink: 0 }}>
                        {tour.type === 'season' ? <IconTrophy size={28} /> : <IconUsers size={28} />}
                    </ThemeIcon>

                    <div style={{ flex: 1, minWidth: 0 }}>
                        <Group align="center" gap="sm" mb={4} wrap="wrap">
                            <Text fw={700} size="lg" style={{ wordBreak: 'break-word' }}>{tour.title}</Text>
                            <Badge color={typeInfo.color} variant="outline" size="sm">{typeInfo.label}</Badge>
                            {statusInfo && <Badge color={statusInfo.color} variant="dot" size="sm">{statusInfo.label}</Badge>}
                        </Group>

                        <Group gap="lg" wrap="wrap">
                            <Group gap={5} c="dimmed">
                                <IconCalendar size={15} />
                                <Text size="sm">{dateRange}</Text>
                            </Group>
                            {tour.clubName && (
                                <Group gap={5}>
                                    <Avatar size={16} radius="xl" />
                                    <Text size="sm" c="dimmed">{tour.clubName}</Text>
                                </Group>
                            )}
                        </Group>

                        {tour.type !== 'season' && (
                            <Group gap="xs" mt="xs" align="center" style={{ maxWidth: 300 }}>
                                <Progress
                                    value={progress} size="sm"
                                    color={progress >= 100 ? 'red' : 'blue'}
                                    radius="xl" style={{ flex: 1 }}
                                />
                                <Text size="xs" c="dimmed">{currentCount}/{maxCount}</Text>
                            </Group>
                        )}
                    </div>
                </Group>

                <ThemeIcon variant="transparent" color="gray" style={{ flexShrink: 0 }}>
                    <IconChevronRight size={24} />
                </ThemeIcon>
            </Group>
        </Paper>
    );
}
