// src/pages/tournaments/components/management/DeleteGamesModal.jsx
import { useState } from 'react';
import {
    Modal, Stack, SegmentedControl, NumberInput,
    Group, Button, Text, Alert,
} from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';

const MODES = [
    { label: 'Весь турнир',   value: 'ALL'   },
    { label: 'Один тур',      value: 'ROUND' },
    { label: 'Диапазон туров', value: 'RANGE' },
];

export default function DeleteGamesModal({ opened, onClose, onConfirm, loading }) {
    const [mode,      setMode]      = useState('ALL');
    const [fromRound, setFromRound] = useState(1);
    const [toRound,   setToRound]   = useState(1);

    const isValid = () => {
        if (mode === 'ALL')   return true;
        if (mode === 'ROUND') return fromRound >= 1;
        return fromRound >= 1 && toRound >= fromRound;
    };

    const handleConfirm = () => {
        onConfirm({ mode, fromRound, toRound });
    };

    const warningText = {
        ALL:   'Будут удалены ВСЕ игры турнира. Рассадка сбросится.',
        ROUND: `Будут удалены все игры тура ${fromRound}.`,
        RANGE: `Будут удалены все игры туров с ${fromRound} по ${toRound}.`,
    }[mode];

    return (
        <Modal opened={opened} onClose={onClose} title="Удаление игр" centered>
            <Stack gap="md">
                <SegmentedControl
                    fullWidth
                    data={MODES}
                    value={mode}
                    onChange={setMode}
                />

                {mode === 'ROUND' && (
                    <NumberInput
                        label="Номер тура"
                        min={1}
                        value={fromRound}
                        onChange={v => setFromRound(Number(v))}
                    />
                )}

                {mode === 'RANGE' && (
                    <Group grow>
                        <NumberInput
                            label="С тура"
                            min={1}
                            value={fromRound}
                            onChange={v => setFromRound(Number(v))}
                        />
                        <NumberInput
                            label="По тур"
                            min={fromRound}
                            value={toRound}
                            onChange={v => setToRound(Number(v))}
                        />
                    </Group>
                )}

                <Alert color="red" icon={<IconAlertTriangle size={16} />}>
                    <Text size="sm">{warningText}</Text>
                    <Text size="xs" c="dimmed" mt={4}>Это действие необратимо.</Text>
                </Alert>

                <Group justify="flex-end">
                    <Button variant="subtle" color="gray" onClick={onClose}>
                        Отмена
                    </Button>
                    <Button
                        color="red"
                        disabled={!isValid()}
                        loading={loading}
                        onClick={handleConfirm}
                    >
                        Удалить
                    </Button>
                </Group>
            </Stack>
        </Modal>
    );
}
