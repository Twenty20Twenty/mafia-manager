package com.mafia.manager.service;

import com.mafia.manager.dto.*;
import com.mafia.manager.entity.*;
import com.mafia.manager.entity.enums.*;
import com.mafia.manager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис управления играми и протоколами.
 *
 * <p>Отвечает за:</p>
 * <ul>
 *   <li>Создание пустых игр с 10 слотами</li>
 *   <li>Сохранение/обновление протокола (слоты, роли, фолы, очки)</li>
 *   <li>Вычисление очков за «Лучший ход» на стороне сервера</li>
 *   <li>Удаление игр</li>
 *   <li>Маппинг игр в DTO</li>
 *   <li>Аудит-логирование всех изменяющих операций (через {@link GameAuditService})</li>
 * </ul>
 *
 * <p><strong>Важно:</strong> очки {@link BestMove#points} всегда вычисляются
 * на сервере — клиентское значение игнорируется.</p>
 */
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository       gameRepository;
    private final GameSlotRepository   gameSlotRepository;
    private final BestMoveRepository   bestMoveRepository;
    private final UserRepository       userRepository;
    private final TournamentRepository tournamentRepository;
    private final GameAuditService     auditService;

    // ── ЧТЕНИЕ ────────────────────────────────────────────────────────────────

    public List<GameProtocolDto> getGamesByTournament(Long tournamentId) {
        return gameRepository.findByTournamentId(tournamentId).stream()
                .map(this::mapToProtocolDto)
                .collect(Collectors.toList());
    }

    public GameProtocolDto getProtocol(Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow();
        return mapToProtocolDto(game);
    }

    // ── СОЗДАНИЕ ──────────────────────────────────────────────────────────────

    /**
     * Создаёт новую пустую игру с 10 незаполненными слотами.
     * Статус {@code pending}, стадия {@code qualifying}.
     */
    @Transactional
    public GameProtocolDto createGame(Long tournamentId, LocalDate date, Long judgeId) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow();
        User judge = userRepository.findById(judgeId).orElseThrow();

        Game game = new Game();
        game.setTournament(tournament);
        game.setDate(date != null ? date : LocalDate.now());
        game.setStatus(GameStatus.pending);
        game.setTableNumber(1);
        game.setStage(GameStage.qualifying);
        game.setJudge(judge);

        game = gameRepository.save(game);
        createEmptySlots(game);

        auditService.logGameCreated(game.getId(), tournamentId, getCurrentUser());

        return mapToProtocolDto(game);
    }

    // ── СОХРАНЕНИЕ ПРОТОКОЛА ──────────────────────────────────────────────────

    /**
     * Сохраняет протокол игры (draft или completed).
     *
     * <p>Порядок операций критически важен:</p>
     * <ol>
     *   <li>Снимаем состояние слотов из БД — это baseline для аудита баллов</li>
     *   <li>Обрабатываем смену «Первого убитого»</li>
     *   <li>Обновляем слоты новыми данными из DTO</li>
     *   <li>Сохраняем BestMove</li>
     *   <li>Сохраняем игру (триггер сработает с актуальными слотами)</li>
     *   <li>При completed — вызываем пересчёт CI</li>
     *   <li>Логируем изменения баллов (async, не блокирует ответ) — для draft и completed</li>
     *   <li>Логируем статус протокола</li>
     * </ol>
     */
    @Transactional
    public void saveProtocol(Long gameId, GameProtocolDto dto) {
        Game game = loadAndUpdateGameFields(gameId, dto);
        Long tournamentId = game.getTournament().getId();

        // 1. Снимок ПЕРЕД любыми изменениями — для аудита баллов
        List<GameSlot> slotsBeforeSave = gameSlotRepository.findByGameId(gameId);

        // 2. Смена ПУ
        handleFirstKilledChange(gameId, slotsBeforeSave, dto.getSlots());

        // 3. Обновляем слоты
        Map<Integer, GameSlot> slotsMap = slotsBeforeSave.stream()
                .collect(Collectors.toMap(GameSlot::getSlotNumber, Function.identity()));
        updateSlots(slotsMap, dto.getSlots());

        // 4. BestMove
        List<GameSlot> savedSlots = gameSlotRepository.findByGameId(gameId);
        updateBestMove(game, dto.getBestMove(), savedSlots);

        // 5. Сохраняем игру последней
        gameRepository.save(game);

        // 6. При completed — пересчёт CI
        if (GameStatus.completed.name().equals(dto.getStatus())) {
            recalculateAllSlotsForGame(gameId);
        }

        User actor = getCurrentUser();

        // 7. Аудит изменений баллов — всегда, не только при completed
        //    Это позволяет отследить правки в уже завершённых играх
        auditService.logScoreChanges(gameId, tournamentId, actor, slotsBeforeSave, dto.getSlots());

        // 8. Аудит статуса протокола
        auditService.logProtocolSaved(gameId, tournamentId, actor, dto.getStatus(), dto.getWinner());
    }

    // ── УДАЛЕНИЕ ──────────────────────────────────────────────────────────────

    @Transactional
    public void deleteGame(Long id) {
        Game game = gameRepository.findById(id).orElseThrow();
        Long tournamentId = game.getTournament().getId();

        gameRepository.deleteById(id);

        auditService.logGameDeleted(id, tournamentId, getCurrentUser());
    }

    // ── МАССОВОЕ УДАЛЕНИЕ ─────────────────────────────────────────────────────

    @Transactional
    public void deleteGames(Long tournamentId, DeleteGamesRequest req) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();

        switch (req.getMode()) {
            case ALL   -> deleteAllGames(t);
            case ROUND -> deleteRoundGames(t, req.getFromRound(), req.getFromRound());
            case RANGE -> deleteRoundGames(t, req.getFromRound(), req.getToRound());
        }

        auditService.logBulkDeleted(tournamentId, getCurrentUser(),
                req.getMode().name(), req.getFromRound(), req.getToRound());
    }

    private void deleteAllGames(Tournament t) {
        gameRepository.deleteAllByTournamentId(t.getId());
        resetSeedingFlag(t);
    }

    private void deleteRoundGames(Tournament t, Integer from, Integer to) {
        if (from == null || to == null || from < 1 || to < from) {
            throw new IllegalArgumentException(
                    "Некорректный диапазон туров: from=" + from + ", to=" + to);
        }
        gameRepository.deleteByTournamentIdAndRoundNumberBetween(t.getId(), from, to);
    }

    private void resetSeedingFlag(Tournament t) {
        if (t.getSettings() != null) {
            t.getSettings().setIsSeedingGenerated(false);
            tournamentRepository.save(t);
        }
    }

    // ── ЗАМЕНА ИГРОКА В СЛОТЕ ────────────────────────────────────────────────

    @Transactional
    public GameProtocolDto swapSlot(Long gameId, SwapSlotRequest req) {
        Game game = gameRepository.findById(gameId).orElseThrow();

        if (game.getStatus() == GameStatus.completed) {
            throw new IllegalStateException("Нельзя менять состав завершённой игры");
        }

        List<GameSlot> slots = gameSlotRepository.findByGameId(gameId);
        GameSlot target      = resolveTargetSlot(slots, req);

        Long oldUserId = target.getUser() != null ? target.getUser().getId() : null;
        User newUser   = resolveNewUser(req.getNewUserId());

        target.setUser(newUser);
        gameSlotRepository.save(target);

        auditService.logSlotSwapped(
                gameId, game.getTournament().getId(), getCurrentUser(),
                target.getSlotNumber(), oldUserId,
                newUser != null ? newUser.getId() : null
        );

        return mapToProtocolDto(game);
    }

    // ── ВЫЧИСЛЕНИЕ ОЧКОВ ЛХ ───────────────────────────────────────────────────

    /**
     * Вычисляет очки за «Лучший ход».
     * Объявлен package-private для тестирования.
     */
    static BigDecimal calcBestMovePoints(List<Integer> candidateSlotNumbers, List<GameSlot> allSlots) {
        if (candidateSlotNumbers == null || candidateSlotNumbers.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<Integer, GameSlot> slotByNumber = allSlots.stream()
                .collect(Collectors.toMap(GameSlot::getSlotNumber, Function.identity()));

        Set<PlayerRoleInGame> blackRoles = Set.of(PlayerRoleInGame.mafia, PlayerRoleInGame.don);

        long blackCount = candidateSlotNumbers.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(slotByNumber::get)
                .filter(Objects::nonNull)
                .filter(s -> s.getRole() != null && blackRoles.contains(s.getRole()))
                .count();

        if (blackCount >= 3) return new BigDecimal("0.60");
        if (blackCount == 2) return new BigDecimal("0.30");
        if (blackCount == 1) return new BigDecimal("0.10");
        return BigDecimal.ZERO;
    }

    // ── ВНУТРЕННЯЯ ЛОГИКА ─────────────────────────────────────────────────────

    private Game loadAndUpdateGameFields(Long gameId, GameProtocolDto dto) {
        Game game = gameRepository.findById(gameId).orElseThrow();
        game.setStatus(GameStatus.valueOf(dto.getStatus()));
        if (dto.getWinner() != null) game.setWinner(GameWinner.valueOf(dto.getWinner()));
        if (dto.getDate()   != null) game.setDate(dto.getDate());
        if (dto.getStage()  != null) game.setStage(GameStage.valueOf(dto.getStage()));
        return game;
    }

    private void handleFirstKilledChange(Long gameId, List<GameSlot> existingSlots,
                                          List<GameSlotDto> newSlotDtos) {
        GameSlot oldFkSlot = existingSlots.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsFirstKilled()))
                .findFirst()
                .orElse(null);

        Integer newFkSlotNumber = newSlotDtos.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsFirstKilled()))
                .map(GameSlotDto::getSlotNumber)
                .findFirst()
                .orElse(null);

        if (isFirstKilledChanged(oldFkSlot, newFkSlotNumber) && oldFkSlot != null) {
            bestMoveRepository.deleteByGameId(gameId);
            oldFkSlot.setIsFirstKilled(false);
            gameSlotRepository.save(oldFkSlot);
        }
    }

    private void updateSlots(Map<Integer, GameSlot> slotsMap, List<GameSlotDto> slotDtoList) {
        for (GameSlotDto slotDto : slotDtoList) {
            GameSlot slot = slotsMap.get(slotDto.getSlotNumber());
            if (slot == null) continue;

            slot.setUser(slotDto.getPlayerId() != null
                    ? userRepository.getReferenceById(slotDto.getPlayerId()) : null);

            if (slotDto.getRole() != null) slot.setRole(PlayerRoleInGame.valueOf(slotDto.getRole()));
            slot.setIsFirstKilled(Boolean.TRUE.equals(slotDto.getIsFirstKilled()));
            slot.setFouls(slotDto.getFouls());
            slot.setExtraPointsPositive(slotDto.getExtraPos());
            slot.setExtraPointsNegative(slotDto.getExtraNeg());
            slot.setPenaltyPoints(slotDto.getPenalty());

            gameSlotRepository.save(slot);
        }
    }

    private void updateBestMove(Game game, BestMoveDto bmDto, List<GameSlot> savedSlots) {
        if (bmDto != null && bmDto.getAuthorId() != null) {
            saveBestMove(game, bmDto, savedSlots);
        } else {
            bestMoveRepository.deleteByGameId(game.getId());
        }
    }

    private void saveBestMove(Game game, BestMoveDto bmDto, List<GameSlot> savedSlots) {
        GameSlot authorSlot = savedSlots.stream()
                .filter(s -> s.getUser() != null && s.getUser().getId().equals(bmDto.getAuthorId()))
                .findFirst()
                .orElse(null);
        if (authorSlot == null) return;

        BestMove bm = bestMoveRepository.findByGameId(game.getId()).orElse(new BestMove());
        bm.setGame(game);
        bm.setAuthorSlot(authorSlot);

        List<Integer> candidates = bmDto.getCandidates() != null
                ? bmDto.getCandidates() : Collections.emptyList();

        bm.setCandidate1Slot(candidates.size() > 0 ? candidates.get(0) : null);
        bm.setCandidate2Slot(candidates.size() > 1 ? candidates.get(1) : null);
        bm.setCandidate3Slot(candidates.size() > 2 ? candidates.get(2) : null);

        List<Integer> nonNullCandidates = candidates.stream().filter(Objects::nonNull).toList();
        bm.setPoints(calcBestMovePoints(nonNullCandidates, savedSlots));
        bm.setGuessedCount(countGuessedBlacks(nonNullCandidates, savedSlots));

        bestMoveRepository.save(bm);
    }

    private int countGuessedBlacks(List<Integer> candidateSlotNumbers, List<GameSlot> allSlots) {
        Map<Integer, GameSlot> slotByNumber = allSlots.stream()
                .collect(Collectors.toMap(GameSlot::getSlotNumber, Function.identity()));
        Set<PlayerRoleInGame> blackRoles = Set.of(PlayerRoleInGame.mafia, PlayerRoleInGame.don);
        return (int) candidateSlotNumbers.stream()
                .distinct()
                .map(slotByNumber::get)
                .filter(Objects::nonNull)
                .filter(s -> s.getRole() != null && blackRoles.contains(s.getRole()))
                .count();
    }

    private void recalculateAllSlotsForGame(Long gameId) {
        Long tournamentId = gameRepository.findTournamentIdByGameId(gameId);
        List<Long> userIds = gameSlotRepository.findUserIdsByGameId(gameId);
        for (Long userId : userIds) {
            gameSlotRepository.callRecalculatePlayerCi(tournamentId, userId);
        }
    }

    private void createEmptySlots(Game game) {
        List<GameSlot> slots = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            GameSlot slot = new GameSlot();
            slot.setGame(game);
            slot.setSlotNumber(i);
            slot.setRole(PlayerRoleInGame.civilian);
            slots.add(slot);
        }
        gameSlotRepository.saveAll(slots);
    }

    private boolean isFirstKilledChanged(GameSlot oldFkSlot, Integer newFkSlotNumber) {
        if (oldFkSlot == null && newFkSlotNumber == null) return false;
        if (oldFkSlot == null || newFkSlotNumber == null) return true;
        return !oldFkSlot.getSlotNumber().equals(newFkSlotNumber);
    }

    private GameSlot resolveTargetSlot(List<GameSlot> slots, SwapSlotRequest req) {
        if (req.getSlotNumber() != null) {
            return slots.stream()
                    .filter(s -> s.getSlotNumber().equals(req.getSlotNumber()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Слот " + req.getSlotNumber() + " не найден"));
        }
        if (req.getOldUserId() == null) {
            throw new IllegalArgumentException("Необходимо указать slotNumber или oldUserId");
        }
        return slots.stream()
                .filter(s -> s.getUser() != null && s.getUser().getId().equals(req.getOldUserId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Игрок " + req.getOldUserId() + " не найден в этой игре"));
    }

    private User resolveNewUser(Long newUserId) {
        if (newUserId == null) return null;
        return userRepository.findById(newUserId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь " + newUserId + " не найден"));
    }

    /** Возвращает текущего аутентифицированного пользователя. null при анонимном доступе. */
    private User getCurrentUser() {
        try {
            String nick = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByNickname(nick).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // ── МАППИНГ ───────────────────────────────────────────────────────────────

    private GameProtocolDto mapToProtocolDto(Game game) {
        GameProtocolDto dto = new GameProtocolDto();
        dto.setId(game.getId());
        dto.setTournamentId(game.getTournament().getId());
        dto.setRound(game.getRoundNumber());
        dto.setTable(game.getTableNumber());
        dto.setDate(game.getDate());
        dto.setStatus(game.getStatus() != null ? game.getStatus().name() : null);
        dto.setWinner(game.getWinner() != null ? game.getWinner().name() : null);
        dto.setStage(game.getStage() != null ? game.getStage().name() : GameStage.qualifying.name());

        if (game.getJudge() != null) {
            dto.setJudgeId(game.getJudge().getId());
            dto.setJudgeName(game.getJudge().getNickname());
        }

        dto.setCoefficient(resolveFinalCoefficient(game));
        dto.setSlots(loadAndMapSlots(game));

        bestMoveRepository.findByGameId(game.getId())
                .ifPresent(bm -> dto.setBestMove(mapBestMoveDto(bm)));

        return dto;
    }

    private BigDecimal resolveFinalCoefficient(Game game) {
        if (game.getStage() == GameStage.final_round
                && game.getTournament().getSettings() != null) {
            Double fc = game.getTournament().getSettings().getFinalCoefficient();
            if (fc != null) return BigDecimal.valueOf(fc);
        }
        return BigDecimal.ONE;
    }

    private List<GameSlotDto> loadAndMapSlots(Game game) {
        return gameSlotRepository.findByGameId(game.getId()).stream()
                .map(this::mapSlotDto)
                .sorted(Comparator.comparing(GameSlotDto::getSlotNumber))
                .collect(Collectors.toList());
    }

    private GameSlotDto mapSlotDto(GameSlot s) {
        GameSlotDto d = new GameSlotDto();
        d.setId(s.getId());
        d.setSlotNumber(s.getSlotNumber());
        if (s.getUser() != null) {
            d.setPlayerId(s.getUser().getId());
            d.setPlayerNickname(s.getUser().getNickname());
            d.setPlayerAvatar(s.getUser().getAvatarUrl());
        }
        d.setRole(s.getRole() != null ? s.getRole().name() : null);
        d.setIsFirstKilled(s.getIsFirstKilled());
        d.setFouls(s.getFouls());
        d.setExtraPos(s.getExtraPointsPositive());
        d.setExtraNeg(s.getExtraPointsNegative());
        d.setPenalty(s.getPenaltyPoints());
        d.setComputedScore(s.getComputedScore());
        d.setCompensationPoints(s.getCompensationPoints());
        return d;
    }

    private BestMoveDto mapBestMoveDto(BestMove bm) {
        BestMoveDto bmDto = new BestMoveDto();
        if (bm.getAuthorSlot() != null && bm.getAuthorSlot().getUser() != null) {
            bmDto.setAuthorId(bm.getAuthorSlot().getUser().getId());
        }
        List<Integer> cands = new ArrayList<>();
        if (bm.getCandidate1Slot() != null) cands.add(bm.getCandidate1Slot());
        if (bm.getCandidate2Slot() != null) cands.add(bm.getCandidate2Slot());
        if (bm.getCandidate3Slot() != null) cands.add(bm.getCandidate3Slot());
        bmDto.setCandidates(cands);
        bmDto.setGuessedCount(bm.getGuessedCount());
        bmDto.setPoints(bm.getPoints());
        return bmDto;
    }
}
