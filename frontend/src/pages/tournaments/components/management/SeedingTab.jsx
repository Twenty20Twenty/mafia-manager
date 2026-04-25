// src/pages/tournaments/components/management/SeedingTab.jsx
import { useState, useEffect, useCallback } from 'react';
import { Stack, Paper, Title, Text, Group, Select, Button, ActionIcon, Badge, ThemeIcon } from '@mantine/core';
import { IconX, IconTrash, IconSwords, IconTrophy } from '@tabler/icons-react';
import DeleteGamesModal from './DeleteGamesModal';
import FinalistsEditor  from './FinalistsEditor';
import api from '../../../../api/axios';
import { useThemeColors } from '../../../../hooks/useThemeColors';

function PersonOption({ option }) {
    return (
        <Group gap="sm" w="100%">
            <div>
                <Text size="sm">{option.label}</Text>
                {option.description && <Text size="xs" c="dimmed">{option.description}</Text>}
            </div>
        </Group>
    );
}

function SeedingStatus({ tournamentId, isSeedingGenerated }) {
    const [gamesInfo, setGamesInfo] = useState(null);

    const fetchGamesInfo = useCallback(async () => {
        if (!tournamentId) return;
        try {
            const res = await api.get(`/tournaments/${tournamentId}/games`);
            const games = res.data || [];
            const qualifyingGames = games.filter(g => g.stage === 'qualifying' || !g.stage);
            const finalGames      = games.filter(g => g.stage === 'final_round');
            const maxRound = qualifyingGames.reduce((max, g) => Math.max(max, g.round ?? 0), 0);
            const roundMap = {};
            qualifyingGames.forEach(g => {
                const r = g.round ?? 0;
                if (!roundMap[r]) roundMap[r] = { total: 0, completed: 0 };
                roundMap[r].total++;
                if (g.status === 'completed') roundMap[r].completed++;
            });
            const completedRounds = Object.values(roundMap).filter(r => r.total === r.completed && r.total > 0).length;
            setGamesInfo({
                totalRounds: maxRound, completedRounds,
                hasFinalGames: finalGames.length > 0,
                finalGamesCount: finalGames.length,
                finalCompleted: finalGames.filter(g => g.status === 'completed').length,
            });
        } catch (err) { console.error(err); }
    }, [tournamentId]);

    useEffect(() => { fetchGamesInfo(); }, [fetchGamesInfo, isSeedingGenerated]);

    if (!gamesInfo) return null;

    return (
        <Stack gap="xs" mt="sm">
            <Group gap="xs" wrap="wrap">
                <ThemeIcon size="sm" variant="light" color={isSeedingGenerated ? 'green' : 'gray'}>
                    <IconSwords size={12} />
                </ThemeIcon>
                <Text size="sm" c="dimmed">Отборочная рассадка:</Text>
                <Badge color={isSeedingGenerated ? 'green' : 'gray'} size="sm" variant={isSeedingGenerated ? 'light' : 'outline'}>
                    {isSeedingGenerated ? 'Сгенерирована' : 'Не сгенерирована'}
                </Badge>
                {gamesInfo.totalRounds > 0 && (
                    <Text size="sm" c="dimmed">
                        Туров: <Text span fw={600}>{gamesInfo.totalRounds}</Text>
                        {gamesInfo.completedRounds > 0 && <Text span c="teal"> ({gamesInfo.completedRounds} завершено)</Text>}
                    </Text>
                )}
            </Group>

            <Group gap="xs" wrap="wrap">
                <ThemeIcon size="sm" variant="light" color={gamesInfo.hasFinalGames ? 'grape' : 'gray'}>
                    <IconTrophy size={12} />
                </ThemeIcon>
                <Text size="sm" c="dimmed">Финальная рассадка:</Text>
                {gamesInfo.hasFinalGames ? (
                    <Badge color="grape" size="sm" variant="light">
                        Сгенерирована ({gamesInfo.finalCompleted}/{gamesInfo.finalGamesCount} завершено)
                    </Badge>
                ) : (
                    <Badge color="gray" size="sm" variant="outline">Не сгенерирована</Badge>
                )}
            </Group>
        </Stack>
    );
}

export default function SeedingTab({
                                       exceptions, exceptionP1, exceptionP2,
                                       setExceptionP1, setExceptionP2, peopleForExceptions,
                                       onAddException, onRemoveException,
                                       isSeedingGenerated, onGenerateSeeding, onDeleteGames,
                                       finalists, allParticipants, locked,
                                       onSaveFinalists, onAutoFillFinalists, onLockFinalists, onUnlockFinalists, finalistsLoading,
                                       areQualifiersFixed, onGenerateFinalSeeding,
                                       tournamentId,
                                   }) {
    const c = useThemeColors();
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [deleteLoading,   setDeleteLoading]   = useState(false);

    const renderOption = ({ option }) => <PersonOption option={option} />;
    const getLabel     = (ex, side) => side === 'p1' ? (ex.p1Label || ex.p1) : (ex.p2Label || ex.p2);

    const handleDeleteConfirm = async (params) => {
        setDeleteLoading(true);
        try { await onDeleteGames(params); setDeleteModalOpen(false); }
        finally { setDeleteLoading(false); }
    };

    return (
        <Stack gap="lg">
            {/* Исключения */}
            <Paper withBorder p="md" style={{ backgroundColor: c.surface2 }}>
                <Title order={4} mb="md">Исключения рассадки</Title>
                <Text size="sm" c="dimmed" mb="sm">Люди, которые не должны сидеть за одним столом.</Text>
                <Group align="flex-end" mb="md">
                    <Select label="Человек 1" placeholder="Участник или судья" data={peopleForExceptions}
                            searchable value={exceptionP1} onChange={setExceptionP1} style={{ flex: 1 }} renderOption={renderOption} />
                    <Select label="Человек 2" placeholder="Участник или судья" data={peopleForExceptions}
                            searchable value={exceptionP2} onChange={setExceptionP2} style={{ flex: 1 }} renderOption={renderOption} />
                    <Button onClick={onAddException} disabled={!exceptionP1 || !exceptionP2 || exceptionP1 === exceptionP2}>
                        Добавить пару
                    </Button>
                </Group>
                <Stack gap="xs">
                    {exceptions.map(ex => (
                        <Group key={ex.id} p="xs" style={{ borderRadius: 4, backgroundColor: c.surface3 }}>
                            <Text size="sm" fw={700}>{getLabel(ex, 'p1')}</Text>
                            <IconX size={14} style={{ opacity: 0.5 }} />
                            <Text size="sm" fw={700}>{getLabel(ex, 'p2')}</Text>
                            <ActionIcon color="red" variant="subtle" size="xs" onClick={() => onRemoveException(ex.id)} ml="auto">
                                <IconX size={14} />
                            </ActionIcon>
                        </Group>
                    ))}
                    {exceptions.length === 0 && <Text size="xs" c="dimmed">Нет исключений</Text>}
                </Stack>
            </Paper>

            {/* Отборочная рассадка */}
            <Paper withBorder p="xl" style={{ backgroundColor: c.surface2 }}>
                <Group justify="space-between" align="flex-start">
                    <div style={{ flex: 1 }}>
                        <Title order={4}>Отборочная рассадка</Title>
                        <SeedingStatus tournamentId={tournamentId} isSeedingGenerated={isSeedingGenerated} />
                    </div>
                    <Group gap="xs" style={{ flexShrink: 0 }}>
                        {isSeedingGenerated && (
                            <Button variant="outline" color="red" leftSection={<IconTrash size={14} />}
                                    onClick={() => setDeleteModalOpen(true)}>
                                Удалить игры
                            </Button>
                        )}
                        <Button onClick={onGenerateSeeding}>
                            {isSeedingGenerated ? 'Перегенерировать' : 'Сгенерировать'}
                        </Button>
                    </Group>
                </Group>
            </Paper>

            {/* Финалисты */}
            <FinalistsEditor
                finalists={finalists}
                allParticipants={allParticipants}
                locked={locked}
                onSave={onSaveFinalists}
                onAutoFill={onAutoFillFinalists}
                onLock={onLockFinalists}
                onUnlock={onUnlockFinalists}
                loading={finalistsLoading}
            />

            {/* Финальная рассадка */}
            <Paper withBorder p="xl" style={{ backgroundColor: c.surface2 }}>
                <Title order={4} mb="xs">Финальная рассадка</Title>
                <Text c="dimmed" size="sm" mb="md">Зафиксируйте финалистов выше, затем генерируйте финальную рассадку.</Text>
                {areQualifiersFixed && (
                    <Button color="brandRed" onClick={onGenerateFinalSeeding}>
                        Сгенерировать финальную рассадку
                    </Button>
                )}
            </Paper>

            <DeleteGamesModal
                opened={deleteModalOpen}
                onClose={() => setDeleteModalOpen(false)}
                onConfirm={handleDeleteConfirm}
                loading={deleteLoading}
            />
        </Stack>
    );
}
