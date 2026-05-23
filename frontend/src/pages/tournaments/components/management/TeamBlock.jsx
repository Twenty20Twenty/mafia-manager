// src/pages/tournaments/components/management/TeamBlock.jsx
import { useState } from 'react';
import {
    Paper, Group, Text, TextInput, ActionIcon, Select, Stack,
    Badge, Tooltip, Button,
} from '@mantine/core';
import { IconPencil, IconCheck, IconX, IconTrash, IconUserMinus } from '@tabler/icons-react';
import { useThemeColors } from '../../../../hooks/useThemeColors';

/**
 * Карточка одной команды.
 *
 * Props:
 *   team            — { id, name, memberIds, memberNicknames }
 *
 *   teamSize        — максимальный размер команды (из настроек)
 *
 *   freeParticipants— [{ value: '123', label: 'Nickname' }] — участники без команды
 *
 *   onRename(id, newName)
 *
 *   onDelete(id)
 *
 *   onAddMember(teamId, userId)
 *
 *   onRemoveMember(userId)
 */
export default function TeamBlock({
    team, teamSize, freeParticipants,
    onRename, onDelete, onAddMember, onRemoveMember,
}) {
    const c = useThemeColors();
    const [editing,   setEditing]   = useState(false);
    const [nameInput, setNameInput] = useState(team.name);
    const [selected,  setSelected]  = useState(null);

    const slots = teamSize || 5;
    const isFull = (team.memberIds || []).length >= slots;

    const handleRename = () => {
        if (nameInput.trim() && nameInput !== team.name) {
            onRename(team.id, nameInput.trim());
        }
        setEditing(false);
    };

    const handleAdd = () => {
        if (!selected) return;
        onAddMember(team.id, Number(selected));
        setSelected(null);
    };

    return (
        <Paper
            withBorder p="md" radius="md"
            style={{ backgroundColor: c.surface3, borderColor: c.border }}
        >
            {/* Шапка: название + кнопки */}
            <Group justify="space-between" mb="sm" wrap="nowrap">
                {editing ? (
                    <Group gap="xs" style={{ flex: 1 }}>
                        <TextInput
                            size="xs"
                            value={nameInput}
                            onChange={e => setNameInput(e.currentTarget.value)}
                            onKeyDown={e => e.key === 'Enter' && handleRename()}
                            style={{ flex: 1 }}
                            autoFocus
                        />
                        <ActionIcon size="sm" color="green" variant="light" onClick={handleRename}>
                            <IconCheck size={14} />
                        </ActionIcon>
                        <ActionIcon size="sm" color="gray" variant="subtle"
                            onClick={() => { setNameInput(team.name); setEditing(false); }}>
                            <IconX size={14} />
                        </ActionIcon>
                    </Group>
                ) : (
                    <Group gap="xs" wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
                        <Text fw={700} size="sm" truncate>{team.name}</Text>
                        <Badge size="xs" variant="light" color="blue">
                            {(team.memberIds || []).length}/{slots}
                        </Badge>
                        <ActionIcon size="sm" variant="subtle" color="gray"
                            onClick={() => setEditing(true)}>
                            <IconPencil size={14} />
                        </ActionIcon>
                    </Group>
                )}

                <Tooltip label="Удалить команду" withArrow>
                    <ActionIcon size="sm" color="red" variant="subtle"
                        onClick={() => onDelete(team.id)}>
                        <IconTrash size={14} />
                    </ActionIcon>
                </Tooltip>
            </Group>

            {/* Список участников */}
            <Stack gap={4} mb="sm">
                {(team.memberNicknames || []).length === 0 ? (
                    <Text size="xs" c="dimmed">Участников нет</Text>
                ) : (
                    team.memberNicknames.map((nick, idx) => (
                        <Group key={team.memberIds[idx]} justify="space-between" wrap="nowrap">
                            <Text size="sm">{nick}</Text>
                            <ActionIcon
                                size="xs" color="red" variant="subtle"
                                onClick={() => onRemoveMember(team.memberIds[idx])}
                            >
                                <IconUserMinus size={12} />
                            </ActionIcon>
                        </Group>
                    ))
                )}
            </Stack>

            {/* Добавить участника */}
            {!isFull && freeParticipants.length > 0 && (
                <Group gap="xs" align="flex-end">
                    <Select
                        size="xs"
                        placeholder="Добавить участника..."
                        data={freeParticipants}
                        searchable
                        value={selected}
                        onChange={setSelected}
                        style={{ flex: 1 }}
                        nothingFoundMessage="Нет свободных участников"
                    />
                    <Button size="xs" onClick={handleAdd} disabled={!selected}>
                        +
                    </Button>
                </Group>
            )}

            {isFull && (
                <Text size="xs" c="dimmed" ta="center" mt="xs">Команда заполнена</Text>
            )}
        </Paper>
    );
}
