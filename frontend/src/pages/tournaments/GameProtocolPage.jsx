// src/pages/tournaments/GameProtocolPage.jsx
import { useParams, Link } from 'react-router-dom';
import {
    Container, Title, Paper, Group, Button, Table, SegmentedControl,
    Text, Switch, Center, Loader, Alert, Stack, Box, Select,
    NumberInput, Checkbox, Divider, Badge
} from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { IconArrowLeft, IconDeviceFloppy, IconCalendar, IconAlertCircle } from '@tabler/icons-react';
import { useMediaQuery } from '@mantine/hooks';

import { useGameProtocol } from './hooks/useGameProtocol';
import { ROLES_DATA, ROLE_DISTRIBUTION } from './constants/tournamentConstants';
import NewGameForm     from './components/games/NewGameForm';
import SlotRow         from './components/games/SlotRow';
import BestMoveSection from './components/games/BestMoveSection';
import { useThemeColors } from '../../hooks/useThemeColors';

// ─── Мобильная карточка слота ────────────────────────────────────────
function MobileSlotCard({ slot, index, isRatingGame, participantsOptions, onUpdate, c }) {
    const roleConfig    = ROLES_DATA.find(r => r.value === slot.role);
    const roleTextColor = roleConfig ? `var(--mantine-color-${roleConfig.color}-6)` : 'inherit';

    return (
        <Paper withBorder radius="md" p="sm" style={{ backgroundColor: c.surface3 }}>
            <Group justify="space-between" mb="xs" wrap="nowrap">
                <Badge variant="outline" color="gray" size="lg" style={{ minWidth: 36 }}>
                    {slot.slotNumber}
                </Badge>
                {isRatingGame ? (
                    <Select
                        size="xs"
                        data={participantsOptions}
                        value={slot.playerId ? String(slot.playerId) : null}
                        onChange={(val) => {
                            onUpdate(index, 'playerId', val);
                            const p = participantsOptions.find(o => o.value === val);
                            if (p) onUpdate(index, 'playerNickname', p.label);
                        }}
                        searchable
                        placeholder="Выбрать..."
                        nothingFoundMessage="Не найден"
                        style={{ flex: 1 }}
                    />
                ) : (
                    <Text size="sm" fw={500} style={{ flex: 1, textAlign: 'right' }}>
                        {slot.playerNickname || '—'}
                    </Text>
                )}
            </Group>

            <Group grow gap="xs" mb="xs">
                <Select
                    size="xs" label="Роль"
                    data={ROLES_DATA}
                    value={slot.role}
                    onChange={val => onUpdate(index, 'role', val)}
                    styles={{ input: { color: roleTextColor, fontWeight: 500 } }}
                />
                <Stack gap={4}>
                    <Text size="xs" c="dimmed">ПУ</Text>
                    <Checkbox
                        checked={slot.isFirstKilled}
                        onChange={e => onUpdate(index, 'isFirstKilled', e.currentTarget.checked)}
                        disabled={slot.role === 'mafia' || slot.role === 'don'}
                        mt={4}
                    />
                </Stack>
            </Group>

            <Group grow gap="xs">
                <NumberInput
                    size="xs" label="+Доп"
                    value={slot.extraPos}
                    onChange={val => onUpdate(index, 'extraPos', val)}
                    min={0} step={0.1} decimalScale={1}
                    styles={{ input: { color: 'var(--mantine-color-green-4)', textAlign: 'center' } }}
                />
                <NumberInput
                    size="xs" label="-Доп"
                    value={slot.extraNeg}
                    onChange={val => onUpdate(index, 'extraNeg', val)}
                    min={0} step={0.1} decimalScale={1}
                    styles={{ input: { color: 'var(--mantine-color-red-4)', textAlign: 'center' } }}
                />
                <NumberInput
                    size="xs" label="Штраф"
                    value={slot.penalty}
                    onChange={val => onUpdate(index, 'penalty', val)}
                    min={0} step={0.1} decimalScale={1}
                    styles={{ input: { color: 'var(--mantine-color-red-4)', textAlign: 'center' } }}
                />
            </Group>
        </Paper>
    );
}

// ─── Главный компонент ───────────────────────────────────────────────
export default function GameProtocolPage({ isNewRatingGame = false }) {
    const { id: tournamentId, gameId } = useParams();
    const isMobile = useMediaQuery('(max-width: 768px)');
    const c = useThemeColors();

    const {
        pageState, errorMsg,
        game, slots, winner, setWinner,
        isDraft, setIsDraft,
        gameDate, setGameDate,
        bestMove, setBestMove,
        participantsOptions,
        saving,
        isRatingGame, roleCounts,
        handleGameCreated,
        updateSlot,
        handleSave,
    } = useGameProtocol({ tournamentId, gameId, isNewRatingGame });

    if (pageState === 'form') {
        return <NewGameForm tournamentId={tournamentId} onGameCreated={handleGameCreated} />;
    }

    if (pageState === 'loading') {
        return <Container py="xl"><Center mt="xl"><Loader color="brandRed" size="xl" /></Center></Container>;
    }

    if (pageState === 'error') {
        return (
            <Container py="xl">
                <Alert icon={<IconAlertCircle />} color="red" title="Ошибка загрузки">{errorMsg}</Alert>
                <Button mt="md" variant="subtle" component={Link} to={`/tournaments/${tournamentId}`}>
                    Вернуться к турниру
                </Button>
            </Container>
        );
    }

    const pageTitle = isNewRatingGame ? 'Новая игра (Рейтинг)' : `Протокол игры #${game?.id ?? ''}`;

    // ── Десктоп ──────────────────────────────────────────────────────
    if (!isMobile) {
        return (
            <Container size="xl" py="xl">
                <Group justify="space-between" mb="md">
                    <Button component={Link} to={`/tournaments/${tournamentId}`} variant="subtle" color="gray" leftSection={<IconArrowLeft size={16} />}>
                        Назад к турниру
                    </Button>
                    <Title order={3}>{pageTitle}</Title>
                    <Group>
                        <Switch label="Черновик" checked={isDraft} onChange={e => setIsDraft(e.currentTarget.checked)} size="md" color="orange" />
                        <Button leftSection={<IconDeviceFloppy size={18} />} color="green" onClick={handleSave} loading={saving}>
                            Сохранить
                        </Button>
                    </Group>
                </Group>

                {isRatingGame && (
                    <Paper p="md" withBorder mb="md" radius="md" style={{ backgroundColor: c.surface2 }}>
                        <Group>
                            <DatePickerInput
                                label="Дата проведения" placeholder="Выберите дату"
                                leftSection={<IconCalendar size={16} />}
                                value={gameDate} onChange={setGameDate} required
                            />
                            <Text size="sm" c="dimmed" mt={25}>
                                Судья: <b>{game?.judgeName || '—'}</b>
                            </Text>
                        </Group>
                    </Paper>
                )}

                <Paper p="md" withBorder mb="xl" radius="md" style={{ backgroundColor: c.surface2 }}>
                    <Group justify="center">
                        <Text fw={700}>Результат игры:</Text>
                        <SegmentedControl
                            value={winner}
                            onChange={setWinner}
                            color={winner === 'red' ? 'red' : winner === 'black' ? 'dark' : 'gray'}
                            data={[
                                { label: 'Мирные', value: 'red'   },
                                { label: 'Ничья',  value: 'draw'  },
                                { label: 'Мафия',  value: 'black' },
                            ]}
                        />
                    </Group>
                </Paper>

                {/* Счётчик ролей */}
                <Paper p="sm" withBorder mb="md" radius="md" style={{ backgroundColor: c.surface0 }}>
                    <Group justify="center" gap="xl">
                        {ROLES_DATA.map(r => (
                            <Text
                                key={r.value} size="sm"
                                c={roleCounts[r.value] !== undefined && roleCounts[r.value] !== ROLE_DISTRIBUTION[r.value] ? 'red' : 'dimmed'}
                            >
                                {r.label}: <b>{roleCounts[r.value] ?? 0}</b>/{ROLE_DISTRIBUTION[r.value] ?? '—'}
                            </Text>
                        ))}
                    </Group>
                </Paper>

                <Paper withBorder radius="md" mb="xl" style={{ overflowX: 'auto', backgroundColor: c.surface2 }}>
                    <Table verticalSpacing="xs" horizontalSpacing="sm" highlightOnHover
                        styles={{ thead: { backgroundColor: c.tableHeader } }}>
                        <Table.Thead>
                            <Table.Tr>
                                <Table.Th style={{ width: 40 }}>#</Table.Th>
                                <Table.Th>{isRatingGame ? 'Игрок' : 'Никнейм'}</Table.Th>
                                <Table.Th>Роль</Table.Th>
                                <Table.Th title="Первое убийство">ПУ</Table.Th>
                                <Table.Th title="Доп. баллы +">+ Доп</Table.Th>
                                <Table.Th title="Доп. баллы -">- Доп</Table.Th>
                                <Table.Th title="Дисц. штраф">Штраф</Table.Th>
                            </Table.Tr>
                        </Table.Thead>
                        <Table.Tbody>
                            {slots.map((slot, index) => (
                                <SlotRow
                                    key={slot.slotNumber}
                                    slot={slot}
                                    index={index}
                                    isRatingGame={isRatingGame}
                                    participantsOptions={participantsOptions}
                                    onUpdate={updateSlot}
                                />
                            ))}
                        </Table.Tbody>
                    </Table>
                </Paper>

                <BestMoveSection bestMove={bestMove} setBestMove={setBestMove} slots={slots} />
            </Container>
        );
    }

    // ── Мобилка ──────────────────────────────────────────────────────
    return (
        <Container size="xl" py="sm" px="xs">
            <Group justify="space-between" mb="sm" wrap="nowrap">
                <Button component={Link} to={`/tournaments/${tournamentId}`}
                    variant="subtle" color="gray" size="xs" leftSection={<IconArrowLeft size={14} />} px="xs">
                    Назад
                </Button>
                <Text fw={700} size="sm" ta="center" style={{ flex: 1 }} lineClamp={1}>{pageTitle}</Text>
                <Button leftSection={<IconDeviceFloppy size={16} />} color="green" size="xs" onClick={handleSave} loading={saving}>
                    Сохранить
                </Button>
            </Group>

            <Paper p="xs" withBorder mb="sm" radius="md" style={{ backgroundColor: c.surface2 }}>
                <Group justify="space-between">
                    <Switch label="Черновик" checked={isDraft} onChange={e => setIsDraft(e.currentTarget.checked)} size="sm" color="orange" />
                    {isRatingGame && <Text size="xs" c="dimmed">Судья: <b>{game?.judgeName || '—'}</b></Text>}
                </Group>
            </Paper>

            {isRatingGame && (
                <Paper p="xs" withBorder mb="sm" radius="md" style={{ backgroundColor: c.surface2 }}>
                    <DatePickerInput
                        label="Дата проведения" placeholder="Выберите дату"
                        leftSection={<IconCalendar size={16} />}
                        value={gameDate} onChange={setGameDate} required size="sm"
                    />
                </Paper>
            )}

            <Paper p="sm" withBorder mb="sm" radius="md" style={{ backgroundColor: c.surface2 }}>
                <Stack gap="xs" align="center">
                    <Text fw={700} size="sm">Результат игры:</Text>
                    <SegmentedControl
                        fullWidth value={winner} onChange={setWinner}
                        color={winner === 'red' ? 'red' : winner === 'black' ? 'dark' : 'gray'}
                        data={[
                            { label: 'Мирные', value: 'red'   },
                            { label: 'Ничья',  value: 'draw'  },
                            { label: 'Мафия',  value: 'black' },
                        ]}
                    />
                </Stack>
            </Paper>

            {/* Счётчик ролей */}
            <Paper p="xs" withBorder mb="sm" radius="md" style={{ backgroundColor: c.surface0 }}>
                <Group justify="center" gap="sm" wrap="wrap">
                    {ROLES_DATA.map(r => (
                        <Text
                            key={r.value} size="xs"
                            c={roleCounts[r.value] !== undefined && roleCounts[r.value] !== ROLE_DISTRIBUTION[r.value] ? 'red' : 'dimmed'}
                        >
                            {r.label}: <b>{roleCounts[r.value] ?? 0}</b>/{ROLE_DISTRIBUTION[r.value] ?? '—'}
                        </Text>
                    ))}
                </Group>
            </Paper>

            {/* Слоты */}
            <Stack gap="xs" mb="md">
                {slots.map((slot, index) => (
                    <MobileSlotCard
                        key={slot.slotNumber}
                        slot={slot}
                        index={index}
                        isRatingGame={isRatingGame}
                        participantsOptions={participantsOptions}
                        onUpdate={updateSlot}
                        c={c}
                    />
                ))}
            </Stack>

            <BestMoveSection bestMove={bestMove} setBestMove={setBestMove} slots={slots} />

            <Button fullWidth leftSection={<IconDeviceFloppy size={18} />} color="green"
                onClick={handleSave} loading={saving} mt="sm" mb="xl">
                Сохранить протокол
            </Button>
        </Container>
    );
}
