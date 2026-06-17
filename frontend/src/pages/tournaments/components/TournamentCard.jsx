// src/pages/tournaments/components/TournamentCard.jsx
import { Paper, Group, ThemeIcon, Text, Badge, Progress, Stack } from '@mantine/core';
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
    const isFull       = currentCount >= maxCount;

    const dateLabel = formatTournamentDates(tour.startDate, tour.endDate);
    const isRating  = tour.type === 'season';

    return (
        <Paper
            component={Link}
            to={`/tournaments/${tour.id}`}
            withBorder
            radius="md"
            style={{
                textDecoration: 'none',
                color: 'inherit',
                transition: 'background-color 0.15s, transform 0.1s',
                backgroundColor: c.surface2,
                display: 'block',
            }}
            onMouseEnter={e => {
                e.currentTarget.style.backgroundColor = c.surface3;
                e.currentTarget.style.transform = 'translateY(-1px)';
            }}
            onMouseLeave={e => {
                e.currentTarget.style.backgroundColor = c.surface2;
                e.currentTarget.style.transform = 'translateY(0)';
            }}
        >
            <Group wrap="nowrap" gap={0} align="stretch">
                {/* Цветная левая полоска */}
                <div style={{
                    width: 4,
                    borderRadius: '8px 0 0 8px',
                    backgroundColor: `var(--mantine-color-${typeInfo.color}-filled)`,
                    flexShrink: 0,
                }} />

                {/* Основной контент */}
                <Group
                    p={{ base: 'sm', sm: 'md' }}
                    justify="space-between"
                    wrap="nowrap"
                    style={{ flex: 1, minWidth: 0 }}
                >
                    {/* Иконка */}
                    <ThemeIcon
                        size={40}
                        radius="md"
                        color={typeInfo.color}
                        variant="light"
                        style={{ flexShrink: 0 }}
                    >
                        {isRating
                            ? <IconTrophy size={20} />
                            : <IconUsers size={20} />
                        }
                    </ThemeIcon>

                    {/* Текст */}
                    <Stack gap={4} style={{ flex: 1, minWidth: 0 }}>
                        {/* Заголовок и тип на одной строке */}
                        <Group gap="xs" wrap="nowrap" align="center">
                            <Text
                                fw={700}
                                size="md"
                                truncate
                                style={{ flex: 1, minWidth: 0 }}
                            >
                                {tour.title}
                            </Text>
                            <Badge
                                color={typeInfo.color}
                                variant="light"
                                size="xs"
                                style={{ flexShrink: 0 }}
                                visibleFrom="sm"
                            >
                                {typeInfo.label}
                            </Badge>
                        </Group>

                        {/* Дата и статус */}
                        <Group gap="xs" wrap="wrap">
                            <Group gap={4} c="dimmed">
                                <IconCalendar size={12} />
                                <Text size="xs">{dateLabel}</Text>
                            </Group>
                            {statusInfo && (
                                <Badge color={statusInfo.color} variant="dot" size="xs">
                                    {statusInfo.label}
                                </Badge>
                            )}
                        </Group>

                        {/* Прогресс участников — только не для рейтинга */}
                        {!isRating && (
                            <Group gap="xs" align="center" mt={2}>
                                <Progress
                                    value={progress}
                                    size="xs"
                                    color={isFull ? 'red' : typeInfo.color}
                                    radius="xl"
                                    style={{ flex: 1 }}
                                />
                                <Text size="xs" c={isFull ? 'red' : 'dimmed'} style={{ flexShrink: 0 }}>
                                    {currentCount}/{maxCount}
                                </Text>
                            </Group>
                        )}
                    </Stack>

                    {/* Стрелка */}
                    <IconChevronRight
                        size={16}
                        color={c.textSecondary}
                        style={{ flexShrink: 0, opacity: 0.5 }}
                    />
                </Group>
            </Group>
        </Paper>
    );
}