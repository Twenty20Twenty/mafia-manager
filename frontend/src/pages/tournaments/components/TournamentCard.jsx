// src/pages/tournaments/components/TournamentCard.jsx
import {Paper, Group, ThemeIcon, Text, Badge, Avatar, Progress, Stack} from '@mantine/core';
import { IconCalendar, IconUsers, IconTrophy, IconChevronRight } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { TOURNAMENT_TYPES, TOURNAMENT_STATUSES } from '../constants/tournamentConstants';
import { useThemeColors } from '../../../hooks/useThemeColors';
import { formatTournamentDates } from '../utils/tournamentDateUtils';

export default function TournamentCard({ tour }) {
    const c = useThemeColors();

    const typeInfo   = TOURNAMENT_TYPES[tour.type]      || { color: 'gray', label: tour.type };
    const statusInfo = TOURNAMENT_STATUSES[tour.status] || null;

    const currentCount = tour.participantsCount || 0;
    const maxCount     = tour.settings?.maxParticipants || 100;
    const progress     = Math.min((currentCount / maxCount) * 100, 100);

    const dateLabel = formatTournamentDates(tour.startDate, tour.endDate);

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
                display: 'block',
            }}
            onMouseEnter={e => e.currentTarget.style.backgroundColor = c.surface3}
            onMouseLeave={e => e.currentTarget.style.backgroundColor = c.surface2}
        >
            <Group justify="space-between" wrap="nowrap" align="center">
                <Group wrap="nowrap" gap={{ base: 'sm', sm: 'xl' }} align="flex-start"
                       style={{ flex: 1, minWidth: 0 }}>
                    <ThemeIcon size={48} radius="md" color={typeInfo.color} variant="light"
                               style={{ flexShrink: 0 }}>
                        {tour.type === 'season' ? <IconTrophy size={28} /> : <IconUsers size={28} />}
                    </ThemeIcon>

                    <div style={{ flex: 1, minWidth: 0 }}>
                        <Stack align="flex-start" gap="xs" mb="xs">
                            <Text fw={700} size="xl" style={{ wordBreak: 'break-word' }}>
                                {tour.title}
                            </Text>

                            <Badge color={typeInfo.color} variant="outline" size="sm">
                                {typeInfo.label}
                            </Badge>

                            {statusInfo && (
                                <Badge color={statusInfo.color} variant="dot" size="sm">
                                    {statusInfo.label}
                                </Badge>
                            )}
                        </Stack>

                        <Stack gap={4} wrap="wrap" mb={tour.type !== 'season' ? 'xs' : 0}>
                            <Group gap={5} c="dimmed">
                                <IconCalendar size={15} />
                                <Text size="sm">{dateLabel}</Text>
                            </Group>
                            {/*
                            {tour.clubName && (
                                <Group gap={5}>
                                    <Text size="sm" c="dimmed">{tour.clubName}</Text>
                                </Group>
                            )}*/}
                        </Stack>

                        {tour.type !== 'season' && (
                            <Group gap="xs" align="center" style={{ maxWidth: 300 }}>
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