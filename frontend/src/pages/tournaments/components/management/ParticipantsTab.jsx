// src/pages/tournaments/components/management/ParticipantsTab.jsx
import { Stack, Paper, Title, Group, Select, Button, Avatar, Text, ActionIcon, Divider } from '@mantine/core';
import { IconUserPlus, IconCheck, IconX } from '@tabler/icons-react';
import { useThemeColors } from '../../../../hooks/useThemeColors';
import TeamsManager from './TeamsManager';

export default function ParticipantsTab({
    isRating, isTeam,
    tournamentId, teamSize, maxParticipants,
    approvedParticipants, pendingRequests,
    addPlayerOptions, selectedPlayerId, setSelectedPlayerId,
    onAddManual, onAccept, onReject, onRemove,
    }) {
    const c = useThemeColors();

    return (
        <Stack gap="lg">
            {/* Добавить участника */}
            <Paper withBorder p="md" style={{ backgroundColor: c.surface2 }}>
                <Title order={4} mb="md">Добавить участника</Title>
                <Group>
                    <Select
                        placeholder="Поиск по никнейму..."
                        data={addPlayerOptions}
                        searchable
                        value={selectedPlayerId}
                        onChange={setSelectedPlayerId}
                        style={{ flex: 1 }}
                        nothingFoundMessage="Игрок не найден"
                    />
                    <Button onClick={onAddManual} leftSection={<IconUserPlus size={16} />}>Добавить</Button>
                </Group>
            </Paper>

            {/* Заявки на участие (только для не-рейтинговых) */}
            {!isRating && pendingRequests.length > 0 && (
                <Paper
                    withBorder p="md"
                    style={{ backgroundColor: c.surface2, borderColor: 'var(--mantine-color-orange-5)' }}
                >
                    <Title order={5} mb="sm">Заявки на участие ({pendingRequests.length})</Title>
                    <Stack gap="xs">
                        {pendingRequests.map(p => (
                            <Group key={p.id} p="xs" justify="space-between"
                                   style={{ borderRadius: 6, backgroundColor: c.surface3 }}>
                                <Group gap="sm">
                                    <Avatar src={p.avatarUrl} size="sm" />
                                    <Text size="sm">
                                        {p.nickname}
                                        {p.clubName && (
                                            <Text span c="dimmed" size="xs"> ({p.clubName})</Text>
                                        )}
                                    </Text>
                                </Group>
                                <Group gap="xs">
                                    <ActionIcon color="green" variant="filled" onClick={() => onAccept(p.id)}>
                                        <IconCheck size={16} />
                                    </ActionIcon>
                                    <ActionIcon color="red" variant="light" onClick={() => onReject(p.id)}>
                                        <IconX size={16} />
                                    </ActionIcon>
                                </Group>
                            </Group>
                        ))}
                    </Stack>
                </Paper>
            )}

            {/* Список участников */}
            <Paper withBorder p="md" style={{ backgroundColor: c.surface2 }}>
                <Title order={5} mb="sm">Список участников ({approvedParticipants.length})</Title>
                <Stack gap="xs">
                    {approvedParticipants.map((p, idx) => (
                        <Group key={p.id} p="xs" justify="space-between"
                               style={{ borderRadius: 6, backgroundColor: c.surface3 }}>
                            <Group gap="sm">
                                <Text c="dimmed" size="xs">{idx + 1}</Text>
                                <Avatar src={p.avatarUrl} size="sm" />
                                <Text size="sm">
                                    {p.nickname}
                                    {p.clubName && (
                                        <Text span c="dimmed" size="xs"> ({p.clubName})</Text>
                                    )}
                                </Text>
                            </Group>
                            <ActionIcon color="red" variant="subtle" size="sm" onClick={() => onRemove(p.id)}>
                                <IconX size={14} />
                            </ActionIcon>
                        </Group>
                    ))}
                </Stack>
            </Paper>

            {/* Блок команд — только для командных турниров */}
            {isTeam && tournamentId && (
                <>
                    <Divider label="Формирование команд" labelPosition="center" />
                    <TeamsManager
                        tournamentId={tournamentId}
                        teamSize={teamSize}
                        maxParticipants={maxParticipants}
                        approvedParticipants={approvedParticipants}
                    />
                </>
            )}
        </Stack>
    );
}
