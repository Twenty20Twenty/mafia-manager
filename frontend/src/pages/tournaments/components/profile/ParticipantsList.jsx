// src/pages/tournaments/components/profile/ParticipantsList.jsx
import { Paper, Title, Divider, Stack, Text, Group, Avatar } from '@mantine/core';
import { Link } from 'react-router-dom';
import { useThemeColors } from '../../../../hooks/useThemeColors';

export default function ParticipantsList({ participants, isTeamTournament }) {
    const c = useThemeColors();

    return (
        <Paper withBorder p={{ base: 'md', sm: 'xl' }} style={{ backgroundColor: c.surface2 }}>
            <Title order={4} mb="md">
                {isTeamTournament ? 'Список команд (в разработке)' : 'Список участников'}
            </Title>
            <Divider mb="md" />

            {isTeamTournament ? (
                <Text c="dimmed">Отображение командных составов скоро появится</Text>
            ) : (
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
            )}
        </Paper>
    );
}
