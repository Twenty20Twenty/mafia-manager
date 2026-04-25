// src/pages/tournaments/hooks/useGameProtocol.js
import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { gamesApi } from '../../../api/games';
import api from '../../../api/axios';
import {
    mapSlotsFromDto,
    buildProtocolDto,
    validateProtocol,
    calcBestMovePoints,
} from '../utils/protocolUtils';
import { ROLE_DISTRIBUTION } from '../constants/tournamentConstants';

const INITIAL_BEST_MOVE = { authorSlotNumber: null, candidates: [null, null, null], points: 0 };

export function useGameProtocol({ tournamentId, gameId, isNewRatingGame }) {
    const navigate = useNavigate();

    const [pageState, setPageState] = useState(isNewRatingGame ? 'form' : 'loading');
    const [errorMsg, setErrorMsg]   = useState('');

    const [game, setGame]       = useState(null);
    const [slots, setSlots]     = useState([]);
    const [winner, setWinner]   = useState(null);
    const [isDraft, setIsDraft] = useState(false);
    const [gameDate, setGameDate] = useState(new Date());
    const [bestMove, setBestMove] = useState(INITIAL_BEST_MOVE);
    const [participantsOptions, setParticipantsOptions] = useState([]);
    const [saving, setSaving]   = useState(false);

    // ─── Применить DTO ───────────────────────────────────────────
    const applyGameDto = useCallback((gameDto) => {
        setGame(gameDto);
        setSlots(mapSlotsFromDto(gameDto.slots || []));
        setWinner(gameDto.winner ?? null);
        setIsDraft(gameDto.status === 'draft');
        if (gameDto.date) setGameDate(new Date(gameDto.date));
        if (gameDto.bestMove?.authorId) {
            const authorSlot = (gameDto.slots || []).find(s => s.playerId === gameDto.bestMove.authorId);
            setBestMove({
                authorSlotNumber: authorSlot?.slotNumber ?? null,
                candidates: [...(gameDto.bestMove.candidates || []), null, null, null].slice(0, 3),
                points: Number(gameDto.bestMove.points ?? 0),
            });
        }
    }, []);

    // ─── Загрузить участников ────────────────────────────────────
    const loadParticipants = useCallback(async () => {
        try {
            const res = await api.get(`/tournaments/${tournamentId}/participants`);
            const approved = (res.data ?? []).filter(p => p.status === 'approved');
            setParticipantsOptions(approved.map(p => ({ value: String(p.id), label: p.nickname })));
        } catch (e) {
            console.warn('Не удалось загрузить участников', e);
        }
    }, [tournamentId]);

    // ─── Загрузить существующую игру ────────────────────────────
    const loadExistingGame = useCallback(async () => {
        setPageState('loading');
        try {
            const gameDto = await gamesApi.getGame(gameId);
            const isRating = gameDto.round === null || gameDto.round === 0;
            if (isRating) await loadParticipants();
            applyGameDto(gameDto);
            setPageState('ready');
        } catch (err) {
            console.error('Ошибка загрузки протокола', err);
            setErrorMsg(err.response?.data?.message || err.message || 'Неизвестная ошибка');
            setPageState('error');
        }
    }, [gameId, applyGameDto, loadParticipants]);

    useEffect(() => {
        if (!isNewRatingGame) loadExistingGame();
    }, [isNewRatingGame, loadExistingGame]);

    // ─── После создания новой игры ──────────────────────────────
    const handleGameCreated = useCallback(async (gameDto) => {
        await loadParticipants();
        applyGameDto(gameDto);
        setPageState('ready');
    }, [applyGameDto, loadParticipants]);

    // ─── Автоподсчёт ЛХ ─────────────────────────────────────────
    useEffect(() => {
        if (!bestMove.authorSlotNumber) return;
        const newPoints = calcBestMovePoints(bestMove.candidates, slots);
        if (newPoints !== bestMove.points) {
            setBestMove(prev => ({ ...prev, points: newPoints }));
        }
    }, [bestMove.candidates, slots, bestMove.authorSlotNumber]);

    // ─── Обновление слота ────────────────────────────────────────
    const updateSlot = useCallback((index, field, value) => {
        setSlots(prev => {
            const updated = [...prev];
            updated[index] = { ...updated[index], [field]: value };

            if (field === 'role' && (value === 'mafia' || value === 'don')) {
                if (updated[index].isFirstKilled) {
                    updated[index].isFirstKilled = false;
                    setBestMove(INITIAL_BEST_MOVE);
                }
            }
            if (field === 'isFirstKilled') {
                if (value === true) {
                    updated.forEach((s, i) => { if (i !== index) updated[i] = { ...s, isFirstKilled: false }; });
                    setBestMove(bm => ({ ...bm, authorSlotNumber: updated[index].slotNumber }));
                } else {
                    setBestMove(INITIAL_BEST_MOVE);
                }
            }
            return updated;
        });
    }, []);

    // ─── Сохранение ─────────────────────────────────────────────
    const handleSave = async () => {
        const isRatingGame = game ? (game.round === null || game.round === 0) : isNewRatingGame;
        const err = validateProtocol(slots, winner, isRatingGame);
        if (err) { alert(err); return; }
        if (!game?.id) { alert('Ошибка: ID игры не определён'); return; }

        setSaving(true);
        try {
            const dto = buildProtocolDto(game.id, Number(tournamentId), winner, isDraft, gameDate, slots, bestMove);
            console.log(dto);
            await gamesApi.saveProtocol(game.id, dto);
            alert(isDraft ? 'Черновик сохранён!' : 'Протокол сохранён!');
            navigate(`/tournaments/${tournamentId}`);
        } catch (err) {
            console.error('Ошибка сохранения', err);
            alert('Ошибка сохранения: ' + (err.response?.data?.message || err.message));
        } finally {
            setSaving(false);
        }
    };

    // ─── Производные ────────────────────────────────────────────
    const isRatingGame = game ? (game.round === null || game.round === 0) : isNewRatingGame;

    const roleCounts = useMemo(() => {
        const c = { sheriff: 0, don: 0, mafia: 0 };
        slots.forEach(s => { if (c[s.role] !== undefined) c[s.role]++; });
        return c;
    }, [slots]);

    return {
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
    };
}
