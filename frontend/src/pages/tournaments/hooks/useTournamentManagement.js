// src/pages/tournaments/hooks/useTournamentManagement.js

import { useState, useEffect, useCallback, useMemo } from 'react';
import { notifications } from '@mantine/notifications';
import api from '../../../api/axios';
import { tournamentsApi } from '../../../api/tournaments';
import { initSettings, buildUpdatePayload, mapExceptions } from '../utils/tournamentUtils';
import {
    deleteAllGames, deleteRoundGames, deleteRangeGames,
    getFinalists, setFinalists, autoFillFinalists,
    lockFinalists, unlockFinalists,
} from '../../../api/tournaments_additions';

export function useTournamentManagement(id) {
    // --- Данные ---
    const [tournament, setTournament] = useState(null);
    const [loading, setLoading]       = useState(true);
    const [allJudges, setAllJudges]   = useState([]);
    const [allPlayers, setAllPlayers] = useState([]);

    // --- Настройки ---
    const [settings, setSettings] = useState(initSettings({}));

    // --- Участники ---
    const [approvedParticipants, setApprovedParticipants] = useState([]);
    const [pendingRequests, setPendingRequests]           = useState([]);
    const [selectedPlayerId, setSelectedPlayerId]         = useState(null);

    // --- Судьи ---
    const [headJudgeId, setHeadJudgeId]           = useState('');
    const [finalJudgeId, setFinalJudgeId]         = useState(null);
    const [tournamentJudges, setTournamentJudges] = useState([]);
    const [tableJudgesMap, setTableJudgesMap]     = useState({});

    // --- Исключения ---
    const [exceptions, setExceptions]   = useState([]);
    const [exceptionP1, setExceptionP1] = useState(null);
    const [exceptionP2, setExceptionP2] = useState(null);

    // --- Финалисты ---
    const [finalists,        setFinalistsState]  = useState([]);
    const [finalistsLocked,  setFinalistsLocked]  = useState(false);
    const [finalistsLoading, setFinalistsLoading] = useState(false);

    const loadFinalists = useCallback(async (approvedList = []) => {
        try {
            const data = await getFinalists(id);
            const enriched = (data.playerIds || []).map(pid => {
                const p = approvedList.find(ap => ap.id === pid);
                return p
                    ? { id: pid, nickname: p.nickname, avatarUrl: p.avatarUrl }
                    : { id: pid, nickname: `ID ${pid}` };
            });
            setFinalistsState(enriched);
            setFinalistsLocked(data.locked || false);
        } catch (err) {
            console.error('Ошибка загрузки финалистов', err);
        }
    }, [id]);

    // ─── ЗАГРУЗКА ───────────────────────────────────────────────
    const fetchInitialData = useCallback(async () => {
        try {
            setLoading(true);

            const [
                tourRes,
                partsRes,
                excRes,
                playersRes,
                judgesRes,
                tournamentJudgesRes,
                tableJudgesRes,
            ] = await Promise.all([
                api.get(`/tournaments/${id}`),
                api.get(`/tournaments/${id}/participants`),
                api.get(`/tournaments/${id}/exceptions`),
                api.get('/users?size=500'),
                api.get('/judges?size=100'),
                api.get(`/tournaments/${id}/judges`),
                tournamentsApi.getTableJudges(id),
            ]);

            const t = tourRes.data;
            setTournament(t);
            setSettings(initSettings(t));
            setHeadJudgeId(t.headJudgeId ? String(t.headJudgeId) : '');
            setFinalJudgeId(t.settings?.finalJudgeId ? String(t.settings.finalJudgeId) : null);
            setTableJudgesMap(tableJudgesRes || {});
            setTournamentJudges((tournamentJudgesRes.data || []).map(String));

            const allParts = partsRes.data;
            const approved = allParts.filter(p => p.status === 'approved');
            const pending  = allParts.filter(p => p.status === 'pending');
            setApprovedParticipants(approved);
            setPendingRequests(pending);

            setExceptions(mapExceptions(excRes.data));
            setAllPlayers(playersRes.data.content || []);
            setAllJudges(judgesRes.data.content || []);

            await loadFinalists(approved);
        } catch (error) {
            console.error('Ошибка загрузки данных управления', error);
            notifications.show({ color: 'red', message: 'Не удалось загрузить данные турнира' });
        } finally {
            setLoading(false);
        }
    }, [id, loadFinalists]);

    useEffect(() => {
        if (id) fetchInitialData();
    }, [fetchInitialData, id]);

    // ─── ПРОИЗВОДНЫЕ OPTIONS ────────────────────────────────────
    const approvedIds = useMemo(
        () => new Set(approvedParticipants.map(p => p.id)),
        [approvedParticipants]
    );
    const pendingIds = useMemo(
        () => new Set(pendingRequests.map(p => p.id)),
        [pendingRequests]
    );

    const addPlayerOptions = useMemo(
        () =>
            allPlayers
                .filter(p => !approvedIds.has(p.id) && !pendingIds.has(p.id))
                .map(p => ({ value: String(p.id), label: p.nickname })),
        [allPlayers, approvedIds, pendingIds]
    );

    const allJudgesOptions = useMemo(
        () => allJudges.map(j => ({ value: String(j.userId), label: j.nickname, avatar: j.avatarUrl })),
        [allJudges]
    );

    const assignedJudgesOptions = useMemo(() => {
        const ids = new Set([...tournamentJudges, headJudgeId].filter(Boolean));
        return Array.from(ids)
            .map(jId => {
                const j = allJudges.find(pl => String(pl.userId) === jId);
                return j ? { value: String(j.userId), label: j.nickname, avatar: j.avatarUrl } : null;
            })
            .filter(Boolean);
    }, [tournamentJudges, headJudgeId, allJudges]);

    const participantsForExceptions = useMemo(() => {
        const map = new Map();
        approvedParticipants.forEach(p =>
            map.set(String(p.id), {
                value: String(p.id),
                label: p.nickname,
                description: p.clubName || '',
                type: 'player',
            })
        );
        assignedJudgesOptions.forEach(j => {
            if (!map.has(j.value))
                map.set(j.value, { ...j, description: 'Судья', type: 'judge' });
        });
        return Array.from(map.values());
    }, [approvedParticipants, assignedJudgesOptions]);

    // ─── ОБРАБОТЧИКИ ────────────────────────────────────────────

    const handleSaveSettings = async () => {
        try {
            const payload = buildUpdatePayload(tournament, settings, headJudgeId);
            await api.put(`/tournaments/${id}`, payload);
            setTournament(prev => ({
                ...prev,
                headJudgeId: payload.headJudgeId,
                cityId:      payload.cityId,
                title:       payload.title,
            }));
            notifications.show({ color: 'green', message: 'Настройки успешно сохранены!' });
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка сохранения: ' + (error.response?.data?.message || error.message),
            });
        }
    };

    const handleSaveJudges = async () => {
        try {
            // 1. Сохраняем ГС через обновление турнира
            const payload = buildUpdatePayload(tournament, settings, headJudgeId);
            await api.put(`/tournaments/${id}`, payload);

            // 2. Сохраняем список судей турнира
            const judgeIds = tournamentJudges.map(Number);
            await api.put(`/tournaments/${id}/judges`, judgeIds);

            // 3. Сохраняем назначения судей по столам
            await tournamentsApi.updateTableJudges(id, tableJudgesMap);

            // 4. Обновляем локальный стейт турнира
            setTournament(prev => ({
                ...prev,
                headJudgeId: payload.headJudgeId,
            }));

            notifications.show({ color: 'green', message: 'Судьи успешно сохранены!' });
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка сохранения судей: ' + (error.response?.data?.message || error.message),
            });
        }
    };

    const handleAcceptRequest = async (userId) => {
        try {
            await api.patch(`/tournaments/${id}/participants/${userId}/status?status=approved`);
            const accepted = pendingRequests.find(p => p.id === userId);
            if (accepted) {
                setPendingRequests(prev => prev.filter(p => p.id !== userId));
                setApprovedParticipants(prev => [...prev, { ...accepted, status: 'approved' }]);
            }
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка: ' + (error.response?.data?.message || error.message),
            });
        }
    };

    const handleRejectRequest = async (userId) => {
        try {
            await api.patch(`/tournaments/${id}/participants/${userId}/status?status=rejected`);
            setPendingRequests(prev => prev.filter(p => p.id !== userId));
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка отклонения заявки: ' + (error.response?.data?.message || error.message),
            });
        }
    };

    const handleAddManual = async () => {
        if (!selectedPlayerId) return;
        try {
            await api.post(`/tournaments/${id}/participants?userId=${selectedPlayerId}`);
            const added = allPlayers.find(p => p.id === Number(selectedPlayerId));
            if (added) setApprovedParticipants(prev => [...prev, { ...added, status: 'approved' }]);
            setSelectedPlayerId(null);
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка добавления: ' + (error.response?.data?.message || error.message),
            });
        }
    };

    const handleRemoveParticipant = async (userId) => {
        try {
            await api.delete(`/tournaments/${id}/participants/${userId}`);
            setApprovedParticipants(prev => prev.filter(p => p.id !== userId));
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка удаления: ' + (error.response?.data?.message || error.message),
            });
        }
    };

    const handleAddException = async () => {
        if (!exceptionP1 || !exceptionP2 || exceptionP1 === exceptionP2) return;
        try {
            await api.post(`/tournaments/${id}/exceptions?p1=${exceptionP1}&p2=${exceptionP2}`);
            const excRes = await api.get(`/tournaments/${id}/exceptions`);
            setExceptions(mapExceptions(excRes.data));
            setExceptionP1(null);
            setExceptionP2(null);
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка добавления исключения: ' + (error.response?.data?.message || error.message),
            });
        }
    };

    const handleRemoveException = async (exId) => {
        try {
            await api.delete(`/tournaments/${id}/exceptions/${exId}`);
            setExceptions(prev => prev.filter(x => x.id !== exId));
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка удаления: ' + (error.response?.data?.message || error.message),
            });
        }
    };

    const handleGenerateSeeding = async () => {
        try {
            await tournamentsApi.updateTableJudges(id, tableJudgesMap);
            await tournamentsApi.generateSeeding(id);
            setSettings(prev => ({ ...prev, isSeedingGenerated: true }));
            // Перезагружаем турнир чтобы получить актуальные данные
            const tourRes = await api.get(`/tournaments/${id}`);
            setTournament(tourRes.data);
            notifications.show({ color: 'green', message: 'Рассадка успешно сгенерирована!' });
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка при генерации: ' + (error.response?.data || error.message),
            });
        }
    };

    const handleFixQualifiers = async () => {
        try {
            await api.post(`/tournaments/${id}/fix-qualifiers`);
            setSettings(prev => ({ ...prev, areQualifiersFixed: true }));
            notifications.show({ color: 'green', message: 'Топ-10 участников зафиксированы!' });
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка фиксации: ' + (error.response?.data || error.message),
            });
        }
    };

    const handleGenerateFinalSeeding = async () => {
        try {
            await api.post(`/tournaments/${id}/generate-final-seeding`);
            notifications.show({ color: 'green', message: 'Финальная рассадка успешно сгенерирована!' });
        } catch (error) {
            notifications.show({
                color: 'red',
                message: 'Ошибка: ' + (error.response?.data || error.message),
            });
        }
    };

    const handleDeleteGames = async ({ mode, fromRound, toRound }) => {
        try {
            if (mode === 'ALL')   await deleteAllGames(id);
            if (mode === 'ROUND') await deleteRoundGames(id, fromRound);
            if (mode === 'RANGE') await deleteRangeGames(id, fromRound, toRound);
            notifications.show({ color: 'green', message: 'Игры удалены' });
            setSettings(prev => ({ ...prev, isSeedingGenerated: false }));
            setTournament(prev => ({
                ...prev,
                settings: { ...prev?.settings, isSeedingGenerated: false },
            }));
        } catch (err) {
            notifications.show({
                color: 'red',
                message: err?.response?.data || 'Ошибка удаления',
            });
        }
    };

    const handleSaveFinalists = async (ids, lock) => {
        setFinalistsLoading(true);
        try {
            await setFinalists(id, ids, lock);
            notifications.show({
                color: 'green',
                message: lock ? 'Финалисты зафиксированы' : 'Финалисты сохранены',
            });
            await loadFinalists(approvedParticipants);
        } catch (err) {
            notifications.show({
                color: 'red',
                message: err?.response?.data || 'Ошибка сохранения',
            });
        } finally {
            setFinalistsLoading(false);
        }
    };

    const handleAutoFillFinalists = async () => {
        setFinalistsLoading(true);
        try {
            await autoFillFinalists(id);
            notifications.show({ color: 'green', message: 'Финалисты заполнены из лидерборда' });
            await loadFinalists(approvedParticipants);
        } catch (err) {
            notifications.show({
                color: 'red',
                message: err?.response?.data || 'Ошибка автозаполнения',
            });
        } finally {
            setFinalistsLoading(false);
        }
    };

    const handleLockFinalists = async () => {
        setFinalistsLoading(true);
        try {
            await lockFinalists(id);
            notifications.show({ color: 'orange', message: 'Финалисты зафиксированы' });
            setFinalistsLocked(true);
        } catch (err) {
            notifications.show({ color: 'red', message: err?.response?.data || 'Ошибка' });
        } finally {
            setFinalistsLoading(false);
        }
    };

    const handleUnlockFinalists = async () => {
        setFinalistsLoading(true);
        try {
            await unlockFinalists(id);
            notifications.show({ color: 'blue', message: 'Фиксация снята' });
            setFinalistsLocked(false);
        } catch (err) {
            notifications.show({ color: 'red', message: err?.response?.data || 'Ошибка' });
        } finally {
            setFinalistsLoading(false);
        }
    };

    return {
        // State
        tournament, loading, settings, setSettings,
        approvedParticipants, pendingRequests,
        selectedPlayerId, setSelectedPlayerId,
        headJudgeId, setHeadJudgeId,
        finalJudgeId, setFinalJudgeId,
        tournamentJudges, setTournamentJudges,
        tableJudgesMap, setTableJudgesMap,
        exceptions, exceptionP1, setExceptionP1, exceptionP2, setExceptionP2,
        finalists: finalists, finalistsLocked, finalistsLoading,
        // Computed
        addPlayerOptions, allJudgesOptions, assignedJudgesOptions, participantsForExceptions,
        // Handlers
        handleSaveSettings,
        handleSaveJudges,
        handleAcceptRequest,
        handleRejectRequest,
        handleAddManual, handleRemoveParticipant,
        handleAddException, handleRemoveException,
        handleGenerateSeeding,
        handleFixQualifiers,
        handleGenerateFinalSeeding,
        handleDeleteGames,
        handleSaveFinalists,
        handleAutoFillFinalists,
        handleLockFinalists,
        handleUnlockFinalists,
    };
}
