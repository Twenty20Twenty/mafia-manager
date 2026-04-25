// src/pages/tournaments/components/games/DeleteGameModal.jsx
import { Modal, Stack, Text, Group, Button, Badge } from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';

/**
 * Модал подтверждения удаления одной игры.
 *
 * Props:
 *   opened  — bool
 *   onClose — закрыть без удаления
 *   onConfirm — подтвердить удаление
 *   game    — объект игры { id, table, tableNumber, round, judgeName, status }
 *   loading — bool
 */
export default function DeleteRatingGameModal({ opened, onClose, onConfirm, game, loading }) {
    if (!game) return null;

    const tableNum  = game.table ?? game.tableNumber ?? '?';
    const roundNum  = game.round ?? '—';
    const judgeName = game.judgeName || 'Не назначен';
    const isFinal   = game.stage === 'final_round';

    return (
        <Modal
            opened={opened}
            onClose={onClose}
            title="Удаление игры"
            centered
            size="sm"
        >
            <Stack gap="md">
                <Group gap="xs" wrap="nowrap">
                    <IconAlertTriangle size={20} color="var(--mantine-color-red-5)" style={{ flexShrink: 0 }} />
                    <Text size="sm">Вы уверены, что хотите удалить эту игру?</Text>
                </Group>

                <Stack gap="xs" p="sm" bg="var(--mantine-color-dark-6)" style={{ borderRadius: 8 }}>
                    <Group gap="xs">
                        <Text size="xs" c="dimmed">Стол:</Text>
                        <Text size="xs" fw={600}>{tableNum}</Text>
                        {isFinal && <Badge size="xs" color="grape" variant="light">Финал</Badge>}
                    </Group>
                    <Group gap="xs">
                        <Text size="xs" c="dimmed">Тур:</Text>
                        <Text size="xs" fw={600}>{roundNum}</Text>
                    </Group>
                    <Group gap="xs">
                        <Text size="xs" c="dimmed">Судья:</Text>
                        <Text size="xs" fw={600}>{judgeName}</Text>
                    </Group>
                </Stack>

                <Text size="xs" c="dimmed">
                    Это действие необратимо. Все данные протокола будут удалены.
                </Text>

                <Group justify="flex-end">
                    <Button variant="subtle" color="gray" onClick={onClose} disabled={loading}>
                        Отмена
                    </Button>
                    <Button color="red" onClick={onConfirm} loading={loading}>
                        Удалить
                    </Button>
                </Group>
            </Stack>
        </Modal>
    );
}
