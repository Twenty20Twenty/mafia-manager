package com.mafia.manager.service;

import com.mafia.manager.dto.*;
import com.mafia.manager.entity.*;
import com.mafia.manager.entity.enums.*;
import com.mafia.manager.repository.*;
import lombok.RequiredArgsConstructor;
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

    // ── ЧТЕНИЕ ────────────────────────────────────────────────────────────────

    /**
     * Возвращает все игры турнира в виде списка протоколов.
     *
     * @param tournamentId идентификатор турнира
     * @return список DTO протоколов
     */
    public List<GameProtocolDto> getGamesByTournament(Long tournamentId) {
        return gameRepository.findByTournamentId(tournamentId).stream()
                .map(this::mapToProtocolDto)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает полный протокол одной игры (слоты + лучший ход).
     *
     * @param gameId идентификатор игры
     * @return DTO протокола
     * @throws NoSuchElementException если игра не найдена
     */
    public GameProtocolDto getProtocol(Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow();
        return mapToProtocolDto(game);
    }

    // ── СОЗДАНИЕ ──────────────────────────────────────────────────────────────

    /**
     * Создаёт новую пустую игру с 10 незаполненными слотами.
     *
     * <p>Игра получает статус {@code pending}, стадию {@code qualifying}.
     * Дата устанавливается из запроса или в {@code LocalDate.now()}.</p>
     *
     * @param tournamentId идентификатор турнира
     * @param date         дата игры (если {@code null} — сегодняшняя)
     * @param judgeId      идентификатор судьи
     * @return DTO созданной игры
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

        return mapToProtocolDto(game);
    }

    // ── СОХРАНЕНИЕ ПРОТОКОЛА ──────────────────────────────────────────────────

    /**
     * Сохраняет протокол игры (слоты, победитель, лучший ход).
     *
     * <p>Порядок операций критически важен для корректного срабатывания триггеров БД:</p>
     * <ol>
     *   <li>Обрабатываем смену «Первого убитого» — при изменении удаляем старый BestMove</li>
     *   <li>Обновляем данные всех слотов (игроки, роли, фолы, очки)</li>
     *   <li>Сохраняем или удаляем запись BestMove</li>
     *   <li><strong>Последним</strong> сохраняем саму игру — триггер сработает с актуальными слотами</li>
     *   <li>Для завершённых игр принудительно пересчитываем CI всех игроков через хранимую процедуру</li>
     * </ol>
     *
     * @param gameId идентификатор игры
     * @param dto    данные протокола из запроса
     */
    @Transactional
    public void saveProtocol(Long gameId, GameProtocolDto dto) {
        Game game = loadAndUpdateGameFields(gameId, dto);

        List<GameSlot> existingSlots = gameSlotRepository.findByGameId(gameId);
        handleFirstKilledChange(gameId, existingSlots, dto.getSlots());

        Map<Integer, GameSlot> slotsMap = existingSlots.stream()
                .collect(Collectors.toMap(GameSlot::getSlotNumber, Function.identity()));
        updateSlots(slotsMap, dto.getSlots());

        List<GameSlot> savedSlots = gameSlotRepository.findByGameId(gameId);
        updateBestMove(game, dto.getBestMove(), savedSlots);

        // Игра сохраняется ПОСЛЕДНЕЙ — это гарантирует корректный порядок триггеров
        gameRepository.save(game);

        if (GameStatus.completed.name().equals(dto.getStatus())) {
            recalculateAllSlotsForGame(gameId);
        }
    }

    // ── УДАЛЕНИЕ ──────────────────────────────────────────────────────────────

    /**
     * Удаляет игру (каскадно удаляет слоты и BestMove).
     *
     * @param id идентификатор игры
     */
    @Transactional
    public void deleteGame(Long id) {
        gameRepository.deleteById(id);
    }

    // ── ВЫЧИСЛЕНИЕ ОЧКОВ ЛХ ───────────────────────────────────────────────────

    /**
     * Вычисляет очки за «Лучший ход» по количеству угаданных чёрных среди трёх кандидатов.
     *
     * <p>Таблица начисления:</p>
     * <ul>
     *   <li>3 чёрных из 3 → 0.60</li>
     *   <li>2 чёрных из 3 → 0.30</li>
     *   <li>1 чёрный  из 3 → 0.10</li>
     *   <li>0             → 0.00</li>
     * </ul>
     *
     * <p>Метод объявлен с package-уровнем видимости (без {@code private}) для тестирования.</p>
     *
     * @param candidateSlotNumbers номера слотов кандидатов (1–10), может содержать {@code null}
     * @param allSlots             все слоты игры с назначенными ролями
     * @return очки за лучший ход
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

    /**
     * Загружает игру из БД и обновляет её поля из DTO (статус, победитель, дата, стадия).
     */
    private Game loadAndUpdateGameFields(Long gameId, GameProtocolDto dto) {
        Game game = gameRepository.findById(gameId).orElseThrow();
        game.setStatus(GameStatus.valueOf(dto.getStatus()));

        if (dto.getWinner() != null) game.setWinner(GameWinner.valueOf(dto.getWinner()));
        if (dto.getDate()   != null) game.setDate(dto.getDate());
        if (dto.getStage()  != null) game.setStage(GameStage.valueOf(dto.getStage()));

        return game;
    }

    /**
     * Обрабатывает изменение «Первого убитого».
     *
     * <p>Если ПУ сменился: удаляем BestMove (так как автор ЛХ — всегда ПУ)
     * и снимаем флаг {@code isFirstKilled} у старого слота.</p>
     */
    private void handleFirstKilledChange(Long gameId, List<GameSlot> existingSlots, List<GameSlotDto> newSlotDtos) {
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

    /**
     * Обновляет данные слотов из DTO-запроса.
     *
     * @param slotsMap    карта существующих слотов по номеру слота
     * @param slotDtoList список DTO слотов из запроса
     */
    private void updateSlots(Map<Integer, GameSlot> slotsMap, List<GameSlotDto> slotDtoList) {
        for (GameSlotDto slotDto : slotDtoList) {
            GameSlot slot = slotsMap.get(slotDto.getSlotNumber());
            if (slot == null) continue;

            slot.setUser(slotDto.getPlayerId() != null
                    ? userRepository.getReferenceById(slotDto.getPlayerId())
                    : null);

            if (slotDto.getRole() != null) {
                slot.setRole(PlayerRoleInGame.valueOf(slotDto.getRole()));
            }

            slot.setIsFirstKilled(Boolean.TRUE.equals(slotDto.getIsFirstKilled()));
            slot.setFouls(slotDto.getFouls());
            slot.setExtraPointsPositive(slotDto.getExtraPos());
            slot.setExtraPointsNegative(slotDto.getExtraNeg());
            slot.setPenaltyPoints(slotDto.getPenalty());

            gameSlotRepository.save(slot);
        }
    }

    /**
     * Сохраняет или удаляет запись BestMove в зависимости от наличия данных в DTO.
     */
    private void updateBestMove(Game game, BestMoveDto bmDto, List<GameSlot> savedSlots) {
        if (bmDto != null && bmDto.getAuthorId() != null) {
            saveBestMove(game, bmDto, savedSlots);
        } else {
            bestMoveRepository.deleteByGameId(game.getId());
        }
    }

    /**
     * Сохраняет запись BestMove.
     *
     * <p>Очки ({@code points}) вычисляются на сервере.
     * Клиентское значение полностью игнорируется.</p>
     */
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
                ? bmDto.getCandidates()
                : Collections.emptyList();

        // поля маппятся на candidate_1_slot, candidate_2_slot, candidate_3_slot в БД
        bm.setCandidate1Slot(candidates.size() > 0 ? candidates.get(0) : null);
        bm.setCandidate2Slot(candidates.size() > 1 ? candidates.get(1) : null);
        bm.setCandidate3Slot(candidates.size() > 2 ? candidates.get(2) : null);

        List<Integer> nonNullCandidates = candidates.stream().filter(Objects::nonNull).toList();

        // Вычисляем очки и количество угаданных чёрных на сервере
        bm.setPoints(calcBestMovePoints(nonNullCandidates, savedSlots));
        bm.setGuessedCount(countGuessedBlacks(nonNullCandidates, savedSlots));

        bestMoveRepository.save(bm);
    }

    /**
     * Считает количество угаданных чёрных среди кандидатов.
     */
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

    /**
     * Пересчитывает CI (Competitive Index) для всех игроков завершённой игры.
     * Вызывает хранимую процедуру {@code recalculate_player_ci} через репозиторий.
     */
    private void recalculateAllSlotsForGame(Long gameId) {
        Long tournamentId = gameRepository.findTournamentIdByGameId(gameId);
        List<Long> userIds = gameSlotRepository.findUserIdsByGameId(gameId);
        for (Long userId : userIds) {
            gameSlotRepository.callRecalculatePlayerCi(tournamentId, userId);
        }
    }

    /**
     * Создаёт 10 пустых слотов (1–10) для новой игры с ролью {@code civilian} по умолчанию.
     */
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

    /**
     * Определяет, изменился ли «Первый убитый» между сохранённым состоянием и новым запросом.
     *
     * @param oldFkSlot          слот старого ПУ (может быть {@code null})
     * @param newFkSlotNumber     номер нового слота ПУ (может быть {@code null})
     * @return {@code true}, если ПУ изменился
     */
    private boolean isFirstKilledChanged(GameSlot oldFkSlot, Integer newFkSlotNumber) {
        if (oldFkSlot == null && newFkSlotNumber == null) return false;
        if (oldFkSlot == null || newFkSlotNumber == null) return true;
        return !oldFkSlot.getSlotNumber().equals(newFkSlotNumber);
    }

    // ── МАППИНГ ───────────────────────────────────────────────────────────────

    /**
     * Преобразует сущность {@link Game} в полный DTO протокола (со слотами и ЛХ).
     */
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

    /**
     * Определяет коэффициент игры: для финальной стадии берёт значение из настроек турнира,
     * для отборочной — всегда 1.
     */
    private BigDecimal resolveFinalCoefficient(Game game) {
        if (game.getStage() == GameStage.final_round && game.getTournament().getSettings() != null) {
            Double fc = game.getTournament().getSettings().getFinalCoefficient();
            if (fc != null) return BigDecimal.valueOf(fc);
        }
        return BigDecimal.ONE;
    }

    /**
     * Загружает слоты игры из БД и возвращает отсортированный список DTO.
     */
    private List<GameSlotDto> loadAndMapSlots(Game game) {
        return gameSlotRepository.findByGameId(game.getId()).stream()
                .map(this::mapSlotDto)
                .sorted(Comparator.comparing(GameSlotDto::getSlotNumber))
                .collect(Collectors.toList());
    }

    /** Преобразует слот игры в DTO. */
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

// ── МАССОВОЕ УДАЛЕНИЕ ─────────────────────────────────────────────────────────

    /**
     * Удаляет игры турнира согласно режиму запроса.
     *
     * <ul>
     *   <li>{@code ALL}   — удаляет все игры турнира и сбрасывает флаг isSeedingGenerated</li>
     *   <li>{@code ROUND} — удаляет один тур (fromRound)</li>
     *   <li>{@code RANGE} — удаляет туры в диапазоне [fromRound, toRound]</li>
     * </ul>
     *
     * @throws IllegalArgumentException при некорректных параметрах диапазона
     */
    @Transactional
    public void deleteGames(Long tournamentId, DeleteGamesRequest req) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();

        switch (req.getMode()) {
            case ALL   -> deleteAllGames(t);
            case ROUND -> deleteRoundGames(t, req.getFromRound(), req.getFromRound());
            case RANGE -> deleteRoundGames(t, req.getFromRound(), req.getToRound());
        }
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

    /** Сбрасывает флаг сгенерированной рассадки в настройках турнира. */
    private void resetSeedingFlag(Tournament t) {
        if (t.getSettings() != null) {
            t.getSettings().setIsSeedingGenerated(false);
            tournamentRepository.save(t);
        }
    }

// ── ЗАМЕНА ИГРОКА В СЛОТЕ ────────────────────────────────────────────────────

    /**
     * Заменяет игрока в слоте уже созданной игры.
     *
     * <p>Если {@code slotNumber} задан — меняем именно этот слот.
     * Иначе ищем слот по {@code oldUserId}.
     * {@code newUserId == null} означает «освободить слот» (убрать игрока).</p>
     *
     * @throws IllegalStateException если игра завершена (status = completed)
     * @throws IllegalArgumentException если слот не найден
     */
    @Transactional
    public GameProtocolDto swapSlot(Long gameId, SwapSlotRequest req) {
        Game game = gameRepository.findById(gameId).orElseThrow();

        if (game.getStatus() == GameStatus.completed) {
            throw new IllegalStateException("Нельзя менять состав завершённой игры");
        }

        List<GameSlot> slots = gameSlotRepository.findByGameId(gameId);
        GameSlot target      = resolveTargetSlot(slots, req);

        User newUser = resolveNewUser(req.getNewUserId());
        target.setUser(newUser);
        gameSlotRepository.save(target);

        return mapToProtocolDto(game);
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

    /** Преобразует сущность {@link BestMove} в DTO. */
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
