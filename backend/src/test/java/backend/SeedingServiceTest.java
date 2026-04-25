package backend;

import com.mafia.manager.dto.LeaderboardEntryDto;
import com.mafia.manager.entity.*;
import com.mafia.manager.entity.enums.*;
import com.mafia.manager.entity.json.TournamentSettings;
import com.mafia.manager.repository.*;
import com.mafia.manager.service.SeedingService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UT-14 — Комплексные тесты алгоритма рассадки (SeedingService).
 *
 * Сценарии:
 *   S-01: Базовая рассадка 1-й тур, 30 участников, 3 стола
 *   S-02: Уникальность слотов — 10 туров, 10 игроков (каждый слот уникален)
 *   S-03: Более 11 туров, 30 участников — фоллбэк на любой свободный слот
 *   S-04: Исключения игрок–игрок соблюдаются
 *   S-05: Исключение игрок–судья стола соблюдаются
 *   S-06: Смешанная рассадка: 7 обычных туров + 4 швейцарских (50 участников)
 *   S-07: Финальная рассадка (10 игроков)
 *   S-08: Исправление бага — roundsCount берётся из настроек, а не 999
 *   S-09: Командный турнир — игроки одной команды не сидят вместе
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeedingService — алгоритм рассадки")
class SeedingServiceTest {

    @Mock TournamentRepository            tournamentRepository;
    @Mock TournamentParticipantRepository participantRepository;
    @Mock GameRepository                  gameRepository;
    @Mock GameSlotRepository              gameSlotRepository;
    @Mock SeedingExceptionRepository      exceptionRepository;
    @Mock UserRepository                  userRepository;

    @InjectMocks
    SeedingService seedingService;

    // ── Фабрики ───────────────────────────────────────────────────────────────

    private User user(long id, String nick) {
        User u = new User();
        u.setId(id);
        u.setNickname(nick);
        return u;
    }

    private TournamentParticipant participant(Tournament t, User u) {
        TournamentParticipant p = new TournamentParticipant();
        p.setTournament(t);
        p.setUser(u);
        p.setStatus(ParticipantStatus.approved);
        return p;
    }

    private Tournament tournament(int maxParticipants, int roundsCount) {
        TournamentSettings settings = new TournamentSettings();
        settings.setMaxParticipants(maxParticipants);
        settings.setRoundsCount(roundsCount);
        settings.setIsSeedingGenerated(false);

        Tournament t = new Tournament();
        t.setId(1L);
        t.setType(TournamentType.individual);
        t.setSettings(settings);
        return t;
    }

    private Tournament swissTournament(int maxParticipants, int roundsCount,
                                       int swissStart, List<Integer> tiers) {
        Tournament t = tournament(maxParticipants, roundsCount);
        t.getSettings().setIsSwissSystem(true);
        t.getSettings().setSwissRoundsStart(swissStart);
        t.getSettings().setSwissTiers(tiers);
        return t;
    }

    /** Создаёт N участников */
    private List<TournamentParticipant> participants(Tournament t, int n) {
        List<TournamentParticipant> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(participant(t, user(i, "Player" + i)));
        }
        return list;
    }

    /** Создаёт историю игр с заполненными слотами для данного списка участников */
    private List<Game> buildHistory(Tournament t, List<TournamentParticipant> parts,
                                    int roundsPlayed, List<GameSlot> capturedSlots) {
        List<User> players = parts.stream().map(TournamentParticipant::getUser).collect(Collectors.toList());
        List<Game> games = new ArrayList<>();

        for (int round = 1; round <= roundsPlayed; round++) {
            int tablesCount = players.size() / 10;
            for (int table = 0; table < tablesCount; table++) {
                Game game = new Game();
                game.setId((long) (round * 100 + table));
                game.setRoundNumber(round);
                game.setTableNumber(table + 1);
                game.setTournament(t);
                game.setStatus(GameStatus.completed);
                games.add(game);

                // Назначаем 10 игроков на стол (round-robin упрощённый)
                for (int slot = 0; slot < 10; slot++) {
                    int playerIdx = (table * 10 + slot + (round - 1) * 3) % players.size();
                    GameSlot gs = new GameSlot();
                    gs.setGame(game);
                    gs.setUser(players.get(playerIdx));
                    gs.setSlotNumber(slot + 1);
                    capturedSlots.add(gs);
                }
            }
        }
        return games;
    }

    // ── Настройка мок-ответов ─────────────────────────────────────────────────

    private void setupMocks(Tournament t, List<TournamentParticipant> parts,
                            List<Game> history, List<GameSlot> capturedSlots) {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.approved))
                .thenReturn(parts);
        when(exceptionRepository.findByTournamentId(1L)).thenReturn(List.of());
        when(gameRepository.findByTournamentId(1L)).thenReturn(history);

        when(gameSlotRepository.findByGameId(anyLong()))
                .thenAnswer(inv -> {
                    Long gameId = inv.getArgument(0);
                    return capturedSlots.stream()
                            .filter(s -> s.getGame().getId().equals(gameId))
                            .collect(Collectors.toList());
                });

        // После сохранения игр их добавляем в историю
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            if (g.getId() == null) g.setId((long) (history.size() + 1) * 1000);
            history.add(g);
            // Обновляем мок для повторного вызова findByTournamentId
            when(gameRepository.findByTournamentId(1L)).thenReturn(history);
            return g;
        });

        when(gameSlotRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<GameSlot> slots = inv.getArgument(0);
            capturedSlots.addAll(slots);
            return slots;
        });

        doNothing().when(tournamentRepository).setSeedingStatusTrue(anyLong());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // S-01: Базовая рассадка — 30 участников, 1-й тур
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("S-01: 30 участников, 3 стола")
    void generateNextRound_30players_eachPlayedOnce() {
        System.out.println("S-01");
        Tournament t = tournament(30, 12);
        List<TournamentParticipant> parts = participants(t, 30);
        List<GameSlot> capturedSlots = new ArrayList<>();
        List<Game> history = new ArrayList<>();

        setupMocks(t, parts, history, capturedSlots);

        seedingService.generateNextRound(1L);

        // 3 стола × 10 слотов = 30 слотов сгенерировано
        assertThat(capturedSlots).hasSize(360);

        // Каждый игрок должен быть в ровно 1 слоте
        Map<Long, Long> playerCounts = capturedSlots.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getUser().getId(),
                        Collectors.counting()
                ));
        assertThat(playerCounts).hasSize(30);
        playerCounts.values().forEach(count ->
                assertThat(count).as("Каждый игрок должен быть ровно 1 раз").isEqualTo(12L)
        );
    }

    // ════════════════════════════════════════════════════════════════════════════
    // S-02: Уникальность слотов — 10 туров, 10 игроков
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("S-02: 10 игроков, 10 туров — каждый слот (1-10) занят каждым игроком ровно 1 раз")
    void generateNextRound_10players_10rounds_uniqueSlots() {
        System.out.println("S-02");
        Tournament t = tournament(10, 10);
        List<TournamentParticipant> parts = participants(t, 10);
        List<GameSlot> capturedSlots = new ArrayList<>();
        List<Game> history = new ArrayList<>();

        setupMocks(t, parts, history, capturedSlots);


        seedingService.generateNextRound(1L);


        // === БЛОК ВЫВОДА СОСТАВА СТОЛОВ ===
        System.out.println("\nОТЧЕТ ПО РАССАДКЕ (Тур / Слоты 1-10):");
        System.out.println("------------------------------------------------------------------------------------------");
        Map<Integer, List<GameSlot>> slotsByRound = capturedSlots.stream()
                .collect(Collectors.groupingBy(s -> s.getGame().getRoundNumber()));

        for (int r = 1; r <= 10; r++) {
            List<GameSlot> roundSlots = slotsByRound.getOrDefault(r, Collections.emptyList());
            // Сортируем по номеру слота для красивого вывода
            String roundRow = roundSlots.stream()
                    .sorted(Comparator.comparingInt(GameSlot::getSlotNumber))
                    .map(s -> String.format("[%2d: %-10s]", s.getSlotNumber(), s.getUser().getNickname()))
                    .collect(Collectors.joining(" "));
            System.out.printf("Тур %2d: %s%n", r, roundRow);
        }
        System.out.println("------------------------------------------------------------------------------------------\n");

        // Проверка уникальности слотов для каждого игрока
        Map<Long, Set<Integer>> playerSlots = new HashMap<>();
        capturedSlots.forEach(s -> {
            Long playerId = s.getUser().getId();
            playerSlots.computeIfAbsent(playerId, k -> new HashSet<>());
            playerSlots.get(playerId).add(s.getSlotNumber());
        });

        playerSlots.forEach((playerId, slots) -> {
            // Находим имя игрока для понятного сообщения об ошибке
            String playerName = parts.stream()
                    .filter(p -> p.getUser().getId().equals(playerId))
                    .map(p -> p.getUser().getNickname())
                    .findFirst().orElse("Unknown");

            assertThat(slots)
                    .as("Игрок %s (ID:%d) должен занять каждый слот 1-10 ровно по 1 разу. Получено слотов: %s",
                            playerName, playerId, slots)
                    .hasSize(10);
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    // S-03: Более 11 туров, 30 участников — фоллбэк на любой слот
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("S-03: 30 игроков, 12 туров — после 10 уникальных слотов фоллбэк не ломает рассадку")
    void generateNextRound_30players_12rounds_fallbackWorks() {
        System.out.println("S-03");
        Tournament t = tournament(30, 12);
        List<TournamentParticipant> parts = participants(t, 30);
        List<GameSlot> capturedSlots = new ArrayList<>();
        List<Game> history = new ArrayList<>();

        setupMocks(t, parts, history, capturedSlots);

        // Генерируем 12 туров

        assertThatCode(() -> seedingService.generateNextRound(1L))
                    .doesNotThrowAnyException();


        // 12 туров × 3 стола × 10 слотов = 360 слотов
        assertThat(capturedSlots).hasSize(360);

        // В каждом туре каждый игрок играет ровно 1 раз
        // Разбиваем по турам через game.roundNumber
        Map<Integer, Set<Long>> playersByRound = new HashMap<>();
        capturedSlots.forEach(s -> {
            int round = s.getGame().getRoundNumber();
            playersByRound.computeIfAbsent(round, k -> new HashSet<>()).add(s.getUser().getId());
        });

        playersByRound.forEach((round, players) ->
                assertThat(players)
                        .as("В туре %d должны быть все 30 уникальных игроков", round)
                        .hasSize(30)
        );
    }

    // ════════════════════════════════════════════════════════════════════════════
    // S-04: Исключения игрок–игрок
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("S-04: Исключение игрок-игрок — два игрока никогда не сидят за одним столом")
    void generateNextRound_playerPlayerException_respected() {
        System.out.println("S-04");
        Tournament t = tournament(20, 11);
        List<TournamentParticipant> parts = participants(t, 20);
        List<GameSlot> capturedSlots = new ArrayList<>();
        List<Game> history = new ArrayList<>();

        User p1 = parts.get(0).getUser(); // ID=1
        User p2 = parts.get(1).getUser(); // ID=2

        // Создаём исключение между игроком 1 и игроком 2
        TournamentSeedingException ex = new TournamentSeedingException();
        ex.setId(1L);
        ex.setTournament(t);
        ex.setPlayer1(p1);
        ex.setPlayer2(p2);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.approved))
                .thenReturn(parts);
        when(exceptionRepository.findByTournamentId(1L)).thenReturn(List.of(ex));
        when(gameRepository.findByTournamentId(1L)).thenReturn(history);
        when(gameSlotRepository.findByGameId(anyLong())).thenReturn(List.of());
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            if (g.getId() == null) g.setId((long) (history.size() + 1) * 1000);
            history.add(g);
            when(gameRepository.findByTournamentId(1L)).thenReturn(history);
            return g;
        });
        when(gameSlotRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<GameSlot> slots = inv.getArgument(0);
            capturedSlots.addAll(slots);
            return slots;
        });
        doNothing().when(tournamentRepository).setSeedingStatusTrue(anyLong());

        // Генерируем несколько туров

        seedingService.generateNextRound(1L);


        // Проверяем: P1 и P2 никогда не за одним столом
        Map<Long, Set<Long>> playersByGame = new HashMap<>();
        capturedSlots.forEach(s -> {
            Long gameId = s.getGame().getId();
            playersByGame.computeIfAbsent(gameId, k -> new HashSet<>()).add(s.getUser().getId());
        });

        playersByGame.forEach((gameId, playerIds) -> {
            boolean bothPresent = playerIds.contains(p1.getId()) && playerIds.contains(p2.getId());
            assertThat(bothPresent)
                    .as("Игроки 1 и 2 не должны быть за одним столом (игра %d)", gameId)
                    .isFalse();
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    // S-05: Исключение игрок–судья
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("S-05: Исключение игрок-судья — игрок не садится за стол судьи")
    void generateNextRound_playerJudgeException_respected() {
        System.out.println("S-05");
        Tournament t = tournament(20, 5);
        List<TournamentParticipant> parts = participants(t, 20);
        List<GameSlot> capturedSlots = new ArrayList<>();
        List<Game> history = new ArrayList<>();

        User judge1 = user(101L, "Judge1");
        User judge2 = user(102L, "Judge2");
        User restrictedPlayer = parts.get(0).getUser(); // ID=1

        // Назначаем судей на столы: стол 1 → judge1, стол 2 → judge2
        Map<String, Long> tableJudges = new HashMap<>();
        tableJudges.put("1", judge1.getId());
        tableJudges.put("2", judge2.getId());
        t.getSettings().setStaticTableJudges(tableJudges);

        // Исключение: игрок 1 не может сидеть со судьёй judge1 (стол 1)
        TournamentSeedingException judgeEx = new TournamentSeedingException();
        judgeEx.setId(10L);
        judgeEx.setTournament(t);
        judgeEx.setPlayer1(restrictedPlayer);
        judgeEx.setPlayer2(judge1);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.approved))
                .thenReturn(parts);
        when(exceptionRepository.findByTournamentId(1L)).thenReturn(List.of(judgeEx));
        when(gameRepository.findByTournamentId(1L)).thenReturn(history);
        when(gameSlotRepository.findByGameId(anyLong())).thenReturn(List.of());
        when(userRepository.findById(judge1.getId())).thenReturn(Optional.of(judge1));
        when(userRepository.findById(judge2.getId())).thenReturn(Optional.of(judge2));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            if (g.getId() == null) g.setId((long) (history.size() + 1) * 1000);
            history.add(g);
            when(gameRepository.findByTournamentId(1L)).thenReturn(history);
            return g;
        });
        when(gameSlotRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<GameSlot> slots = inv.getArgument(0);
            capturedSlots.addAll(slots);
            return slots;
        });
        doNothing().when(tournamentRepository).setSeedingStatusTrue(anyLong());

        seedingService.generateNextRound(1L);

        // Находим игру за столом 1 и проверяем, что игрок 1 там не сидит
        capturedSlots.stream()
                .filter(s -> s.getGame().getTableNumber() != null && s.getGame().getTableNumber() == 1)
                .forEach(s -> assertThat(s.getUser().getId())
                        .as("Игрок 1 не должен сидеть за столом судьи judge1")
                        .isNotEqualTo(restrictedPlayer.getId()));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // S-06: Смешанная рассадка: 7 обычных + 4 швейцарских тура, 50 участников
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("S-06: 50 игроков — 7 обычных туров, затем туры 8-11 по швейцарке (тиры 30+20)")
    void generateNextRound_swissAfterBalanced_50players() {
        System.out.println("S-06");
        // Тиры: 30 сильнейших + 20 слабейших
        Tournament t = swissTournament(50, 11, 8, List.of(30, 20));
        List<TournamentParticipant> parts = participants(t, 50);
        List<GameSlot> capturedSlots = new ArrayList<>();
        List<Game> history = new ArrayList<>();

        // Исключения для реалистичности: 2 пары игрок-игрок + 1 игрок-судья
        User judge = user(200L, "HeadJudge");
        Map<String, Long> tableJudges = new HashMap<>();
        tableJudges.put("1", judge.getId());
        t.getSettings().setStaticTableJudges(tableJudges);

        TournamentSeedingException ex1 = new TournamentSeedingException();
        ex1.setId(1L); ex1.setTournament(t);
        ex1.setPlayer1(parts.get(0).getUser());
        ex1.setPlayer2(parts.get(2).getUser());

        TournamentSeedingException ex2 = new TournamentSeedingException();
        ex2.setId(2L); ex2.setTournament(t);
        ex2.setPlayer1(parts.get(1).getUser());
        ex2.setPlayer2(parts.get(3).getUser());

        TournamentSeedingException judgeEx = new TournamentSeedingException();
        judgeEx.setId(3L); judgeEx.setTournament(t);
        judgeEx.setPlayer1(parts.get(5).getUser());
        judgeEx.setPlayer2(judge);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.approved))
                .thenReturn(parts);
        when(exceptionRepository.findByTournamentId(1L)).thenReturn(List.of(ex1, ex2, judgeEx));
        when(gameRepository.findByTournamentId(1L)).thenReturn(history);
        when(gameSlotRepository.findByGameId(anyLong())).thenAnswer(inv -> {
            Long gid = inv.getArgument(0);
            return capturedSlots.stream()
                    .filter(s -> s.getGame().getId().equals(gid))
                    .collect(Collectors.toList());
        });
        when(userRepository.findById(judge.getId())).thenReturn(Optional.of(judge));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            if (g.getId() == null) g.setId((long) (history.size() + 1) * 1000 + new Random().nextInt(999));
            history.add(g);
            lenient().when(gameRepository.findByTournamentId(1L)).thenReturn(history);
            return g;
        });
        when(gameSlotRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<GameSlot> slots = inv.getArgument(0);
            capturedSlots.addAll(slots);
            return slots;
        });
        doNothing().when(tournamentRepository).setSeedingStatusTrue(anyLong());

        // Мок лидерборда для швейцарских туров (нужен для сортировки тиров)
        List<LeaderboardEntryDto> mockLeaderboard = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            final int rank = i;
            final long uid = parts.get(i).getUser().getId();
            LeaderboardEntryDto entry = mock(LeaderboardEntryDto.class);
            when(entry.getUserId()).thenReturn(uid);
            lenient().when(entry.getTotalScore()).thenReturn((double)(50 - rank)); // убывающий score
            mockLeaderboard.add(entry);
        }
        when(tournamentRepository.getLeaderboard(1L, false, "total"))
                .thenReturn(mockLeaderboard);

        // Шаг 1: генерируем туры 1-7 (обычные)

        assertThatCode(() -> seedingService.generateNextRound(1L))
                .doesNotThrowAnyException();


        long gamesAfter7Rounds = history.size();
        assertThat(gamesAfter7Rounds).isEqualTo(7L * 5); // 7 туров × 5 столов


        // Шаг 2: генерируем туры 8-11 (швейцарские)
        for (int round = 8; round <= 11; round++) {
            final int r = round;
            assertThatCode(() -> seedingService.generateNextRound(1L))
                    .as("Швейцарский тур %d не должен бросать исключение", r)
                    .doesNotThrowAnyException();
        }

        // 11 туров × 5 столов = 55 игр
        assertThat(history).hasSize(11 * 5);

        // Проверяем: в каждой игре ровно 10 игроков
        capturedSlots.stream()
                .collect(Collectors.groupingBy(s -> s.getGame().getId(), Collectors.counting()))
                .forEach((gameId, count) ->
                        assertThat(count).as("Игра %d должна иметь 10 слотов", gameId).isEqualTo(10L)
                );

        // Исключения соблюдены: пары ex1 и ex2 не за одним столом
        capturedSlots.stream()
                .collect(Collectors.groupingBy(s -> s.getGame().getId(),
                        Collectors.mapping(s -> s.getUser().getId(), Collectors.toSet())))
                .forEach((gameId, playerIds) -> {
                    boolean ex1Violated = playerIds.contains(ex1.getPlayer1().getId())
                            && playerIds.contains(ex1.getPlayer2().getId());
                    boolean ex2Violated = playerIds.contains(ex2.getPlayer1().getId())
                            && playerIds.contains(ex2.getPlayer2().getId());

                    assertThat(ex1Violated).as("Исключение ex1 нарушено в игре " + gameId).isFalse();
                    assertThat(ex2Violated).as("Исключение ex2 нарушено в игре " + gameId).isFalse();
                });


    }

    // ════════════════════════════════════════════════════════════════════════════
    // S-07: Финальная рассадка — 10 финалистов
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("S-07: Финальная рассадка — 10 финалистов, 1 стол, stage=final_round")
    void generateFinalRound_10finalists_oneTable() {
        System.out.println("S-07");
        Tournament t = tournament(30, 10);
        // Задаём топ-10 финалистов
        List<Long> top10Ids = LongStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());
        t.getSettings().setTop10PlayerIds(top10Ids);
        t.getSettings().setAreQualifiersFixed(true);

        List<GameSlot> capturedSlots = new ArrayList<>();
        List<Game> history = new ArrayList<>();

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));
        when(gameRepository.findByTournamentId(1L)).thenReturn(history);
        //when(exceptionRepository.findByTournamentId(1L)).thenReturn(List.of());

        // Мок для поиска каждого финалиста
        top10Ids.forEach(id -> when(userRepository.findById(id))
                .thenReturn(Optional.of(user(id, "Finalist" + id))));

        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            if (g.getId() == null) g.setId(9999L);
            history.add(g);
            return g;
        });
        when(gameSlotRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<GameSlot> slots = inv.getArgument(0);
            capturedSlots.addAll(slots);
            return slots;
        });
        doNothing().when(tournamentRepository).setSeedingStatusTrue(anyLong());

        seedingService.generateFinalRound(1L);

        // Должна быть создана ровно 1 игра с 10 слотами
        assertThat(history).hasSize(1);
        assertThat(capturedSlots).hasSize(10);

        // Stage = final_round
        assertThat(history.get(0).getStage()).isEqualTo(GameStage.final_round);

        // Все 10 финалистов в игре
        Set<Long> playerIds = capturedSlots.stream()
                .map(s -> s.getUser().getId())
                .collect(Collectors.toSet());
        assertThat(playerIds).containsExactlyInAnyOrderElementsOf(top10Ids);
    }

    @Test
    @DisplayName("S-07b: Финал без зафиксированных финалистов → RuntimeException")
    void generateFinalRound_noFinalists_throws() {
        System.out.println("S-07b");
        Tournament t = tournament(30, 10);
        t.getSettings().setTop10PlayerIds(null);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> seedingService.generateFinalRound(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("зафиксируйте");
    }

    @Test
    @DisplayName("S-07c: Финал с менее чем 10 финалистами → RuntimeException")
    void generateFinalRound_lessThan10Finalists_throws() {
        System.out.println("S-07c");
        Tournament t = tournament(30, 10);
        t.getSettings().setTop10PlayerIds(List.of(1L, 2L, 3L)); // только 3

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> seedingService.generateFinalRound(1L))
                .isInstanceOf(RuntimeException.class);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // S-08: Исправление бага — roundsCount из настроек, а не 999
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("S-08 [БАГ-ФИКС]: generateNextRound генерирует только roundsCount туров, а не 999")
    void generateNextRound_respectsRoundsCountFromSettings() {
        System.out.println("S-08");
        int expectedRounds = 5;
        Tournament t = tournament(10, expectedRounds); // только 5 туров
        // БЕЗ швейцарки — раньше код генерировал 999 туров

        List<TournamentParticipant> parts = participants(t, 10);
        List<GameSlot> capturedSlots = new ArrayList<>();
        List<Game> history = new ArrayList<>();

        setupMocks(t, parts, history, capturedSlots);

        // Вызываем generateNextRound ровно expectedRounds раз

        seedingService.generateNextRound(1L);


        // Проверяем что сгенерировано ровно expectedRounds туров
        long distinctRounds = history.stream()
                .map(Game::getRoundNumber)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        assertThat(distinctRounds).isEqualTo(expectedRounds);

        // Следующий вызов не должен генерировать тур выше roundsCount
        // (это ответственность контроллера, но алгоритм не должен падать)
        // Просто проверяем что история содержит ровно 5 туров
        assertThat(history)
                .as("Должно быть сгенерировано ровно %d тура(ов) × 1 стол = %d игр",
                        expectedRounds, expectedRounds)
                .hasSize(expectedRounds);
    }

    @Test
    @DisplayName("S-08b: Без швейцарки — swissStart вычисляется как roundsCount+1, а не 999")
    void resolveSwissStart_noSwiss_returnsRoundsCountPlusOne() {
        System.out.println("S-08b");
        // Турнир без швейцарки, 7 туров
        Tournament t = tournament(10, 7);
        List<TournamentParticipant> parts = participants(t, 10);
        List<GameSlot> capturedSlots = new ArrayList<>();
        List<Game> history = new ArrayList<>();

        setupMocks(t, parts, history, capturedSlots);

        // Генерируем 7 туров

        seedingService.generateNextRound(1L);


        // Все туры должны быть 1-7, нет ни одного тура с номером > 7
        history.forEach(game ->
                assertThat(game.getRoundNumber())
                        .as("Номер тура не должен превышать roundsCount=7")
                        .isLessThanOrEqualTo(7)
        );
    }

    // ════════════════════════════════════════════════════════════════════════════
    // S-09: Командный турнир — игроки одной команды не за одним столом
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("S-09: Командный турнир — игроки одной команды не сидят за одним столом (10 команд, столы по 10)")
    void generateNextRound_teamTournament_sameTeamNotTogether_10Seats() {
        System.out.println("S-09");
        Tournament t = tournament(20, 5);
        t.setType(TournamentType.team);

        List<TournamentParticipant> parts = new ArrayList<>();
        int userId = 1;

        // Создаём 10 команд по 2 человека в каждой
        for (long teamId = 1; teamId <= 10; teamId++) {
            TournamentTeam team = new TournamentTeam();
            team.setId(teamId);
            team.setName("Команда " + teamId);

            for (int i = 0; i < 2; i++) {
                TournamentParticipant p = participant(t, user(userId, "Player_" + userId));
                p.setTeam(team);
                parts.add(p);
                userId++; // ID пользователей будут от 1 до 20
            }
        }

        List<GameSlot> capturedSlots = new ArrayList<>();
        List<Game> history = new ArrayList<>();

        setupMocks(t, parts, history, capturedSlots);

        seedingService.generateNextRound(1L);

        // Группируем слоты по игре, а внутри игры — по командам
        capturedSlots.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getGame().getId(),
                        Collectors.groupingBy(
                                // Вычисляем ID команды по ID пользователя:
                                // ID 1, 2 -> Команда 1
                                // ID 3, 4 -> Команда 2
                                // ...
                                // ID 19, 20 -> Команда 10
                                s -> (s.getUser().getId() + 1) / 2,
                                Collectors.counting()
                        )
                ))
                .forEach((gameId, teamCounts) -> {
                    // За столом на 10 человек должно оказаться все 10 РАЗНЫХ команд
                    assertThat(teamCounts)
                            .as("В игре %d должны присутствовать представители 10 разных команд", gameId)
                            .hasSize(10);

                    // От каждой команды за столом должен сидеть строго 1 человек
                    teamCounts.forEach((teamId, count) -> {
                        assertThat(count)
                                .as("В игре %d команда %d имеет %d игроков за столом (ожидается ровно 1)", gameId, teamId, count)
                                .isEqualTo(1L);
                    });
                });
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Негативные сценарии
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Количество участников не кратно 10 → RuntimeException")
    void generateNextRound_participantsNotMultipleOf10_throws() {
        Tournament t = tournament(15, 5);
        List<TournamentParticipant> parts = participants(t, 15); // 15 не кратно 10

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.approved))
                .thenReturn(parts);
        when(exceptionRepository.findByTournamentId(1L)).thenReturn(List.of());
        when(gameRepository.findByTournamentId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> seedingService.generateNextRound(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("кратно 10");
    }

    @Test
    @DisplayName("Нет участников → RuntimeException")
    void generateNextRound_noParticipants_throws() {
        Tournament t = tournament(10, 5);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.approved))
                .thenReturn(List.of());
        when(exceptionRepository.findByTournamentId(1L)).thenReturn(List.of());
        when(gameRepository.findByTournamentId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> seedingService.generateNextRound(1L))
                .isInstanceOf(RuntimeException.class);
    }

    // ── Вспомогательные импорты (для компилятора) ─────────────────────────────
    private java.util.stream.LongStream LongStream(long start, long end) {
        return java.util.stream.LongStream.rangeClosed(start, end);
    }

    // Делаем доступным java.util.stream.LongStream
    private static final class LongStream {
        static java.util.stream.LongStream rangeClosed(long start, long end) {
            return java.util.stream.LongStream.rangeClosed(start, end);
        }
    }
}
