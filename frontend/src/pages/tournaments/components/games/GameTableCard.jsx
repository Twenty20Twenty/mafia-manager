// src/pages/tournaments/components/games/GameTableCard.jsx
import { useState }          from 'react';
import { Paper, Group, Badge, Text, ActionIcon, Table, Tooltip, Stack } from '@mantine/core';
import { IconEdit, IconRefresh, IconTrash }  from '@tabler/icons-react';
import { Link }              from 'react-router-dom';
import { resolveCanEdit }    from '../../utils/tournamentUtils';
import SwapSlotModal         from './SwapSlotModal';
import DeleteRatingGameModal from './DeleteRatingGameModal.jsx';
import { swapSlot }          from '../../../../api/tournaments_additions';
import { gamesApi }          from '../../../../api/games.js';
import { useThemeColors }    from '../../../../hooks/useThemeColors';

const ROLE_CONFIG = {
    civilian: { color: 'red',    label: 'Мир'  },
    sheriff:  { color: 'yellow', label: 'Шер'  },
    mafia:    { color: 'blue',   label: 'Маф'  },
    don:      { color: 'grape',  label: 'Дон'  },
};

const formatPoints = (value) =>
    Number.isInteger(value)
        ? value.toString()
        : value.toFixed(2).replace(/\.?0+$/, '');

function SlotPointsTooltip({ slot, game, isDraw, isFinal, coeff }) {
    if (isDraw) {
        return slot.penalty > 0 ? (
            <Stack gap={2}>
                <Text size="xs" c="dimmed">Ничья (0 баллов)</Text>
                <Text size="xs" c="red">Дисциплинарный: -{(slot.penalty * coeff).toFixed(2)}</Text>
            </Stack>
        ) : (
            <Text size="xs" c="dimmed">Ничья (0 баллов)</Text>
        );
    }

    const lhPoints = slot.isFirstKilled && game.bestMove?.authorId === slot.playerId
        ? Number(game.bestMove?.points ?? 0) : 0;

    return (
        <Stack gap={2}>
            {isFinal && <Text size="xs" c="grape" fw={700}>Коэффициент финала: x{coeff}</Text>}
            {lhPoints > 0 && <Text size="xs" c="orange">ЛХ: +{formatPoints(lhPoints * coeff)}</Text>}
            {slot.extraPos > 0 && <Text size="xs" c="green">Доп: +{formatPoints(slot.extraPos * coeff)}</Text>}
            {slot.extraNeg > 0 && <Text size="xs" c="red">Штраф: -{formatPoints(slot.extraNeg * coeff)}</Text>}
            {slot.penalty > 0 && <Text size="xs" c="red">Дисциплинарный: -{formatPoints(slot.penalty * coeff)}</Text>}
            {slot.compensationPoints > 0 && <Text size="xs" c="blue">Ci: +{formatPoints(slot.compensationPoints * coeff)}</Text>}
        </Stack>
    );
}

function WinnerBanner({ game, isCompleted, areResultsHidden, c }) {
    const bannerStyle = {
        padding: '8px', textAlign: 'center',
        borderBottomLeftRadius: '7px', borderBottomRightRadius: '7px',
        borderTop: `1px solid ${c.border}`,
    };

    if (isCompleted && !areResultsHidden) {
        let bg   = c.winnerDrawBg;
        let text = 'Ничья';

        if (game.winner === 'red')   { bg = c.winnerRedBg;   text = 'Победа Мирных'; }
        if (game.winner === 'black') { bg = c.winnerBlackBg; text = 'Победа Мафии';  }

        return (
            <div style={{ ...bannerStyle, backgroundColor: bg }}>
                <Text fw={400} c={c.textPrimary} size="xs" tt="uppercase" style={{ letterSpacing: '0.5px' }}>
                    {text}
                </Text>
            </div>
        );
    }

    if (game.status === 'pending' || game.status === 'draft') {
        return (
            <div style={{ ...bannerStyle, backgroundColor: c.surface3 }}>
                <Text c={game.status === 'draft' ? 'orange' : 'dimmed'} size="xs">
                    {game.status === 'draft' ? 'Черновик — результаты не сохранены' : 'Игра ожидает заполнения'}
                </Text>
            </div>
        );
    }

    return null;
}

export default function GameTableCard({ game, tournament, user, participantOptions = [], onRefresh, isRating }) {
    const c = useThemeColors();

    const [swapOpen,    setSwapOpen]    = useState(false);
    const [swapLoading, setSwapLoading] = useState(false);
    const [deleteOpen,  setDeleteOpen]  = useState(false);
    const [delLoading,  setDelLoading]  = useState(false);

    const isDraft          = game.status === 'draft';
    const isCompleted      = game.status === 'completed';
    const areResultsHidden = tournament?.settings?.areResultsHidden || !isCompleted || isDraft;
    const canEdit          = resolveCanEdit(user, tournament, game);
    const isDraw           = game.winner === 'draw';
    const isFinal          = game.stage === 'final_round';
    const coeff            = game.coefficient || 1.0;

    const handleSwapConfirm = async (params) => {
        setSwapLoading(true);
        try {
            await swapSlot(game.id, params);
            setSwapOpen(false);
            onRefresh?.();
        } catch (err) {
            alert(err?.response?.data || 'Ошибка замены игрока');
        } finally {
            setSwapLoading(false);
        }
    };

    const handleDeleteConfirm = async () => {
        setDelLoading(true);
        try {
            await gamesApi.deleteGame(game.id);
            setDeleteOpen(false);
            onRefresh?.();
        } catch (err) {
            alert(err?.response?.data?.message || err?.response?.data || 'Ошибка удаления игры');
        } finally {
            setDelLoading(false);
        }
    };

    const rows = (game.slots || []).map(slot => {
        const roleConfig    = ROLE_CONFIG[slot.role?.toLowerCase()] || null;
        const roleTextColor = !areResultsHidden && roleConfig
            ? `var(--mantine-color-${roleConfig.color}-light-color)` : 'inherit';

        return (
            <Table.Tr
                key={slot.slotNumber}
                bg={slot.isFirstKilled && !areResultsHidden ? c.slotFkBg : undefined}
            >
                <Table.Td w={40} style={{ minWidth: 40, textAlign: 'center' }}>
                    <Text size="sm" c="dimmed">{slot.slotNumber}</Text>
                </Table.Td>

                <Table.Td style={{ minWidth: 140 }}>
                    <Text
                        size="sm" fw={500}
                        component={slot.playerId ? Link : 'span'}
                        to={slot.playerId ? `/players/${slot.playerId}` : undefined}
                        style={{ textDecoration: 'none', color: c.textPrimary }}
                    >
                        {slot.playerNickname || 'Слот пуст'}
                    </Text>
                </Table.Td>

                <Table.Td w={60} style={{ minWidth: 60, textAlign: 'center', color: roleTextColor, fontWeight: 600 }}>
                    {!areResultsHidden && roleConfig
                        ? <Text size="sm">{roleConfig.label}</Text>
                        : <Text c="dimmed" size="xs">-</Text>
                    }
                </Table.Td>

                <Table.Td
                    w={60}
                    style={{
                        minWidth: 60, textAlign: 'center',
                        backgroundColor: !areResultsHidden && isCompleted
                            ? slot.extraNeg > 0
                                ? 'rgba(255, 80, 80, 0.18)'
                                : slot.extraPos > 0
                                    ? 'rgba(130, 230, 50, 0.18)'
                                    : undefined
                            : undefined,
                    }}
                >
                    {!areResultsHidden && isCompleted ? (
                        <Tooltip
                            label={<SlotPointsTooltip slot={slot} game={game} isDraw={isDraw} isFinal={isFinal} coeff={coeff} />}
                            color="gray" withArrow position="left"
                        >
                            <Text size="sm" style={{ cursor: 'help' }}>
                                {formatPoints(Number(slot.totalScore ?? slot.computedScore ?? 0) * coeff)}
                            </Text>
                        </Tooltip>
                    ) : (
                        <Text c="dimmed" size="xs">-</Text>
                    )}
                </Table.Td>
            </Table.Tr>
        );
    });

    return (
        <>
            <Paper
                radius="md" w={350} withBorder
                style={{
                    display: 'flex', flexDirection: 'column',
                    borderColor: c.border,
                    backgroundColor: c.surface2,
                }}
            >
                {/* Шапка */}
                <Group
                    justify="space-between" p="xs"
                    style={{
                        backgroundColor: c.surface3,
                        borderTopLeftRadius: '7px', borderTopRightRadius: '7px',
                        borderBottom: `1px solid ${c.border}`,
                    }}
                >
                    <Group gap="xs">
                        <Text fw={700} c={c.textBright}>Стол {game.table ?? game.tableNumber ?? '?'}</Text>
                        {isFinal && <Badge size="xs" color="grape" variant="light">ФИНАЛ x{coeff}</Badge>}
                        {isDraft  && <Badge size="xs" color="orange" variant="outline">Черновик</Badge>}
                    </Group>

                    <Group gap="xs">
                        {canEdit && isRating && (
                            <Tooltip label="Удалить игру" withArrow>
                                <ActionIcon variant="subtle" color="red" size="sm" onClick={() => setDeleteOpen(true)}>
                                    <IconTrash size={16} />
                                </ActionIcon>
                            </Tooltip>
                        )}
                        {canEdit && !isCompleted && (
                            <Tooltip label="Заменить игрока в слоте" withArrow>
                                <ActionIcon variant="subtle" color="blue" size="sm" onClick={() => setSwapOpen(true)}>
                                    <IconRefresh size={14} />
                                </ActionIcon>
                            </Tooltip>
                        )}
                        {canEdit && (
                            <ActionIcon
                                component={Link}
                                to={`/tournaments/${tournament.id}/games/${game.id}`}
                                variant="subtle" color="gray" size="sm"
                            >
                                <IconEdit size={16} />
                            </ActionIcon>
                        )}
                        <Group gap={5}>
                            <Text size="xs" c="dimmed">Судья:</Text>
                            <Text size="xs" fw={500} c={c.textPrimary}>{game.judgeName || 'Не назначен'}</Text>
                        </Group>
                    </Group>
                </Group>

                {/* Таблица */}
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', backgroundColor: c.surface2 }}>
                    <Table
                        verticalSpacing="xs" horizontalSpacing="xs"
                        withRowBorders withColumnBorders
                        style={{ fontSize: '14px', flex: 1 }}
                    >
                        <Table.Tbody>{rows}</Table.Tbody>
                    </Table>
                </div>

                <WinnerBanner game={game} isCompleted={isCompleted} areResultsHidden={areResultsHidden} c={c} />
            </Paper>

            <SwapSlotModal
                opened={swapOpen}
                onClose={() => setSwapOpen(false)}
                onConfirm={handleSwapConfirm}
                slots={game.slots || []}
                participantOptions={participantOptions}
                loading={swapLoading}
            />
            <DeleteRatingGameModal
                opened={deleteOpen}
                onClose={() => setDeleteOpen(false)}
                onConfirm={handleDeleteConfirm}
                game={game}
                loading={delLoading}
            />
        </>
    );
}