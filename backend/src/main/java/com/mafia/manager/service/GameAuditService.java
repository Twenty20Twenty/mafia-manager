package com.mafia.manager.service;

import com.mafia.manager.entity.GameAuditAction;
import com.mafia.manager.entity.GameAuditLog;
import com.mafia.manager.entity.GameSlot;
import com.mafia.manager.entity.User;
import com.mafia.manager.repository.GameAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис аудит-логирования операций с игровыми протоколами.
 *
 * <p>Все методы помечены {@code @Async} — запись лога не блокирует
 * основной поток обработки запроса.</p>
 *
 * <p><b>Логируемые события:</b></p>
 * <ul>
 *   <li>Создание игры</li>
 *   <li>Сохранение черновика / завершение</li>
 *   <li>Удаление одной / массовое удаление</li>
 *   <li>Замена игрока в слоте</li>
 *   <li>Изменение баллов (extraPos, extraNeg, penalty, fouls) — только при реальном изменении,
 *       включая повторное сохранение уже завершённой игры</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameAuditService {

    private final GameAuditLogRepository auditLogRepository;

    // ── Публичные методы ──────────────────────────────────────────────────────

    @Async
    public void logGameCreated(Long gameId, Long tournamentId, User actor) {
        save(gameId, tournamentId, GameAuditAction.GAME_CREATED, actor,
                "tournamentId=" + tournamentId);
    }

    @Async
    public void logProtocolSaved(Long gameId, Long tournamentId, User actor,
                                 String status, String winner) {
        String details = "status=" + status + "; winner=" + winner;
        GameAuditAction action = "completed".equals(status)
                ? GameAuditAction.GAME_COMPLETED
                : GameAuditAction.PROTOCOL_DRAFT_SAVED;
        save(gameId, tournamentId, action, actor, details);
    }

    @Async
    public void logGameDeleted(Long gameId, Long tournamentId, User actor) {
        save(gameId, tournamentId, GameAuditAction.GAME_DELETED, actor,
                "gameId=" + gameId);
    }

    @Async
    public void logBulkDeleted(Long tournamentId, User actor, String mode,
                               Integer fromRound, Integer toRound) {
        String details = "mode=" + mode
                + (fromRound != null ? "; fromRound=" + fromRound : "")
                + (toRound   != null ? "; toRound="   + toRound   : "");
        save(null, tournamentId, GameAuditAction.GAMES_BULK_DELETED, actor, details);
    }

    @Async
    public void logSlotSwapped(Long gameId, Long tournamentId, User actor,
                               Integer slotNumber, Long oldUserId, Long newUserId) {
        String details = "slot=" + slotNumber
                + "; oldUserId=" + oldUserId
                + "; newUserId=" + newUserId;
        save(gameId, tournamentId, GameAuditAction.SLOT_PLAYER_SWAPPED, actor, details);
    }

    /**
     * Сравнивает старые слоты (снимок из БД до сохранения) с новыми (из DTO)
     * и логирует каждый слот, у которого изменились числовые баллы или фолы.
     *
     * <p>Работает как для черновиков, так и для уже завершённых игр (повторное редактирование).</p>
     *
     * <p>Формат записи details:
     * {@code slot=3; player=Феникс; extraPos: 0.5→0.3; extraNeg: 0.0→0.1}</p>
     *
     * @param gameId        ID игры
     * @param tournamentId  ID турнира
     * @param actor         кто сохраняет
     * @param oldSlots      слоты из БД ДО сохранения (снимок)
     * @param newSlotDtos   слоты из тела запроса
     */
    @Async
    public void logScoreChanges(Long gameId, Long tournamentId, User actor,
                                List<GameSlot> oldSlots,
                                List<com.mafia.manager.dto.GameSlotDto> newSlotDtos) {
        if (newSlotDtos == null || newSlotDtos.isEmpty()) return;

        // Индексируем старые слоты по номеру для O(1)-доступа
        Map<Integer, GameSlot> oldBySlotNumber = oldSlots.stream()
                .filter(s -> s.getSlotNumber() != null)
                .collect(Collectors.toMap(
                        GameSlot::getSlotNumber,
                        s -> s,
                        (a, b) -> a   // дубли не должны быть, но на всякий случай
                ));

        for (com.mafia.manager.dto.GameSlotDto newDto : newSlotDtos) {
            if (newDto.getSlotNumber() == null) continue;

            GameSlot old = oldBySlotNumber.get(newDto.getSlotNumber());
            if (old == null) continue;

            List<String> changes = buildScoreChanges(old, newDto);
            if (changes.isEmpty()) continue;

            // Имя игрока для контекста — берём из старого слота или из DTO
            String playerInfo = resolvePlayerName(old, newDto);

            String details = "slot=" + newDto.getSlotNumber()
                    + "; player=" + playerInfo
                    + "; " + String.join("; ", changes);

            // Одна запись лога на каждый изменённый слот
            save(gameId, tournamentId, GameAuditAction.SLOT_SCORES_CHANGED, actor, details);
        }
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    /**
     * Определяет имя игрока для аудит-лога.
     * Приоритет: никнейм из старого слота → никнейм из DTO → «slot-N».
     */
    private String resolvePlayerName(GameSlot old, com.mafia.manager.dto.GameSlotDto newDto) {
        if (old.getUser() != null && old.getUser().getNickname() != null) {
            return old.getUser().getNickname();
        }
        if (newDto.getPlayerNickname() != null && !newDto.getPlayerNickname().isBlank()) {
            return newDto.getPlayerNickname();
        }
        return "slot-" + newDto.getSlotNumber();
    }

    /**
     * Сравнивает числовые баллы и фолы одного слота.
     * Возвращает список строк вида {@code "extraPos: 0.5→0.3"}.
     * Пустой список означает — ничего не изменилось.
     */
    private List<String> buildScoreChanges(GameSlot old,
                                           com.mafia.manager.dto.GameSlotDto newDto) {
        List<String> changes = new ArrayList<>();

        // extra_points_positive
        BigDecimal oldPos = nullToZero(old.getExtraPointsPositive());
        BigDecimal newPos = nullToZero(newDto.getExtraPos());
        if (oldPos.compareTo(newPos) != 0) {
            changes.add("extraPos: " + fmt(oldPos) + "→" + fmt(newPos));
        }

        // extra_points_negative
        BigDecimal oldNeg = nullToZero(old.getExtraPointsNegative());
        BigDecimal newNeg = nullToZero(newDto.getExtraNeg());
        if (oldNeg.compareTo(newNeg) != 0) {
            changes.add("extraNeg: " + fmt(oldNeg) + "→" + fmt(newNeg));
        }

        // penalty_points
        BigDecimal oldPen = nullToZero(old.getPenaltyPoints());
        BigDecimal newPen = nullToZero(newDto.getPenalty());
        if (oldPen.compareTo(newPen) != 0) {
            changes.add("penalty: " + fmt(oldPen) + "→" + fmt(newPen));
        }

        // fouls — тоже меняется и важен для аудита
        int oldFouls = old.getFouls() != null ? old.getFouls() : 0;
        int newFouls = newDto.getFouls() != null ? newDto.getFouls() : 0;
        if (oldFouls != newFouls) {
            changes.add("fouls: " + oldFouls + "→" + newFouls);
        }

        return changes;
    }

    private BigDecimal nullToZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private String fmt(BigDecimal val) {
        return val.stripTrailingZeros().toPlainString();
    }

    private void save(Long gameId, Long tournamentId, GameAuditAction action,
                      User actor, String details) {
        try {
            GameAuditLog entry = GameAuditLog.of(
                    gameId, tournamentId, action,
                    actor != null ? actor.getId()       : null,
                    actor != null ? actor.getNickname() : "system",
                    details
            );
            auditLogRepository.save(entry);
            log.debug("[AUDIT] {} | game={} | tournament={} | actor={} | {}",
                    action, gameId, tournamentId,
                    actor != null ? actor.getNickname() : "system",
                    details);
        } catch (Exception ex) {
            // Лог не должен ронять основную логику
            log.error("[AUDIT] Failed to save audit log: action={}, gameId={}", action, gameId, ex);
        }
    }
}
