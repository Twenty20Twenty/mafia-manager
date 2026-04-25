// src/pages/tournaments/components/management/FinalistsEditor.jsx
import { useState } from 'react';
import {
    Stack, Paper, Title, Text, Group, Button, Badge,
    Avatar, ActionIcon, Select, Divider,
} from '@mantine/core';
import { IconX, IconArrowUp, IconArrowDown, IconLock, IconLockOpen, IconWand } from '@tabler/icons-react';
import { useThemeColors } from '../../../../hooks/useThemeColors';

export default function FinalistsEditor({
                                            finalists = [],
                                            allParticipants = [],
                                            locked,
                                            onSave,
                                            onAutoFill,
                                            onLock,
                                            onUnlock,
                                            loading,
                                        }) {
    const c = useThemeColors();
    const [draft,    setDraft]    = useState(() => finalists.map(p => ({ ...p })));
    const [selected, setSelected] = useState(null);

    const alreadyInList = new Set(draft.map(p => String(p.id)));
    const options = allParticipants.filter(o => !alreadyInList.has(o.value));

    const addPlayer = () => {
        if (!selected) return;
        const opt = allParticipants.find(o => o.value === selected);
        if (!opt || draft.length >= 10) return;
        setDraft(prev => [...prev, { id: Number(opt.value), nickname: opt.label }]);
        setSelected(null);
    };

    const removePlayer = (id) => setDraft(prev => prev.filter(p => p.id !== id));

    const moveUp = (idx) => {
        if (idx === 0) return;
        setDraft(prev => { const next = [...prev]; [next[idx - 1], next[idx]] = [next[idx], next[idx - 1]]; return next; });
    };

    const moveDown = (idx) => {
        setDraft(prev => {
            if (idx >= prev.length - 1) return prev;
            const next = [...prev]; [next[idx], next[idx + 1]] = [next[idx + 1], next[idx]]; return next;
        });
    };

    const handleSave = (withLock = false) => onSave(draft.map(p => p.id), withLock);

    return (
        <Paper withBorder p="md" style={{ backgroundColor: c.surface2 }}>
            <Group justify="space-between" mb="md" wrap="nowrap">
                <div>
                    <Title order={4}>Финалисты</Title>
                    <Text size="sm" c="dimmed">
                        {draft.length}/10 игроков
                        {locked && <Badge ml="xs" size="xs" color="orange">Зафиксировано</Badge>}
                    </Text>
                </div>
                <Group gap="xs">
                    <Button size="xs" variant="light" leftSection={<IconWand size={14} />}
                            onClick={onAutoFill} loading={loading} disabled={locked}>
                        Из лидерборда
                    </Button>
                    {locked ? (
                        <Button size="xs" variant="outline" color="orange" leftSection={<IconLockOpen size={14} />}
                                onClick={onUnlock} loading={loading}>
                            Разблокировать
                        </Button>
                    ) : (
                        <Button size="xs" color="orange" variant="outline" leftSection={<IconLock size={14} />}
                                onClick={onLock} loading={loading} disabled={draft.length === 0}>
                            Зафиксировать
                        </Button>
                    )}
                </Group>
            </Group>

            <Stack gap="xs" mb="md">
                {draft.length === 0 && (
                    <Text size="sm" c="dimmed" ta="center" py="sm">
                        Список пуст — добавьте игроков или заполните из лидерборда
                    </Text>
                )}
                {draft.map((player, idx) => (
                    <Group key={player.id} p="xs" justify="space-between"
                           style={{ borderRadius: 6, backgroundColor: c.surface3 }}>
                        <Group gap="sm">
                            <Text size="xs" c="dimmed" w={18} ta="right">{idx + 1}.</Text>
                            <Avatar src={player.avatarUrl} size="sm" radius="xl" />
                            <Text size="sm">{player.nickname}</Text>
                        </Group>
                        <Group gap={4}>
                            <ActionIcon size="xs" variant="subtle" color="gray" onClick={() => moveUp(idx)} disabled={locked || idx === 0}>
                                <IconArrowUp size={12} />
                            </ActionIcon>
                            <ActionIcon size="xs" variant="subtle" color="gray" onClick={() => moveDown(idx)} disabled={locked || idx === draft.length - 1}>
                                <IconArrowDown size={12} />
                            </ActionIcon>
                            <ActionIcon size="xs" variant="subtle" color="red" onClick={() => removePlayer(player.id)} disabled={locked}>
                                <IconX size={12} />
                            </ActionIcon>
                        </Group>
                    </Group>
                ))}
            </Stack>

            {!locked && (
                <>
                    <Divider mb="sm" />
                    <Group align="flex-end">
                        <Select
                            placeholder="Добавить игрока"
                            data={options}
                            searchable
                            value={selected}
                            onChange={setSelected}
                            style={{ flex: 1 }}
                            disabled={draft.length >= 10}
                        />
                        <Button onClick={addPlayer} disabled={!selected || draft.length >= 10}>Добавить</Button>
                    </Group>
                    {draft.length >= 10 && (
                        <Text size="xs" c="dimmed" mt={4}>Достигнут максимум 10 финалистов</Text>
                    )}
                </>
            )}

            {!locked && (
                <Group justify="flex-end" mt="md">
                    <Button loading={loading} onClick={() => handleSave(false)} disabled={draft.length === 0}>
                        Сохранить
                    </Button>
                </Group>
            )}
        </Paper>
    );
}
