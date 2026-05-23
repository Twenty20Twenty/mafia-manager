// src/pages/tournaments/components/profile/ParticipantsList.jsx
import { useState, useEffect } from 'react';
import { Paper, Title, Divider, Stack, Text, Group, Avatar, Badge, Center, Loader } from '@mantine/core';
import { Link } from 'react-router-dom';
import { IconUsers } from '@tabler/icons-react';
import api from '../../../../api/axios';
import { teamsApi } from '../../../../api/teams';
import { useThemeColors } from '../../../../hooks/useThemeColors';

function IndividualList({ participants, c }) {
    return (
        <Stack gap="xs">
            {participants.length > 0 ? (
                participants.map((participant, index) => (
                    <Paper key={participant.id} withBorder p="xs" radius="sm"
                        style={{ backgroundColor: c.surface3 }}>
                        <Group wrap="nowrap">
                            <Text c="dimmed" w={24} ta="center" fw={700} size="sm" style={{ flexShrink: 0 }}>
                                {index + 1}
                            </Text>
                            <Group
                                component={Link} to={`/players/${participant.id}`}
                                style={{ textDecoration: 'none', color: 'inherit', cursor: 'pointer', minWidth: 0 }}
                                gap="sm" wrap="nowrap"
                            >
                                <Avatar src={participant.avatarUrl} size="sm" radius="xl" color="brandRed" style={{ flexShrink: 0 }}>
                                    {participant.nickname?.[0]}
                                </Avatar>
                                <div style={{ minWidth: 0 }}>
                                    <Text fw={500} size="sm" lh={1.2} truncate>{participant.nickname}</Text>
                                    {participant.clubName && (
                                        <Text size="xs" c="dimmed" lh={1.2} truncate>{participant.clubName}</Text>
                                    )}
                                </div>
                            </Group>
                        </Group>
                    </Paper>
                ))
            ) : (
                <Text c="dimmed">Список участников пуст</Text>
            )}
        </Stack>
    );
}

function TeamCard({ team, index, c }) {
    return (
        <Paper withBorder p="md" radius="md" style={{ backgroundColor: c.surface3 }}>
            <Group justify="space-between" mb="xs" wrap="nowrap">
                <Group gap="xs" wrap="nowrap">
                    <Text c="dimmed" fw={700} size="sm" w={24} ta="center">{index + 1}</Text>
                    <IconUsers size={16} style={{ opacity: 0.5, flexShrink: 0 }} />
                    <Text fw={700} size="sm" truncate>{team.name}</Text>
                </Group>
                <Badge size="sm" variant="light" color="blue" style={{ flexShrink: 0 }}>
                    {(team.memberNicknames || []).length} игр.
                </Badge>
            </Group>
            <Stack gap={4} pl={40}>
                {(team.memberNicknames || []).map((nick, idx) => (
                    <Group key={team.memberIds[idx]} gap="sm" wrap="nowrap">
                        <Avatar size="xs" radius="xl" color="brandRed">{nick?.[0]}</Avatar>
                        <Text
                            size="xs"
                            component={Link}
                            to={`/players/${team.memberIds[idx]}`}
                            style={{ textDecoration: 'none', color: 'inherit' }}
                        >
                            {nick}
                        </Text>
                    </Group>
                ))}
                {(team.memberNicknames || []).length === 0 && (
                    <Text size="xs" c="dimmed">Участников нет</Text>
                )}
            </Stack>
        </Paper>
    );
}

export default function ParticipantsList({ participants, isTeamTournament, tournamentId }) {
    const c = useThemeColors();
    const [teams, setTeams]     = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!isTeamTournament || !tournamentId) return;
        setLoading(true);
        teamsApi.getTeams(tournamentId)
            .then(setTeams)
            .catch(console.error)
            .finally(() => setLoading(false));
    }, [isTeamTournament, tournamentId]);

    return (
        <Paper withBorder p={{ base: 'md', sm: 'xl' }} style={{ backgroundColor: c.surface2 }}>
            <Title order={4} mb="md">
                {isTeamTournament ? `Команды (${teams.length})` : `Участники (${participants.filter(p => p.status === 'approved').length})`}
            </Title>
            <Divider mb="md" />

            {isTeamTournament ? (
                loading ? (
                    <Center py="md"><Loader color="brandRed" size="sm" /></Center>
                ) : teams.length === 0 ? (
                    <Text c="dimmed">Команды ещё не сформированы</Text>
                ) : (
                    <Stack gap="sm">
                        {teams.map((team, idx) => (
                            <TeamCard key={team.id} team={team} index={idx} c={c} />
                        ))}
                    </Stack>
                )
            ) : (
                <IndividualList
                    participants={participants.filter(p => p.status === 'approved')}
                    c={c}
                />
            )}
        </Paper>
    );
}
