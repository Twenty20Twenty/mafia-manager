package com.mafia.manager.service;

import com.mafia.manager.dto.PlayerStatsDto;
import com.mafia.manager.entity.*;
import com.mafia.manager.entity.enums.GameWinner;
import com.mafia.manager.entity.enums.ParticipantStatus;
import com.mafia.manager.entity.enums.PlayerRoleInGame;
import com.mafia.manager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис статистики игроков.
 *
 * <p>Агрегирует статистику по завершённым играм и хранит её в таблице {@code player_stats}
 * в разрезе периодов: «за всё время» (periodYear = null) и «за конкретный год».</p>
 *
 * <p><strong>Алгоритм пересчёта для одного игрока:</strong></p>
 * <ol>
 *   <li>Загружаем все слоты игрока в завершённых играх (все турниры, все годы)</li>
 *   <li>Агрегируем данные в памяти по периодам (карта {@code Short → MutableStats})</li>
 *   <li>Upsert записей в {@code player_stats}: обновляем существующие или создаём новые</li>
 * </ol>
 *
 * <p>Пересчёт вызывается автоматически при переводе турнира в статус {@code completed}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerStatsService {

    private final PlayerStatsRepository          playerStatsRepository;
    private final TournamentParticipantRepository participantRepository;
    private final GameRepository                 gameRepository;
    private final GameSlotRepository             gameSlotRepository;
    private final BestMoveRepository             bestMoveRepository;
    private final UserRepository                 userRepository;

    // ── ПУБЛИЧНЫЙ API ─────────────────────────────────────────────────────────

    /**
     * Возвращает всю статистику игрока по всем периодам.
     *
     * <p>Результат отсортирован: сначала «за всё время» (periodYear = null),
     * затем по возрастанию года.</p>
     *
     * @param userId идентификатор пользователя
     * @return список DTO статистики (пустой список, если статистики нет)
     */
    public List<PlayerStatsDto> getStats(Long userId) {
        return playerStatsRepository.findByUserId(userId).stream()
                .map(PlayerStatsDto::fromEntity)
                .sorted(Comparator.comparingInt(s -> s.getPeriodYear() == null ? 0 : s.getPeriodYear()))
                .collect(Collectors.toList());
    }

    /**
     * Пересчитывает статистику для всех одобренных участников турнира.
     *
     * <p>Вызывается при переводе турнира в статус {@code completed}.
     * Ошибки для отдельных участников логируются, но не прерывают пересчёт для остальных.</p>
     *
     * @param tournamentId идентификатор турнира
     */
    @Transactional
    public void recalculateForTournament(Long tournamentId) {
        log.info("Запуск пересчёта статистики для участников турнира {}", tournamentId);

        List<Long> participantUserIds = loadApprovedParticipantIds(tournamentId);

        if (participantUserIds.isEmpty()) {
            log.warn("Турнир {} не имеет одобренных участников — пересчёт пропущен", tournamentId);
            return;
        }

        log.info("Пересчёт для {} участников турнира {}", participantUserIds.size(), tournamentId);

        for (Long userId : participantUserIds) {
            try {
                recalculateForUser(userId);
            } catch (Exception e) {
                log.error("Ошибка пересчёта статистики для userId={}: {}", userId, e.getMessage(), e);
            }
        }

        log.info("Пересчёт статистики для турнира {} завершён", tournamentId);
    }

    // ── ВНУТРЕННЯЯ ЛОГИКА ─────────────────────────────────────────────────────

    /**
     * Пересчитывает ВСЮ статистику одного игрока по всем его завершённым играм.
     *
     * <p>Если у игрока нет ни одной игры — пересчёт пропускается.</p>
     */
    private void recalculateForUser(Long userId) {
        User user = userRepository.getReferenceById(userId);

        List<GameSlot> allSlots = gameSlotRepository.findCompletedSlotsByUserId(userId);
        if (allSlots.isEmpty()) return;

        Set<Long> gameIdsWithBestMove        = bestMoveRepository.findGameIdsWhereAuthorUserId(userId);
        Set<Long> gameIdsWithPerfectBestMove  = bestMoveRepository.findGameIdsWherePerfectBestMoveByUserId(userId);

        Map<Short, MutableStats> statsByPeriod = aggregateStats(allSlots, gameIdsWithBestMove, gameIdsWithPerfectBestMove);

        persistStats(user, userId, statsByPeriod);
    }

    /**
     * Агрегирует все слоты игрока в карту периодов.
     *
     * <p>Ключ {@code null} — «за всё время», числовой ключ — за конкретный год.</p>
     */
    private Map<Short, MutableStats> aggregateStats(
            List<GameSlot> allSlots,
            Set<Long> gameIdsWithBestMove,
            Set<Long> gameIdsWithPerfectBestMove
    ) {
        Map<Short, MutableStats> statsByPeriod = new HashMap<>();
        statsByPeriod.put(null, new MutableStats()); // запись «за всё время» всегда присутствует

        for (GameSlot slot : allSlots) {
            Game game = slot.getGame();
            if (game == null || game.getWinner() == null) continue;

            Short year = game.getDate() != null ? (short) game.getDate().getYear() : null;

            updateStats(statsByPeriod.get(null), slot, game, gameIdsWithBestMove, gameIdsWithPerfectBestMove);

            if (year != null) {
                statsByPeriod.computeIfAbsent(year, k -> new MutableStats());
                updateStats(statsByPeriod.get(year), slot, game, gameIdsWithBestMove, gameIdsWithPerfectBestMove);
            }
        }

        return statsByPeriod;
    }

    /**
     * Сохраняет (upsert) записи статистики в БД для каждого периода.
     */
    private void persistStats(User user, Long userId, Map<Short, MutableStats> statsByPeriod) {
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<Short, MutableStats> entry : statsByPeriod.entrySet()) {
            Short period = entry.getKey();
            MutableStats agg = entry.getValue();

            PlayerStats ps = playerStatsRepository
                    .findByUserIdAndPeriodYear(userId, period)
                    .orElse(new PlayerStats());

            applyAggToEntity(ps, user, period, agg, now);
            playerStatsRepository.save(ps);
        }
    }

    /**
     * Применяет значения агрегата к сущности {@link PlayerStats}.
     */
    private void applyAggToEntity(PlayerStats ps, User user, Short period, MutableStats agg, LocalDateTime now) {
        ps.setUser(user);
        ps.setPeriodYear(period);
        ps.setTotalGames(agg.totalGames);
        ps.setGamesCivilian(agg.gamesCivilian);
        ps.setGamesSheriff(agg.gamesSheriff);
        ps.setGamesMafia(agg.gamesMafia);
        ps.setGamesDon(agg.gamesDon);
        ps.setWinsCivilian(agg.winsCivilian);
        ps.setWinsSheriff(agg.winsSheriff);
        ps.setWinsMafia(agg.winsMafia);
        ps.setWinsDon(agg.winsDon);
        ps.setBestMovesTotal(agg.bestMovesTotal);
        ps.setBestMovesPerfect(agg.bestMovesPerfect);
        ps.setFirstKilledCount(agg.firstKilledCount);
        ps.setTotalFouls(agg.totalFouls);
        ps.setLastRecalculatedAt(now);
    }

    /**
     * Обновляет агрегатор данными одного слота/игры.
     *
     * <p>Лучший ход засчитывается только «Первому убитому» (он является автором ЛХ).</p>
     */
    private void updateStats(
            MutableStats stats,
            GameSlot slot,
            Game game,
            Set<Long> gameIdsWithBestMove,
            Set<Long> gameIdsWithPerfectBestMove
    ) {
        stats.totalGames++;

        accumulateRoleStats(stats, slot.getRole(), game.getWinner());
        accumulateBestMoveStats(stats, slot, game.getId(), gameIdsWithBestMove, gameIdsWithPerfectBestMove);
        accumulateFouls(stats, slot.getFouls());
    }

    /**
     * Увеличивает счётчики игр и побед по роли.
     */
    private void accumulateRoleStats(MutableStats stats, PlayerRoleInGame role, GameWinner winner) {
        switch (role) {
            case civilian -> { stats.gamesCivilian++; if (winner == GameWinner.red)   stats.winsCivilian++; }
            case sheriff  -> { stats.gamesSheriff++;  if (winner == GameWinner.red)   stats.winsSheriff++;  }
            case mafia    -> { stats.gamesMafia++;    if (winner == GameWinner.black) stats.winsMafia++;    }
            case don      -> { stats.gamesDon++;      if (winner == GameWinner.black) stats.winsDon++;      }
        }
    }

    /**
     * Засчитывает лучший ход и «идеальный ЛХ» (3 из 3) для «Первого убитого».
     */
    private void accumulateBestMoveStats(
            MutableStats stats,
            GameSlot slot,
            Long gameId,
            Set<Long> gameIdsWithBestMove,
            Set<Long> gameIdsWithPerfectBestMove
    ) {
        if (!Boolean.TRUE.equals(slot.getIsFirstKilled())) return;

        stats.firstKilledCount++;

        if (gameIdsWithBestMove.contains(gameId)) {
            stats.bestMovesTotal++;
            if (gameIdsWithPerfectBestMove.contains(gameId)) {
                stats.bestMovesPerfect++;
            }
        }
    }

    /**
     * Прибавляет фолы из слота к накопителю (учитывает {@code null}).
     */
    private void accumulateFouls(MutableStats stats, Integer fouls) {
        if (fouls != null) {
            stats.totalFouls += fouls;
        }
    }

    /**
     * Загружает идентификаторы одобренных участников турнира.
     */
    private List<Long> loadApprovedParticipantIds(Long tournamentId) {
        return participantRepository
                .findByTournamentIdAndStatus(tournamentId, ParticipantStatus.approved)
                .stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toList());
    }

    // ── ВСПОМОГАТЕЛЬНЫЙ КЛАСС ─────────────────────────────────────────────────

    /**
     * Изменяемый аккумулятор статистики за один период.
     *
     * <p>Используется только в рамках одного вызова {@link #recalculateForUser(Long)} —
     * не является потокобезопасным и не должен выходить за пределы метода.</p>
     */
    private static class MutableStats {
        int totalGames    = 0;
        int gamesCivilian = 0, gamesSheriff = 0, gamesMafia = 0, gamesDon = 0;
        int winsCivilian  = 0, winsSheriff  = 0, winsMafia  = 0, winsDon  = 0;
        int bestMovesTotal = 0, bestMovesPerfect = 0;
        int firstKilledCount = 0;
        int totalFouls = 0;
    }
}
