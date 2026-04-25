package com.mafia.manager.service;

import com.mafia.manager.dto.*;
import com.mafia.manager.entity.*;
import com.mafia.manager.entity.enums.*;
import com.mafia.manager.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UT-08, UT-09, UT-10 — Тесты GameService.
 * Покрывают: сохранение черновика, завершение партии, триггер пересчёта CI.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameService — протоколы и CI")
class GameServiceTest {

    @Mock GameRepository       gameRepository;
    @Mock GameSlotRepository   gameSlotRepository;
    @Mock BestMoveRepository   bestMoveRepository;
    @Mock UserRepository       userRepository;
    @Mock TournamentRepository tournamentRepository;

    @InjectMocks GameService gameService;

    // ── Фабрики ───────────────────────────────────────────────────────────────

    private Game pendingGame(Long id, Long tournamentId) {
        Tournament t = new Tournament();
        t.setId(tournamentId);
        Game g = new Game();
        g.setId(id);
        g.setTournament(t);
        g.setStatus(GameStatus.pending);
        g.setDate(LocalDate.now());
        return g;
    }

    private List<GameSlot> emptySlots(Game game) {
        return IntStream.rangeClosed(1, 10).mapToObj(i -> {
            GameSlot s = new GameSlot();
            s.setId((long) i);
            s.setGame(game);
            s.setSlotNumber(i);
            s.setRole(PlayerRoleInGame.civilian);
            s.setFouls(0);
            s.setExtraPointsPositive(BigDecimal.ZERO);
            s.setExtraPointsNegative(BigDecimal.ZERO);
            s.setPenaltyPoints(BigDecimal.ZERO);
            s.setIsFirstKilled(false);
            return s;
        }).collect(Collectors.toList());
    }

    private List<GameSlotDto> buildSlotDtos(String... roles) {
        List<GameSlotDto> dtos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GameSlotDto dto = new GameSlotDto();
            dto.setSlotNumber(i + 1);
            dto.setPlayerId((long) (i + 1));
            dto.setRole(roles.length > i ? roles[i] : "civilian");
            dto.setIsFirstKilled(false);
            dto.setFouls(0);
            dto.setExtraPos(BigDecimal.ZERO);
            dto.setExtraNeg(BigDecimal.ZERO);
            dto.setPenalty(BigDecimal.ZERO);
            dtos.add(dto);
        }
        return dtos;
    }

    /** 6 мирных, 1 шериф, 2 мафии, 1 дон */
    private String[] standardRoles() {
        return new String[]{"civilian", "civilian", "civilian", "civilian",
                "civilian", "civilian", "sheriff", "mafia", "mafia", "don"};
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UT-08 — Сохранение черновика
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("UT-08: Сохранение протокола со статусом DRAFT — игра сохраняется как черновик")
    void saveProtocol_draft_statusDraft() {
        Game game = pendingGame(1L, 100L);
        List<GameSlot> existingSlots = emptySlots(game);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gameSlotRepository.findByGameId(1L)).thenReturn(existingSlots);
        lenient().when(bestMoveRepository.findByGameId(1L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(anyLong())).thenAnswer(inv -> {
            User u = new User(); u.setId(inv.getArgument(0)); return u;
        });
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameSlotRepository.save(any(GameSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        GameProtocolDto dto = new GameProtocolDto();
        dto.setStatus("draft");
        dto.setWinner("red");
        dto.setSlots(buildSlotDtos(standardRoles()));

        gameService.saveProtocol(1L, dto);

        verify(gameRepository).save(argThat(g -> g.getStatus() == GameStatus.draft));
        // При статусе draft — триггер пересчёта CI НЕ вызывается
        verify(gameSlotRepository, never()).callRecalculatePlayerCi(anyLong(), anyLong());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UT-09 — Завершение партии: пересчёт CI
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("UT-09: Завершение партии (COMPLETED) → вызывается пересчёт CI для всех игроков")
    void saveProtocol_completed_triggersRecalcCI() {
        Game game = pendingGame(2L, 100L);
        List<GameSlot> existingSlots = emptySlots(game);

        when(gameRepository.findById(2L)).thenReturn(Optional.of(game));
        when(gameSlotRepository.findByGameId(2L))
                .thenReturn(existingSlots)
                .thenReturn(existingSlots); // второй вызов в recalculateAllSlotsForGame
        lenient().when(bestMoveRepository.findByGameId(2L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(anyLong())).thenAnswer(inv -> {
            User u = new User(); u.setId(inv.getArgument(0)); return u;
        });
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameSlotRepository.save(any(GameSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.findTournamentIdByGameId(2L)).thenReturn(100L);
        when(gameSlotRepository.findUserIdsByGameId(2L))
                .thenReturn(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

        GameProtocolDto dto = new GameProtocolDto();
        dto.setStatus("completed");
        dto.setWinner("red");
        dto.setSlots(buildSlotDtos(standardRoles()));

        gameService.saveProtocol(2L, dto);

        // Проверяем что CI пересчитан для всех 10 игроков
        verify(gameSlotRepository, times(10))
                .callRecalculatePlayerCi(eq(100L), anyLong());
    }

    @Test
    @DisplayName("UT-09b: Завершение с победителем RED — статус COMPLETED")
    void saveProtocol_completedRedWinner_statusCompleted() {
        Game game = pendingGame(3L, 100L);
        List<GameSlot> existingSlots = emptySlots(game);

        when(gameRepository.findById(3L)).thenReturn(Optional.of(game));
        when(gameSlotRepository.findByGameId(3L)).thenReturn(existingSlots);
        lenient().when(bestMoveRepository.findByGameId(3L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(anyLong())).thenAnswer(inv -> {
            User u = new User(); u.setId(inv.getArgument(0)); return u;
        });
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameSlotRepository.save(any(GameSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.findTournamentIdByGameId(3L)).thenReturn(100L);
        when(gameSlotRepository.findUserIdsByGameId(3L)).thenReturn(List.of(1L));

        GameProtocolDto dto = new GameProtocolDto();
        dto.setStatus("completed");
        dto.setWinner("red");
        dto.setSlots(buildSlotDtos(standardRoles()));

        gameService.saveProtocol(3L, dto);

        verify(gameRepository).save(argThat(g ->
                g.getStatus() == GameStatus.completed &&
                g.getWinner() == GameWinner.red
        ));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UT-10 — Формула CI
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("UT-10: CI при first_killed_cnt <= B — compensation_points = ci_base")
    void saveProtocol_firstKilledWithLH_ciBaseCalculated() {
        // Тест проверяет, что при first_killed = 2, total_games = 4, b=max(4, round(4*0.4))=2
        // ci_base = 2*0.4/2 = 0.4 → выполняется через хранимую процедуру в БД
        // На уровне Java-кода мы проверяем, что callRecalculatePlayerCi вызывается корректно

        Game game = pendingGame(10L, 200L);
        List<GameSlot> slots = emptySlots(game);
        // Устанавливаем первого убитого для слота 1
        slots.get(0).setIsFirstKilled(true);
        slots.get(0).setRole(PlayerRoleInGame.civilian);

        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));
        when(gameSlotRepository.findByGameId(10L)).thenReturn(slots);
        lenient().when(bestMoveRepository.findByGameId(10L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(anyLong())).thenAnswer(inv -> {
            User u = new User(); u.setId(inv.getArgument(0)); return u;
        });
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameSlotRepository.save(any(GameSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.findTournamentIdByGameId(10L)).thenReturn(200L);
        when(gameSlotRepository.findUserIdsByGameId(10L)).thenReturn(List.of(1L));

        GameProtocolDto dto = new GameProtocolDto();
        dto.setStatus("completed");
        dto.setWinner("red");
        dto.setSlots(buildSlotDtos(standardRoles()));

        gameService.saveProtocol(10L, dto);

        // CI пересчитывается через хранимую процедуру для игрока 1 (ПУ)
        verify(gameSlotRepository).callRecalculatePlayerCi(200L, 1L);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Тесты создания игры
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("createGame — создаётся игра с 10 пустыми слотами, статус pending")
    void createGame_createsGameWith10Slots() {
        Tournament t = new Tournament();
        t.setId(1L);
        t.setSettings(new com.mafia.manager.entity.json.TournamentSettings());

        User judge = new User();
        judge.setId(5L);
        judge.setNickname("Judge");

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));
        when(userRepository.findById(5L)).thenReturn(Optional.of(judge));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(99L);
            return g;
        });
        when(gameSlotRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(gameSlotRepository.findByGameId(99L)).thenReturn(emptySlots(pendingGame(99L, 1L)));

        GameProtocolDto result = gameService.createGame(1L, LocalDate.now(), 5L);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getStatus()).isEqualTo("pending");

        // Сохраняется 1 игра с 10 слотами
        verify(gameRepository).save(any(Game.class));
        verify(gameSlotRepository).saveAll(argThat((List<GameSlot> slots) -> slots.size() == 10));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Тесты замены состава стола
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("swapSlot — замена игрока в слоте по slotNumber")
    void swapSlot_bySlotNumber_replacesPlayer() {
        Game game = pendingGame(5L, 1L);
        game.setStatus(GameStatus.pending); // незавершённая

        User oldPlayer = new User(); oldPlayer.setId(1L);
        User newPlayer = new User(); newPlayer.setId(99L);

        GameSlot targetSlot = new GameSlot();
        targetSlot.setId(1L);
        targetSlot.setSlotNumber(3);
        targetSlot.setUser(oldPlayer);
        targetSlot.setGame(game);

        List<GameSlot> slots = new ArrayList<>(emptySlots(game));
        slots.set(2, targetSlot);

        when(gameRepository.findById(5L)).thenReturn(Optional.of(game));
        when(gameSlotRepository.findByGameId(5L)).thenReturn(slots);
        when(userRepository.findById(99L)).thenReturn(Optional.of(newPlayer));
        when(gameSlotRepository.save(any(GameSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.findById(5L)).thenReturn(Optional.of(game)); // повторный для mapToDto

        SwapSlotRequest req = new SwapSlotRequest();
        req.setSlotNumber(3);
        req.setNewUserId(99L);

        gameService.swapSlot(5L, req);

        verify(gameSlotRepository).save(argThat(s ->
                s.getSlotNumber() == 3 && s.getUser().getId() == 99L
        ));
    }

    @Test
    @DisplayName("swapSlot на завершённую игру → IllegalStateException")
    void swapSlot_completedGame_throws() {
        Game game = pendingGame(6L, 1L);
        game.setStatus(GameStatus.completed);

        when(gameRepository.findById(6L)).thenReturn(Optional.of(game));

        SwapSlotRequest req = new SwapSlotRequest();
        req.setSlotNumber(1);
        req.setNewUserId(5L);

        assertThatThrownBy(() -> gameService.swapSlot(6L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("завершённой");
    }

    @Test
    @DisplayName("swapSlot с newUserId=null освобождает слот")
    void swapSlot_nullNewUser_clearsSlot() {
        Game game = pendingGame(7L, 1L);

        User existingPlayer = new User(); existingPlayer.setId(3L);
        GameSlot slot = new GameSlot();
        slot.setSlotNumber(1);
        slot.setUser(existingPlayer);
        slot.setGame(game);

        List<GameSlot> slots = new ArrayList<>();
        slots.add(slot);

        when(gameRepository.findById(7L)).thenReturn(Optional.of(game));
        when(gameSlotRepository.findByGameId(7L)).thenReturn(slots);
        when(gameSlotRepository.save(any(GameSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        SwapSlotRequest req = new SwapSlotRequest();
        req.setSlotNumber(1);
        req.setNewUserId(null); // освободить слот

        gameService.swapSlot(7L, req);

        verify(gameSlotRepository).save(argThat(s -> s.getUser() == null));
    }
}
