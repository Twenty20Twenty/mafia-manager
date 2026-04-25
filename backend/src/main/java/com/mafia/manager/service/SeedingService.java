package com.mafia.manager.service;

import com.mafia.manager.dto.LeaderboardEntryDto;
import com.mafia.manager.entity.*;
import com.mafia.manager.entity.enums.*;
import com.mafia.manager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис генерации рассадки (сидинга) игроков по столам.
 *
 * <p>Поддерживает три режима:</p>
 * <ul>
 *   <li><strong>Balanced (обычный)</strong> — равномерное распределение, минимизирующее
 *       повторные пересечения игроков за одним столом и повторные слоты одного игрока</li>
 *   <li><strong>Swiss</strong> — швейцарская система: внутри тира игроки распределяются
 *       случайно, тиры формируются по текущему рейтингу лидерборда</li>
 *   <li><strong>Final Round</strong> — финальная рассадка топ-10 игроков без учёта истории слотов</li>
 * </ul>
 *
 * <p><strong>Алгоритм (Constraints Solver) с фоллбэком на слоты:</strong></p>
 * <ol>
 *   <li>Перемешиваем игроков случайно</li>
 *   <li>Для каждого игрока выбираем стол с наименьшим числом прошлых пересечений</li>
 *   <li>Ищем свободный <em>уникальный</em> слот (1–10), на котором игрок ещё не сидел</li>
 *   <li>Если уникальных слотов нет — берём любой свободный слот (фоллбэк для 11+ туров)</li>
 *   <li>Проверяем жёсткие ограничения: исключения «игрок–игрок» и командные квоты</li>
 *   <li>При неудаче за 50 попыток — {@link RuntimeException}</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeedingService {

    private final TournamentRepository            tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final GameRepository                  gameRepository;
    private final GameSlotRepository              gameSlotRepository;
    private final SeedingExceptionRepository      exceptionRepository;
    private final UserRepository                  userRepository;

    /** Максимальное число попыток рассадки перед ошибкой. */
    private static final int MAX_SEEDING_ATTEMPTS = 100;


    /**
     * Генерирует следующий тур (или несколько туров) для турнира.
     *
     * <p>Если в настройках включена швейцарская система и следующий тур
     * соответствует или превышает {@code swissRoundsStart} — генерируется
     * один швейцарский тур. В остальных случаях генерируются все оставшиеся
     * балансированные туры до {@code roundsCount} (или до начала швейцарки).</p>
     *
     * @param tournamentId идентификатор турнира
     * @throws RuntimeException если количество участников не кратно 10
     */
    @Transactional
    public void generateNextRound(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();

        List<TournamentSeedingException> exceptions  = exceptionRepository.findByTournamentId(tournamentId);
        List<Game> historyGames                       = gameRepository.findByTournamentId(tournamentId);
        List<TournamentParticipant> participants      = loadApprovedParticipants(tournamentId);

        validateParticipantCount(participants.size());

        Map<Integer, Long> tableJudges = buildTableJudgesMap(t);
        Map<Long, Long>    playerTeams = buildPlayerTeamsMap(t, participants);

        int nextRound     = getNextRoundNumber(historyGames);
        int roundsCount   = resolveRoundsCount(t);
        int swissStart    = resolveSwissStart(t, roundsCount);
        int balancedLimit = Math.min(swissStart - 1, roundsCount);

        if (Boolean.TRUE.equals(t.getSettings().getIsSwissSystem()) && nextRound >= swissStart) {
            generateSwissRound(t, nextRound, participants, exceptions, historyGames, tableJudges, playerTeams);
        } else {
            generateAllBalancedRounds(t, tournamentId, nextRound, balancedLimit,
                    participants, exceptions, tableJudges, playerTeams);
        }
    }


    /**
     * Генерирует финальный тур для топ-10 игроков, зафиксированных в настройках турнира.
     *
     * <p>Финальная рассадка не учитывает историю слотов — игрок может попасть на любой слот.
     * Исключения также не применяются.</p>
     *
     * @param tournamentId идентификатор турнира
     * @throws RuntimeException если топ-10 не были предварительно зафиксированы
     */
    @Transactional
    public void generateFinalRound(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();

        List<Long> top10Ids = resolveTop10Ids(t);

        List<User> top10Players = top10Ids.stream()
                .map(uid -> userRepository.findById(uid)
                        .orElseThrow(() -> new RuntimeException("Участник не найден: " + uid)))
                .collect(Collectors.toList());

        Map<Integer, Long> tableJudges = buildTableJudgesMap(t);
        int nextRound = getNextRoundNumber(gameRepository.findByTournamentId(tournamentId));

        // Финал: 1 стол, без исключений, без истории слотов, не Swiss
        Map<Integer, User[]> seating = runSeedingAlgorithm(
                top10Players,
                1,
                1,
                List.of(),
                List.of(),
                false,
                tableJudges,
                t.getType(),
                new HashMap<>()
        );

        saveSeedingToDb(t, nextRound, seating, tableJudges, GameStage.final_round);
    }

    // ── ЛОГИКА ГЕНЕРАЦИИ ─────────────────────────────────────────────────────

    /**
     * Генерирует все оставшиеся балансированные туры.
     *
     * <p>После каждого сгенерированного тура история перезагружается из БД,
     * чтобы алгоритм учитывал свежие слоты.</p>
     */
    private void generateAllBalancedRounds(
            Tournament t, Long tournamentId, int startRound, int limitRound,
            List<TournamentParticipant> participants,
            List<TournamentSeedingException> exceptions,
            Map<Integer, Long> tableJudges,
            Map<Long, Long> playerTeams
    ) {
        List<Game> historyGames = gameRepository.findByTournamentId(tournamentId);

        for (int r = startRound; r <= limitRound; r++) {
            generateBalancedRound(t, r, participants, exceptions, historyGames, tableJudges, playerTeams);
            historyGames = gameRepository.findByTournamentId(tournamentId);
        }
    }

    /**
     * Генерирует один балансированный тур: все участники, один пул, все столы.
     */
    private void generateBalancedRound(
            Tournament t, int roundNum,
            List<TournamentParticipant> participants,
            List<TournamentSeedingException> exceptions,
            List<Game> history,
            Map<Integer, Long> tableJudges,
            Map<Long, Long> playerTeams
    ) {
        log.info("Generating Balanced Round {}", roundNum);

        List<User> pool = participants.stream()
                .map(TournamentParticipant::getUser)
                .collect(Collectors.toList());

        int tablesCount = pool.size() / 10;

        Map<Integer, User[]> seating = runSeedingAlgorithm(
                pool, tablesCount, 1, exceptions, history, false, tableJudges, t.getType(), playerTeams
        );

        saveSeedingToDb(t, roundNum, seating, tableJudges, GameStage.qualifying);
    }

    /**
     * Генерирует один швейцарский тур, разбивая участников на тиры по рейтингу.
     *
     * <p>Каждый тир рассаживается независимо. Внутри тира балансировка случайная.</p>
     */
    private void generateSwissRound(
            Tournament t, int roundNum,
            List<TournamentParticipant> participants,
            List<TournamentSeedingException> exceptions,
            List<Game> history,
            Map<Integer, Long> tableJudges,
            Map<Long, Long> playerTeams
    ) {
        log.info("Generating Swiss Round {}", roundNum);

        List<LeaderboardEntryDto> leaderboard = tournamentRepository.getLeaderboard(t.getId(), false, "total");
        Map<Long, User> userMap = participants.stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), TournamentParticipant::getUser));

        List<Integer> tiers = t.getSettings().getSwissTiers();
        int currentRankOffset  = 0;
        int currentTableOffset = 1;

        for (Integer tierSize : tiers) {
            List<User> tierPool = buildTierPool(leaderboard, userMap, currentRankOffset, tierSize);
            int tablesInTier = tierSize / 10;

            Map<Integer, User[]> tierSeating = runSeedingAlgorithm(
                    tierPool, tablesInTier, currentTableOffset, exceptions, history, true, tableJudges, t.getType(), playerTeams
            );
            saveSeedingToDb(t, roundNum, tierSeating, tableJudges, GameStage.qualifying);

            currentRankOffset  += tierSize;
            currentTableOffset += tablesInTier;
        }
    }

    // ── АЛГОРИТМ РАССАДКИ (CONSTRAINTS SOLVER) ───────────────────────────────

    /**
     * Запускает алгоритм рассадки с повторными попытками.
     *
     * <p>При каждой неудаче (жёсткие ограничения не позволили никуда сесть)
     * делается новая попытка с новым перемешиванием.</p>
     *
     * @throws RuntimeException если за {@value #MAX_SEEDING_ATTEMPTS} попыток рассадка не получилась
     */
    private Map<Integer, User[]> runSeedingAlgorithm(
            List<User> players, int tablesCount, int startTableNum,
            List<TournamentSeedingException> exceptions,
            List<Game> history,
            boolean isSwiss,
            Map<Integer, Long> tableJudges,
            TournamentType type,
            Map<Long, Long> playerTeams
    ) {
        SeedingHistoryInfo historyInfo = buildHistoryInfo(history);

        for (int attempt = 0; attempt < MAX_SEEDING_ATTEMPTS; attempt++) {
            try {
                return trySeeding(players, tablesCount, startTableNum, exceptions, historyInfo, isSwiss, tableJudges, type, playerTeams);
            } catch (Exception ignored) {
                // Неудача — пробуем снова с новым перемешиванием
            }
        }

        throw new RuntimeException(
                "Не удалось сгенерировать валидную рассадку за " + MAX_SEEDING_ATTEMPTS
                + " попыток. Проверьте исключения и командные квоты."
        );
    }

    /**
     * Одна попытка рассадить всех игроков по столам и слотам.
     *
     * <p>Игроки перемешиваются случайно, затем каждый последовательно
     * размещается в первый подходящий слот с минимальными пересечениями.</p>
     *
     * @throws RuntimeException если хотя бы один игрок не смог сесть («Retry»)
     */
    private Map<Integer, User[]> trySeeding(
            List<User> players, int tablesCount, int startTableNum,
            List<TournamentSeedingException> exceptions,
            SeedingHistoryInfo historyInfo,
            boolean isSwiss,
            Map<Integer, Long> tableJudges,
            TournamentType type,
            Map<Long, Long> playerTeams
    ) {
        Map<Integer, User[]> result = initEmptyTables(tablesCount, startTableNum);

        List<User> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        for (User player : shuffledPlayers) {
            boolean seated = seatPlayer(player, result, historyInfo, exceptions, tableJudges, isSwiss, type, playerTeams);
            if (!seated) throw new RuntimeException("Retry");
        }

        return result;
    }

    /**
     * Пытается посадить одного игрока за любой подходящий стол и слот.
     *
     * <p><strong>Логика выбора слота (два прохода):</strong></p>
     * <ol>
     *   <li><em>Первый проход</em> — ищем свободный уникальный слот (не занятый игроком ранее).
     *       Это мягкое требование: соблюдаем если возможно.</li>
     *   <li><em>Фоллбэк</em> — если ни за одним из подходящих столов уникального слота нет,
     *       берём любой свободный слот (без учёта истории). Это необходимо при 11+ турах,
     *       когда игрок уже занимал все 10 позиций.</li>
     * </ol>
     *
     * <p>Жёсткие ограничения (исключения «игрок–игрок», командные квоты) соблюдаются
     * в обоих проходах.</p>
     *
     * @return {@code true}, если игрок успешно размещён
     */
    private boolean seatPlayer(
            User player,
            Map<Integer, User[]> tables,
            SeedingHistoryInfo historyInfo,
            List<TournamentSeedingException> exceptions,
            Map<Integer, Long> tableJudges,
            boolean isSwiss,
            TournamentType type,
            Map<Long, Long> playerTeams
    ) {
        Set<Integer> usedSlots = historyInfo.playedSlots.getOrDefault(player.getId(), new HashSet<>());
        List<Integer> tableOrder = getPreferredTableOrder(player, tables, historyInfo.intersections, isSwiss);

        // ── Первый проход: уникальный слот ───────────────────────────────────
        for (Integer tableNum : tableOrder) {
            User[] table = tables.get(tableNum);
            List<User> seated = getSeatedPlayers(table);

            if (seated.size() >= 10) continue;

            Long judgeId = tableJudges.get(tableNum);
            if (!checkHardConstraints(player, seated, exceptions, judgeId, type, playerTeams)) continue;

            List<Integer> freeUniqueSlots = findFreeUniqueSlots(table, usedSlots);
            if (!freeUniqueSlots.isEmpty()) {
                Collections.shuffle(freeUniqueSlots);
                table[freeUniqueSlots.get(0)] = player;
                return true;
            }
        }

        if (exceptions.isEmpty() && Boolean.FALSE.equals(isSwiss) && TournamentType.individual.equals(type)
                && usedSlots.size() < 10) {
            return false;
        }

        // ── Фоллбэк: любой свободный слот (для 11+ туров) ───────────────────
        // При 11+ турах игрок уже побывал на всех 10 позициях — уникальности
        // больше нет, берём любой свободный слот за подходящим столом.
        log.debug("Player {} has no unique slots left, using fallback to any free slot", player.getNickname());

        for (Integer tableNum : tableOrder) {
            User[] table = tables.get(tableNum);
            List<User> seated = getSeatedPlayers(table);

            if (seated.size() >= 10) continue;

            Long judgeId = tableJudges.get(tableNum);
            if (!checkHardConstraints(player, seated, exceptions, judgeId, type, playerTeams)) continue;

            List<Integer> freeSlots = findFreeSlots(table);
            if (!freeSlots.isEmpty()) {
                Collections.shuffle(freeSlots);
                table[freeSlots.get(0)] = player;
                return true;
            }
        }

        return false;
    }

    // ── ПРОВЕРКИ И ЭВРИСТИКА ──────────────────────────────────────────────────

    /**
     * Проверяет жёсткие ограничения для размещения игрока за стол.
     *
     * <p>Проверяются:</p>
     * <ul>
     *   <li>Исключения «игрок–игрок» (не должны сидеть вместе)</li>
     *   <li>Командные квоты (в командном турнире игроки одной команды не сидят вместе)</li>
     *   <li>Исключение «игрок–судья» этого стола</li>
     * </ul>
     *
     * @return {@code true}, если ограничения не нарушены
     */
    private boolean checkHardConstraints(
            User player, List<User> tableOccupants,
            List<TournamentSeedingException> exceptions,
            Long tableJudgeId,
            TournamentType type,
            Map<Long, Long> playerTeams
    ) {
        for (User occupant : tableOccupants) {
            if (hasPlayerConflict(player, occupant, exceptions)) return false;
            if (isSameTeam(player, occupant, type, playerTeams)) return false;
        }

        if (tableJudgeId != null && hasJudgeConflict(player, tableJudgeId, exceptions)) return false;

        return true;
    }

    /**
     * Возвращает {@code true}, если между двумя игроками есть активное исключение рассадки.
     */
    private boolean hasPlayerConflict(User player, User opponent, List<TournamentSeedingException> exceptions) {
        return exceptions.stream().anyMatch(ex ->
                (ex.getPlayer1().getId().equals(player.getId())   && ex.getPlayer2().getId().equals(opponent.getId())) ||
                (ex.getPlayer1().getId().equals(opponent.getId()) && ex.getPlayer2().getId().equals(player.getId()))
        );
    }

    /**
     * Возвращает {@code true}, если оба игрока состоят в одной команде (в командном турнире).
     */
    private boolean isSameTeam(User player, User occupant, TournamentType type, Map<Long, Long> playerTeams) {
        if (type != TournamentType.team || playerTeams == null) return false;
        Long team1 = playerTeams.get(player.getId());
        Long team2 = playerTeams.get(occupant.getId());
        return team1 != null && team2 != null && team1.equals(team2);
    }

    /**
     * Возвращает {@code true}, если есть исключение между игроком и судьёй стола.
     */
    private boolean hasJudgeConflict(User player, Long judgeId, List<TournamentSeedingException> exceptions) {
        return exceptions.stream().anyMatch(ex ->
                (ex.getPlayer1().getId().equals(player.getId()) && ex.getPlayer2().getId().equals(judgeId)) ||
                (ex.getPlayer1().getId().equals(judgeId)        && ex.getPlayer2().getId().equals(player.getId()))
        );
    }

    /**
     * Возвращает столы в порядке предпочтения для игрока.
     *
     * <p>В швейцарском режиме порядок случаен.
     * В обычном режиме столы сортируются по суммарному числу прошлых встреч с уже сидящими там игроками
     * (чем меньше встреч — тем выше приоритет).</p>
     */
    private List<Integer> getPreferredTableOrder(
            User player,
            Map<Integer, User[]> tables,
            Map<Long, Map<Long, Integer>> historyIntersections,
            boolean isSwiss
    ) {
        List<Integer> tableNums = new ArrayList<>(tables.keySet());

        if (isSwiss) {
            Collections.shuffle(tableNums);
            return tableNums;
        }

        Map<Long, Integer> playerHistory = historyIntersections.getOrDefault(player.getId(), new HashMap<>());

        tableNums.sort((t1, t2) -> {
            int score1 = Arrays.stream(tables.get(t1))
                    .filter(Objects::nonNull)
                    .mapToInt(u -> playerHistory.getOrDefault(u.getId(), 0))
                    .sum();
            int score2 = Arrays.stream(tables.get(t2))
                    .filter(Objects::nonNull)
                    .mapToInt(u -> playerHistory.getOrDefault(u.getId(), 0))
                    .sum();
            return Integer.compare(score1, score2);
        });

        return tableNums;
    }

    // ── ИСТОРИЯ И СОХРАНЕНИЕ ──────────────────────────────────────────────────

    /**
     * Собирает историческую информацию по всем прошлым играм турнира:
     * какие слоты занимал каждый игрок и сколько раз он встречался с каждым оппонентом.
     */
    private SeedingHistoryInfo buildHistoryInfo(List<Game> history) {
        SeedingHistoryInfo info = new SeedingHistoryInfo();

        for (Game game : history) {
            List<GameSlot> slots = gameSlotRepository.findByGameId(game.getId());
            for (GameSlot slot : slots) {
                if (slot.getUser() == null) continue;

                Long pId = slot.getUser().getId();
                info.playedSlots.computeIfAbsent(pId, k -> new HashSet<>()).add(slot.getSlotNumber());

                Map<Long, Integer> pIntersections = info.intersections.computeIfAbsent(pId, k -> new HashMap<>());
                for (GameSlot opponent : slots) {
                    if (opponent.getUser() == null) continue;
                    Long oppId = opponent.getUser().getId();
                    if (!pId.equals(oppId)) {
                        pIntersections.merge(oppId, 1, Integer::sum);
                    }
                }
            }
        }

        return info;
    }

    /**
     * Сохраняет сгенерированную рассадку в БД: создаёт игры и слоты для каждого стола.
     */
    private void saveSeedingToDb(Tournament t, int round, Map<Integer, User[]> seating, Map<Integer, Long> tableJudges, GameStage stage) {
        for (Map.Entry<Integer, User[]> entry : seating.entrySet()) {
            Integer tableNum   = entry.getKey();
            User[]  players    = entry.getValue();

            Game game = createGameForTable(t, round, tableNum, stage, tableJudges.get(tableNum));
            List<GameSlot> slots = buildSlots(game, players);
            gameSlotRepository.saveAll(slots);
        }
        tournamentRepository.setSeedingStatusTrue(t.getId());
    }

    /**
     * Создаёт и сохраняет объект игры для одного стола.
     */
    private Game createGameForTable(Tournament t, int round, int tableNum, GameStage stage, Long judgeId) {
        Game game = new Game();
        game.setTournament(t);
        game.setRoundNumber(round);
        game.setTableNumber(tableNum);
        game.setStatus(GameStatus.pending);
        game.setStage(stage);
        game.setDate(LocalDate.now());

        if (judgeId != null) {
            userRepository.findById(judgeId).ifPresent(game::setJudge);
        }

        return gameRepository.save(game);
    }

    /**
     * Создаёт список слотов для игры из массива игроков.
     * Индекс 0 соответствует слоту 1, ..., индекс 9 → слот 10.
     */
    private List<GameSlot> buildSlots(Game game, User[] players) {
        List<GameSlot> slots = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (players[i] == null) continue;
            GameSlot slot = new GameSlot();
            slot.setGame(game);
            slot.setUser(players[i]);
            slot.setSlotNumber(i + 1);
            slot.setRole(PlayerRoleInGame.civilian);
            slots.add(slot);
        }
        return slots;
    }

    // ── ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ────────────────────────────────────────────────


    /**
     * Определяет количество туров из настроек турнира.
     * Если roundsCount не задан — возвращает 10 (разумный дефолт).
     *
     * @param t турнир
     * @return количество плановых туров
     */
    private int resolveRoundsCount(Tournament t) {
        if (t.getSettings() == null || t.getSettings().getRoundsCount() == null) {
            return 10;
        }
        return t.getSettings().getRoundsCount();
    }

    /**
     * Определяет начальный тур швейцарской системы.
     *
     * <p>Если швейцарская система не включена или swissRoundsStart не задан —
     * возвращает {@code roundsCount + 1}, что гарантирует, что условие
     * {@code nextRound >= swissStart} никогда не выполнится для обычного турнира.</p>
     *
     * <p><strong>Это исправляет баг</strong>, при котором возвращалось значение 999,
     * что приводило к попытке сгенерировать 999 балансированных туров.</p>
     *
     * @param t           турнир
     * @param roundsCount количество обычных туров (из {@link #resolveRoundsCount})
     * @return номер первого швейцарского тура, либо roundsCount+1
     */
    private int resolveSwissStart(Tournament t, int roundsCount) {
        if (t.getSettings() == null) {
            return roundsCount + 1;
        }
        Integer swissStart = t.getSettings().getSwissRoundsStart();
        return swissStart != null ? swissStart : roundsCount + 1;
    }

    /** Загружает одобренных участников турнира. */
    private List<TournamentParticipant> loadApprovedParticipants(Long tournamentId) {
        return participantRepository.findByTournamentIdAndStatus(tournamentId, ParticipantStatus.approved);
    }

    /** Проверяет, что количество участников кратно 10. */
    private void validateParticipantCount(int count) {
        if (count == 0 || count % 10 != 0) {
            throw new RuntimeException(
                    "Количество подтверждённых участников (" + count + ") должно быть кратно 10"
            );
        }
    }

    /** Извлекает карту «номер стола → ID судьи» из настроек турнира. */
    private Map<Integer, Long> buildTableJudgesMap(Tournament t) {
        Map<Integer, Long> tableJudges = new HashMap<>();
        if (t.getSettings().getStaticTableJudges() != null) {
            t.getSettings().getStaticTableJudges()
                    .forEach((k, v) -> tableJudges.put(Integer.parseInt(k), v));
        }
        return tableJudges;
    }

    /** Строит карту «ID игрока → ID команды» для командных турниров. */
    private Map<Long, Long> buildPlayerTeamsMap(Tournament t, List<TournamentParticipant> participants) {
        if (t.getType() != TournamentType.team) return new HashMap<>();

        Map<Long, Long> playerTeams = new HashMap<>();
        participants.forEach(p -> {
            if (p.getTeam() != null) {
                playerTeams.put(p.getUser().getId(), p.getTeam().getId());
            }
        });
        return playerTeams;
    }

    /** Определяет номер следующего тура (максимальный текущий + 1). */
    private int getNextRoundNumber(List<Game> history) {
        return history.stream().mapToInt(Game::getRoundNumber).max().orElse(0) + 1;
    }

    /**
     * Определяет начальный тур швейцарской системы.
     * Возвращает 999, если швейцарки нет или значение не задано.
     */
    private int resolveSwissStart(Tournament t) {
        Integer swissStart = t.getSettings().getSwissRoundsStart();
        return swissStart != null ? swissStart : 999;
    }

    /**
     * Извлекает зафиксированный список топ-10 игроков для финала.
     *
     * @throws RuntimeException если список не зафиксирован или содержит менее 10 ID
     */
    private List<Long> resolveTop10Ids(Tournament t) {
        List<Long> ids = (t.getSettings() != null) ? t.getSettings().getTop10PlayerIds() : null;
        if (ids == null || ids.size() < 10) {
            throw new RuntimeException("Сначала зафиксируйте отборочных через кнопку «Зафиксировать результаты»");
        }
        return ids;
    }

    /** Создаёт карту пустых столов (10 слотов, все null). */
    private Map<Integer, User[]> initEmptyTables(int tablesCount, int startTableNum) {
        Map<Integer, User[]> tables = new HashMap<>();
        for (int i = 0; i < tablesCount; i++) {
            tables.put(startTableNum + i, new User[10]);
        }
        return tables;
    }

    /**
     * Возвращает список игроков, уже сидящих за столом (без null-значений).
     */
    private List<User> getSeatedPlayers(User[] table) {
        return Arrays.stream(table)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Находит индексы (0–9) свободных слотов, на которых игрок ещё НЕ сидел.
     * Используется в первом проходе — предпочтительный выбор.
     *
     * @param table     массив 10 позиций (null = свободно)
     * @param usedSlots набор номеров слотов (1–10), уже занятых игроком в истории
     * @return индексы свободных уникальных слотов
     */
    private List<Integer> findFreeUniqueSlots(User[] table, Set<Integer> usedSlots) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (table[i] == null && !usedSlots.contains(i + 1)) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * Находит индексы (0–9) всех свободных слотов без учёта истории.
     * Используется как фоллбэк при 11+ турах, когда уникальных слотов уже нет.
     *
     * @param table массив 10 позиций (null = свободно)
     * @return индексы всех свободных слотов
     */
    private List<Integer> findFreeSlots(User[] table) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (table[i] == null) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * Формирует пул игроков одного тира на основе позиций в лидерборде.
     */
    private List<User> buildTierPool(List<LeaderboardEntryDto> leaderboard, Map<Long, User> userMap, int offset, int tierSize) {
        List<User> pool = new ArrayList<>();
        for (int i = 0; i < tierSize && (offset + i) < leaderboard.size(); i++) {
            User user = userMap.get(leaderboard.get(offset + i).getUserId());
            if (user != null) pool.add(user);
        }
        return pool;
    }

    // ── ВСПОМОГАТЕЛЬНЫЙ КЛАСС ─────────────────────────────────────────────────

    /**
     * Кэш исторических данных рассадки для одного вызова алгоритма.
     *
     * <p>Содержит:</p>
     * <ul>
     *   <li>{@code playedSlots}    — карта «ID игрока → набор слотов (1–10)», на которых он уже сидел</li>
     *   <li>{@code intersections}  — карта «ID игрока → (ID оппонента → число встреч)»</li>
     * </ul>
     */
    private static class SeedingHistoryInfo {
        final Map<Long, Set<Integer>>          playedSlots   = new HashMap<>();
        final Map<Long, Map<Long, Integer>>    intersections = new HashMap<>();
    }
}
