// src/pages/tournaments/components/management/TeamsManager.jsx
import { useState, useEffect, useCallback } from 'react';
import {
    Stack, Paper, Title, Text, Group, Button, SimpleGrid,
    Loader, Center, Alert,
} from '@mantine/core';
import { IconPlus, IconWand, IconAlertCircle } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { teamsApi } from '../../../../api/teams';
import TeamBlock from './TeamBlock';
import { useThemeColors } from '../../../../hooks/useThemeColors';

/**
 * Блок управления командами.
 * Отображается только для командных турниров (type = 'team').
 *
 * Props:
 *   tournamentId     — ID турнира
 *   teamSize         — кол-во игроков в команде (из settings)
 *   maxParticipants  — макс. участников (из settings)
 *   approvedParticipants — [{ id, nickname }] — все одобренные участники
 */
export default function TeamsManager({
    tournamentId, teamSize, maxParticipants, approvedParticipants,
}) {
    const c = useThemeColors();
    const [teams,   setTeams]   = useState([]);
    const [loading, setLoading] = useState(true);
    const [error,   setError]   = useState(null);

    const maxTeams = (maxParticipants && teamSize && teamSize > 0)
        ? Math.floor(maxParticipants / teamSize)
        : 999;

    // ─── Загрузка ────────────────────────────────────────────────
    const reload = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await teamsApi.getTeams(tournamentId);
            setTeams(data);
        } catch (err) {
            setError('Не удалось загрузить команды');
        } finally {
            setLoading(false);
        }
    }, [tournamentId]);

    useEffect(() => { reload(); }, [reload]);

    // ─── Вычисляемые данные ──────────────────────────────────────

    /** Участники, у которых ещё нет команды */
    const assignedIds = new Set(teams.flatMap(t => t.memberIds || []));
    const freeParticipants = approvedParticipants
        .filter(p => !assignedIds.has(p.id))
        .map(p => ({ value: String(p.id), label: p.nickname }));

    // ─── Обработчики ─────────────────────────────────────────────

    const handleCreate = async () => {
        try {
            await teamsApi.createTeam(tournamentId, '');
            await reload();
        } catch (err) {
            notifications.show({ color: 'red', message: err?.response?.data?.message || 'Ошибка создания команды' });
        }
    };

    const handleGenerateNames = async () => {
        try {
            const names = await teamsApi.generateNames(tournamentId, teams.length);
            // Последовательно переименовываем команды
            await Promise.all(
                teams.map((team, i) =>
                    names[i] ? teamsApi.renameTeam(tournamentId, team.id, names[i]) : Promise.resolve()
                )
            );
            await reload();
            notifications.show({ color: 'teal', message: 'Названия сгенерированы!' });
        } catch (err) {
            notifications.show({ color: 'red', message: 'Ошибка генерации названий' });
        }
    };

    const handleRename = async (teamId, name) => {
        try {
            await teamsApi.renameTeam(tournamentId, teamId, name);
            setTeams(prev => prev.map(t => t.id === teamId ? { ...t, name } : t));
        } catch {
            notifications.show({ color: 'red', message: 'Ошибка переименования' });
        }
    };

    const handleDelete = async (teamId) => {
        try {
            await teamsApi.deleteTeam(tournamentId, teamId);
            await reload();
        } catch {
            notifications.show({ color: 'red', message: 'Ошибка удаления команды' });
        }
    };

    const handleAddMember = async (teamId, userId) => {
        try {
            // Получаем текущих участников команды и добавляем нового
            const team = teams.find(t => t.id === teamId);
            const newIds = [...(team?.memberIds || []), userId];
            await teamsApi.assignMembers(tournamentId, teamId, newIds);
            await reload();
        } catch (err) {
            notifications.show({ color: 'red', message: err?.response?.data?.message || 'Ошибка добавления участника' });
        }
    };

    const handleRemoveMember = async (userId) => {
        try {
            await teamsApi.removeMember(tournamentId, userId);
            await reload();
        } catch {
            notifications.show({ color: 'red', message: 'Ошибка открепления участника' });
        }
    };

    // ─── Render ──────────────────────────────────────────────────

    if (loading) return <Center py="md"><Loader color="brandRed" size="sm" /></Center>;
    if (error)   return <Alert color="red" icon={<IconAlertCircle size={16} />}>{error}</Alert>;

    return (
        <Paper withBorder p="md" style={{ backgroundColor: c.surface2 }}>
            <Group justify="space-between" mb="md" wrap="wrap" gap="xs">
                <div>
                    <Title order={4}>Команды</Title>
                    <Text size="xs" c="dimmed">
                        {teams.length} из {maxTeams} команд •{' '}
                        {freeParticipants.length} участников без команды
                    </Text>
                </div>
                <Group gap="xs">
                    {teams.length > 0 && (
                        <Button
                            size="xs" variant="light" leftSection={<IconWand size={14} />}
                            onClick={handleGenerateNames}
                        >
                            Случайные названия
                        </Button>
                    )}
                    <Button
                        size="xs" leftSection={<IconPlus size={14} />}
                        onClick={handleCreate}
                        disabled={teams.length >= maxTeams}
                    >
                        Создать команду
                    </Button>
                </Group>
            </Group>

            {teams.length === 0 ? (
                <Text size="sm" c="dimmed" ta="center" py="md">
                    Команд ещё нет. Нажмите «Создать команду».
                </Text>
            ) : (
                <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="sm">
                    {teams.map(team => (
                        <TeamBlock
                            key={team.id}
                            team={team}
                            teamSize={teamSize || 5}
                            freeParticipants={freeParticipants}
                            onRename={handleRename}
                            onDelete={handleDelete}
                            onAddMember={handleAddMember}
                            onRemoveMember={handleRemoveMember}
                        />
                    ))}
                </SimpleGrid>
            )}
        </Paper>
    );
}
