// src/pages/tournaments/components/profile/TournamentHeader.jsx
import {Paper, Group, Badge, Title, Text, Avatar, Button, Stack, Grid} from '@mantine/core';
import {
    IconCalendar, IconUsers, IconChartBar, IconBrandVk,
    IconSettings, IconMapPin, IconCrown
} from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { TOURNAMENT_TYPES, TOURNAMENT_STATUSES } from '../../constants/tournamentConstants';
import RatingThresholdInfo from './RatingThresholdInfo';
import { useThemeColors } from '../../../../hooks/useThemeColors';
import { formatTournamentDates } from '../../utils/tournamentDateUtils';

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

    const dateLabel = formatTournamentDates(tournament.startDate, tournament.endDate);

    return (
        <Paper
            withBorder
            radius="md"
            p={{ base: 'md', sm: 'xl' }}
            mb="xl"
            style={{ backgroundColor: c.surface2 }}
        >
            <Grid gutter="xl" align="flex-start">

                {/* LEFT SIDE */}
                <Grid.Col span={{ base: 12, md: 8 }}>

                    <Group mb="xs" wrap="wrap">
                        <Badge color={typeInfo.color} size="lg">
                            {typeInfo.label}
                        </Badge>

                        {statusInfo && (
                            <Badge
                                color={statusInfo.color}
                                variant="outline"
                                size="lg"
                            >
                                {statusInfo.label}
                            </Badge>
                        )}
                    </Group>

                    <Title order={1}>{tournament.title}</Title>

                    {tournament.cityName && (
                        <Group gap={5} mt={6} c="dimmed">
                            <IconMapPin size={16} style={{ opacity: 0.7 }} />
                            <Text size="sm">{tournament.cityName}</Text>
                        </Group>
                    )}

                    <Text
                        c="dimmed"
                        mt="sm"
                        size="md"
                        style={{ maxWidth: 800 }}
                    >
                        {tournament.description || 'Описание отсутствует'}
                    </Text>

                    {/* MINI BLOCKS */}
                    <Group mt="xl" grow align="stretch">

                        {/* DATE */}
                        {(tournament.startDate || tournament.endDate) && (
                            <Paper
                                withBorder
                                radius="md"
                                p="md"
                                style={{ backgroundColor: c.surface3 }}
                            >
                                <Group mb={6}>
                                    <IconCalendar size={18} />
                                    <Text size="xs" c="dimmed" tt="uppercase" fw={700}>
                                        Дата проведения
                                    </Text>
                                </Group>

                                <Text fw={600}>
                                    {dateLabel}
                                </Text>
                            </Paper>
                        )}

                        {/* PARTICIPANTS */}
                        <Paper
                            withBorder
                            radius="md"
                            p="md"
                            style={{ backgroundColor: c.surface3 }}
                        >
                            <Group mb={6}>
                                {isRating ? (
                                    <IconChartBar size={18} />
                                ) : (
                                    <IconUsers size={18} />
                                )}

                                <Text size="xs" c="dimmed" tt="uppercase" fw={700}>
                                    Участники
                                </Text>
                            </Group>

                            <Text fw={700} size="lg">
                                {participantsCount}

                                {!isRating && (
                                    <Text span fw={500} c="dimmed">
                                        {' '} / {maxParticipants}
                                    </Text>
                                )}
                            </Text>
                        </Paper>


                    </Group>

                    {/* RATING */}
                    {isRating && (
                        <Paper
                            withBorder
                            radius="md"
                            p="md"
                            mt="md"
                            style={{
                                backgroundColor: c.surface3,
                            }}
                        >
                            <Text
                                size="xs"
                                c="dimmed"
                                tt="uppercase"
                                fw={700}
                                mb={8}
                            >
                                Рейтинг
                            </Text>

                            <RatingThresholdInfo tournament={tournament} />
                        </Paper>
                    )}



                </Grid.Col>

                {/* RIGHT SIDE */}
                <Grid.Col span={{ base: 12, md: 4 }}>

                    <Stack gap="md">

                        {/* CLUB */}
                        {tournament.organizerId && (
                            <Paper
                                withBorder
                                p="md"
                                radius="md"
                                style={{ backgroundColor: c.surface3 }}
                            >
                                <Text
                                    size="xs"
                                    c="dimmed"
                                    tt="uppercase"
                                    fw={700}
                                    mb="xs"
                                >
                                    Клуб
                                </Text>

                                <Group
                                    component={Link}
                                    to={`/clubs/${tournament.clubId}`}
                                    style={{
                                        textDecoration: 'none',
                                        color: 'inherit',
                                    }}
                                    wrap="nowrap"
                                >
                                    <Avatar
                                        src={tournament.clubLogo}
                                        size="lg"
                                        radius="50%"
                                        color="brandRed"
                                        //w={30}
                                        //h={30}
                                    />

                                    <div>
                                        <Text fw={700} size="lg">
                                            {tournament.clubName}
                                        </Text>

                                        <Text size="xs" c="dimmed">
                                            Перейти в профиль
                                        </Text>
                                    </div>
                                </Group>
                            </Paper>
                        )}

                        {/* ORGANIZER */}
                        {tournament.organizerId && (
                            <Paper
                                withBorder
                                p="md"
                                radius="md"
                                style={{ backgroundColor: c.surface3 }}
                            >
                                <Text
                                    size="xs"
                                    c="dimmed"
                                    tt="uppercase"
                                    fw={700}
                                    mb="xs"
                                >
                                    Организатор
                                </Text>

                                <Group
                                    component={Link}
                                    to={`/players/${tournament.organizerId}`}
                                    style={{
                                        textDecoration: 'none',
                                        color: 'inherit',
                                    }}
                                    wrap="nowrap"
                                >
                                    <Avatar
                                        src={tournament.organizerAvatar}
                                        size="lg"
                                        radius="xl"
                                        color="brandRed"
                                    />

                                    <div>
                                        <Text fw={700} size="lg">
                                            {tournament.organizerName}
                                        </Text>

                                        <Text size="xs" c="dimmed">
                                            Перейти в профиль
                                        </Text>
                                    </div>
                                </Group>
                            </Paper>
                        )}

                        {/* HEAD JUDGE */}
                        {tournament.headJudgeId && (
                            <Paper
                                withBorder
                                p="md"
                                radius="md"
                                style={{ backgroundColor: c.surface3 }}
                            >
                                <Text
                                    size="xs"
                                    c="dimmed"
                                    tt="uppercase"
                                    fw={700}
                                    mb="xs"
                                >
                                    ГС турнира
                                </Text>

                                <Group
                                    component={Link}
                                    to={`/players/${tournament.headJudgeId}`}
                                    style={{
                                        textDecoration: 'none',
                                        color: 'inherit',
                                    }}
                                    wrap="nowrap"
                                >
                                    <Avatar
                                        src={tournament.headJudgeAvatar}
                                        size="lg"
                                        radius="xl"
                                        color="violet"
                                    />

                                    <div>
                                        <Text fw={700} size="lg">
                                            {tournament.headJudgeName}
                                        </Text>

                                        <Text size="xs" c="dimmed">
                                            Перейти в профиль
                                        </Text>
                                    </div>
                                </Group>
                            </Paper>
                        )}
                    </Stack>
                </Grid.Col>
            </Grid>

            {/* ACTIONS */}
            <Group mt="xl" wrap="wrap" gap="sm">

                {tournament.settings?.socialLink && (
                    <Button
                        component="a"
                        href={
                            tournament.settings.socialLink.startsWith('http')
                                ? tournament.settings.socialLink
                                : `https://${tournament.settings.socialLink}`
                        }
                        target="_blank"
                        leftSection={<IconBrandVk size={20} />}
                        variant="default"
                    >
                        Группа турнира
                    </Button>
                )}

                {isRegistrationOpen &&
                    !isRating &&
                    user &&
                    !isAlreadyParticipant &&
                    !isRequestSent && (
                        <Button color="green" onClick={onRegister}>
                            Подать заявку
                        </Button>
                    )}

                {isRequestSent && (
                    <Button disabled variant="light" color="yellow">
                        Заявка на рассмотрении
                    </Button>
                )}

                {isAlreadyParticipant && (
                    <Button disabled variant="light" color="blue">
                        Вы участник
                    </Button>
                )}

                {canManage && (
                    <Button
                        component={Link}
                        to={`/tournaments/${tournament.id}/manage`}
                        leftSection={<IconSettings size={18} />}
                        color="orange"
                        variant="outline"
                    >
                        Настроить турнир
                    </Button>
                )}
            </Group>
        </Paper>
    );
}
