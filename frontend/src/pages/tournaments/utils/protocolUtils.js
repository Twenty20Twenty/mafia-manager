// src/pages/tournaments/utils/protocolUtils.js
import { ROLE_DISTRIBUTION } from '../constants/tournamentConstants';

/** Преобразует DTO-слоты с бэкенда в локальный формат */
export function mapSlotsFromDto(dtoSlots) {
    return dtoSlots.map(s => ({
        slotId:         s.id,
        slotNumber:     s.slotNumber,
        playerId:       s.playerId       ?? null,
        playerNickname: s.playerNickname ?? '',
        role:           s.role           ?? 'civilian',
        fouls:          s.fouls          ?? 0,
        extraPos:       Number(s.extraPos  ?? 0),
        extraNeg:       Number(s.extraNeg  ?? 0),
        penalty:        Number(s.penalty   ?? 0),
        isFirstKilled:  s.isFirstKilled  ?? false,
    }));
}

/** Собирает DTO для сохранения протокола */
export function buildProtocolDto(gameId, tournamentId, winner, isDraft, gameDate, slots, bestMove) {
    return {
        id: gameId,
        tournamentId,
        status: isDraft ? 'draft' : 'completed',
        winner,
        date: gameDate,
        slots: slots.map(s => ({
            slotNumber:    s.slotNumber,
            playerId:      s.playerId ? Number(s.playerId) : null,
            role:          s.role,
            extraPos:      s.extraPos,
            extraNeg:      s.extraNeg,
            penalty:       s.penalty,
            fouls:         s.fouls,
            isFirstKilled: s.isFirstKilled,
        })),
        bestMove: bestMove.authorSlotNumber
            ? {
                authorId:   slots.find(s => s.slotNumber === bestMove.authorSlotNumber)?.playerId ?? null,
                candidates: bestMove.candidates.filter(c => c !== null && c !== 0),
                points:     bestMove.points,
            }
            : null,
    };
}

/** Валидирует протокол перед сохранением, возвращает строку ошибки или null */
export function validateProtocol(slots, winner, isRating) {
    if (isRating && slots.some(s => !s.playerId)) {
        return 'Все 10 мест за столом должны быть заняты!';
    }
    if (isRating) {
        const ids = slots.map(s => Number(s.playerId));
        if (new Set(ids).size !== ids.length) return 'Игроки за столом повторяются!';
    }
    const counts = { sheriff: 0, don: 0, mafia: 0, civilian: 0 };
    slots.forEach(s => { if (counts[s.role] !== undefined) counts[s.role]++; });
    const isValid = Object.entries(ROLE_DISTRIBUTION).every(([r, exp]) => counts[r] === exp);
    if (!isValid) return 'Некорректное распределение ролей: 1 Шериф, 1 Дон, 2 Мафии, 6 Мирных.';
    if (!winner) return 'Выберите победителя!';


    return null;
}

/** Вычисляет баллы за Лучший Ход */
export function calcBestMovePoints(candidates, slots) {
    const black = new Set(['mafia', 'don']);
    const unique = [...new Set(candidates.filter(c => c !== null && c !== 0))];
    const blackCount = unique.filter(n => {
        const s = slots.find(x => x.slotNumber === n);
        return s && black.has(s.role);
    }).length;
    if (blackCount === 3) return 0.6;
    if (blackCount === 2) return 0.3;
    if (blackCount === 1) return 0.1;
    return 0;
}
