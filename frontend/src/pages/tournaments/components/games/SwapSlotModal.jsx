// src/pages/tournaments/components/games/SwapSlotModal.jsx
import { useState, useEffect, useMemo } from 'react';
import {
    Modal, Stack, Select, Group, Button, Text, Divider, SegmentedControl,
} from '@mantine/core';

/**
 * Модал для замены игрока в слоте незавершённой игры.
 *
 * Props:
 *   opened             — bool
 *   onClose()          — закрыть
 *   onConfirm({ oldUserId, newUserId, slotNumber }) — подтвердить
 *   slots              — слоты игры [{ slotNumber, playerId, playerNickname }]
 *   participantOptions — участники: [{ value, label }] ИЛИ [{ id, nickname }] — нормализуем внутри
 *   loading            — bool
 */
export default function SwapSlotModal({ opened, onClose, onConfirm, slots = [], participantOptions = [], loading }) {
    const [mode,       setMode]       = useState('by-slot');
    const [slotNumber, setSlotNumber] = useState(null);
    const [oldUserId,  setOldUserId]  = useState(null);
    const [newUserId,  setNewUserId]  = useState(null);

    // Сбрасываем стейт каждый раз при открытии модала
    useEffect(() => {
        if (opened) {
            setMode('by-slot');
            setSlotNumber(null);
            setOldUserId(null);
            setNewUserId(null);
        }
    }, [opened]);

    // Нормализуем participantOptions — принимаем оба формата:
    // [{ value, label }]  — уже готово
    // [{ id, nickname }]  — объекты участников, нужно конвертировать
    const normalizedOptions = useMemo(() =>
            participantOptions.map(p =>
                p.value !== undefined
                    ? p
                    : { value: String(p.id), label: p.nickname }
            ),
        [participantOptions]
    );

    // Опции слотов для Select
    const slotOptions = slots.map(s => ({
        value: String(s.slotNumber),
        label: `Слот ${s.slotNumber} — ${s.playerNickname || 'пусто'}`,
    }));

    // Только игроки, которые уже сидят в игре
    const occupiedUserIds = new Set(
        slots.map(s => s.playerId != null ? String(s.playerId) : null).filter(Boolean)
    );
    const occupiedOptions = normalizedOptions.filter(o => occupiedUserIds.has(o.value));

    const isValid = mode === 'by-slot' ? slotNumber != null : oldUserId != null;

    const handleConfirm = () => {
        onConfirm({
            slotNumber: mode === 'by-slot'   ? Number(slotNumber) : null,
            oldUserId:  mode === 'by-player' ? Number(oldUserId)  : null,
            newUserId:  newUserId ? Number(newUserId) : null,
        });
    };

    return (
        <Modal opened={opened} onClose={onClose} title="Замена игрока в слоте" centered>
            <Stack gap="md">

                {/* Режим: по слоту или по игроку */}
                <SegmentedControl
                    fullWidth
                    value={mode}
                    onChange={setMode}
                    data={[
                        { label: 'По номеру слота', value: 'by-slot'   },
                        { label: 'По игроку',       value: 'by-player' },
                    ]}
                />

                {/* Выбор слота ИЛИ игрока которого убираем */}
                {mode === 'by-slot' ? (
                    <Select
                        label="Слот"
                        placeholder="Выберите слот"
                        data={slotOptions}
                        value={slotNumber}
                        onChange={setSlotNumber}
                    />
                ) : (
                    <Select
                        label="Убрать игрока"
                        placeholder="Выберите игрока из игры"
                        data={occupiedOptions}
                        searchable
                        value={oldUserId}
                        onChange={setOldUserId}
                    />
                )}

                <Divider label="Заменить на" labelPosition="center" />

                {/* Новый игрок (необязательно — null = освободить слот) */}
                <Select
                    label="Новый игрок"
                    description="Оставьте пустым чтобы освободить слот"
                    placeholder="Выберите игрока"
                    data={normalizedOptions}
                    searchable
                    clearable
                    value={newUserId}
                    onChange={setNewUserId}
                />

                <Group justify="flex-end" mt="xs">
                    <Button variant="subtle" color="gray" onClick={onClose} disabled={loading}>
                        Отмена
                    </Button>
                    <Button
                        loading={loading}
                        disabled={!isValid}
                        onClick={handleConfirm}
                    >
                        Применить
                    </Button>
                </Group>
            </Stack>
        </Modal>
    );
}
