// src/pages/tournaments/components/games/SlotRow.jsx
import { Table, Select, NumberInput, Checkbox, Text } from '@mantine/core';
import { ROLES_DATA } from '../../constants/tournamentConstants';

export default function SlotRow({ slot, index, isRatingGame, participantsOptions, onUpdate }) {
    const roleConfig   = ROLES_DATA.find(r => r.value === slot.role);
    const roleTextColor = roleConfig ? `var(--mantine-color-${roleConfig.color}-6)` : 'inherit';

    return (
        <Table.Tr
            bg={slot.isFirstKilled ? 'rgba(34, 139, 230, 0.15)' : undefined}
        >
            <Table.Td>
                <Text size="sm" c="dimmed" ta="center">{slot.slotNumber}</Text>

            </Table.Td>

            <Table.Td>
                {isRatingGame ? (
                    <Select
                        size="xs"
                        data={participantsOptions}
                        value={slot.playerId ? String(slot.playerId) : null}
                        onChange={(val) => {
                            // Обновляем playerId
                            onUpdate(index, 'playerId', val);

                            // Находим выбранного участника и обновляем playerNickname
                            const selectedParticipant = participantsOptions.find(
                                option => option.value === val
                            );

                            if (selectedParticipant) {
                                onUpdate(index, 'playerNickname', selectedParticipant.label);
                            }
                        }}
                        searchable
                        placeholder="Выбрать..."
                        nothingFoundMessage="Не найден"
                        style={{ minWidth: 140 }}
                    />
                ) : (
                    <Text size="sm">{slot.playerNickname || '—'}</Text>
                )}
            </Table.Td>

            <Table.Td>
                <Select
                    size="xs"
                    data={ROLES_DATA}
                    value={slot.role}
                    onChange={val => onUpdate(index, 'role', val)}
                    styles={{ input: { color: roleTextColor, fontWeight: 500 } }}
                />
            </Table.Td>

            <Table.Td ta="center">
                <Checkbox
                    checked={slot.isFirstKilled}
                    onChange={e => onUpdate(index, 'isFirstKilled', e.currentTarget.checked)}
                    disabled={slot.role === 'mafia' || slot.role === 'don'}
                />
            </Table.Td>

            <Table.Td>
                <NumberInput
                    size="xs"
                    value={slot.extraPos}
                    onChange={val => onUpdate(index, 'extraPos', val)}
                    min={0} step={0.1} decimalScale={1}
                    style={{ width: 70 }}
                    styles={{ input: { color: 'var(--mantine-color-green-4)', textAlign: 'center' } }}
                />
            </Table.Td>

            <Table.Td>
                <NumberInput
                    size="xs"
                    value={slot.extraNeg}
                    onChange={val => onUpdate(index, 'extraNeg', val)}
                    min={0} step={0.1} decimalScale={1}
                    style={{ width: 70 }}
                    styles={{ input: { color: 'var(--mantine-color-red-4)', textAlign: 'center' } }}
                />
            </Table.Td>

            <Table.Td>
                <NumberInput
                    size="xs"
                    value={slot.penalty}
                    onChange={val => onUpdate(index, 'penalty', val)}
                    min={0} step={0.1} decimalScale={1}
                    style={{ width: 70 }}
                    styles={{ input: { color: 'var(--mantine-color-red-4)', textAlign: 'center' } }}
                />
            </Table.Td>
        </Table.Tr>
    );
}
