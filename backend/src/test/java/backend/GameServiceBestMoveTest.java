package com.mafia.manager.service;

import com.mafia.manager.entity.GameSlot;
import com.mafia.manager.entity.enums.PlayerRoleInGame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UT-11 — Тесты формулы «Лучший Ход».
 * Покрывают все 4 ветки: 3/3, 2/3, 1/3, 0/3 угаданных чёрных.
 */
@DisplayName("UT-11: Формула Лучший Ход (calcBestMovePoints)")
class GameServiceBestMoveTest {

    // ── Вспомогательный метод создания слота ──────────────────────────────────

    private GameSlot slot(int number, PlayerRoleInGame role) {
        GameSlot s = new GameSlot();
        s.setSlotNumber(number);
        s.setRole(role);
        return s;
    }

    /** 10 слотов: 1=мирный, 2=шериф, 3=мафия, 4=дон, 5-10=мирные */
    private List<GameSlot> standardSlots() {
        return List.of(
            slot(1,  PlayerRoleInGame.civilian),
            slot(2,  PlayerRoleInGame.sheriff),
            slot(3,  PlayerRoleInGame.mafia),
            slot(4,  PlayerRoleInGame.don),
            slot(5,  PlayerRoleInGame.civilian),
            slot(6,  PlayerRoleInGame.civilian),
            slot(7,  PlayerRoleInGame.civilian),
            slot(8,  PlayerRoleInGame.civilian),
            slot(9,  PlayerRoleInGame.civilian),
            slot(10, PlayerRoleInGame.civilian)
        );
    }

    // ── Позитивные сценарии ───────────────────────────────────────────────────

    @Test
    @DisplayName("3 из 3 мафий угаданы → 0.60")
    void threeBlacks_returnsPoint60() {
        // 2 слота мафии + 1 дон → все три чёрные
        List<GameSlot> slots = List.of(
            slot(1, PlayerRoleInGame.mafia),
            slot(2, PlayerRoleInGame.mafia),
            slot(3, PlayerRoleInGame.don),
            slot(4, PlayerRoleInGame.civilian),
            slot(5, PlayerRoleInGame.civilian),
            slot(6, PlayerRoleInGame.civilian),
            slot(7, PlayerRoleInGame.civilian),
            slot(8, PlayerRoleInGame.civilian),
            slot(9, PlayerRoleInGame.sheriff),
            slot(10, PlayerRoleInGame.civilian)
        );

        BigDecimal result = GameService.calcBestMovePoints(List.of(1, 2, 3), slots);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.60"));
    }

    @Test
    @DisplayName("UT-11: 2 из 3 угаданы → 0.30")
    void twoBlacks_returnsPoint30() {
        List<GameSlot> slots = standardSlots(); // 3=мафия, 4=дон, остальные мирные

        BigDecimal result = GameService.calcBestMovePoints(List.of(3, 4, 1), slots);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.30"));
    }

    @Test
    @DisplayName("1 из 3 угадан → 0.10")
    void oneBlack_returnsPoint10() {
        List<GameSlot> slots = standardSlots(); // 3=мафия, остальные мирные/шериф

        BigDecimal result = GameService.calcBestMovePoints(List.of(3, 1, 2), slots);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.10"));
    }

    @Test
    @DisplayName("0 из 3 угадано → 0.00")
    void zeroBlacks_returnsZero() {
        List<GameSlot> slots = standardSlots(); // кандидаты — мирные/шериф

        BigDecimal result = GameService.calcBestMovePoints(List.of(1, 2, 5), slots);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Граничные условия ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Пустой список кандидатов → 0.00")
    void emptyCandidates_returnsZero() {
        BigDecimal result = GameService.calcBestMovePoints(List.of(), standardSlots());
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("null список кандидатов → 0.00")
    void nullCandidates_returnsZero() {
        BigDecimal result = GameService.calcBestMovePoints(null, standardSlots());
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Дублирующийся слот в кандидатах засчитывается только один раз")
    void duplicateCandidateSlot_countedOnce() {
        // Если передать один и тот же чёрный слот трижды — это считается как 1 угаданный
        List<GameSlot> slots = standardSlots(); // 3=мафия

        BigDecimal result = GameService.calcBestMovePoints(List.of(3, 3, 3), slots);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.10")); // 1 уникальный
    }

    @Test
    @DisplayName("Кандидат с null пропускается")
    void nullCandidateInList_ignored() {
        List<GameSlot> slots = standardSlots(); // 3=мафия, 4=дон

        // null в списке не должен вызывать NPE и не засчитывается
        BigDecimal result = GameService.calcBestMovePoints(
            java.util.Arrays.asList(3, null, 4), slots
        );

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.30")); // 2 угаданных
    }

    @Test
    @DisplayName("Несуществующий номер слота пропускается")
    void nonExistentSlotNumber_ignored() {
        List<GameSlot> slots = standardSlots();

        BigDecimal result = GameService.calcBestMovePoints(List.of(99, 100, 3), slots);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.10")); // только 3=мафия
    }

    // ── Параметризованный тест для всех комбинаций ───────────────────────────

    @ParameterizedTest(name = "Кандидаты={0} → ожидается {1}")
    @MethodSource("bestMoveScenarios")
    @DisplayName("Параметризованная проверка формулы ЛХ")
    void calcBestMove_parametrized(List<Integer> candidates, String expectedPoints) {
        List<GameSlot> slots = List.of(
            slot(1,  PlayerRoleInGame.mafia),
            slot(2,  PlayerRoleInGame.mafia),
            slot(3,  PlayerRoleInGame.don),
            slot(4,  PlayerRoleInGame.civilian),
            slot(5,  PlayerRoleInGame.sheriff),
            slot(6,  PlayerRoleInGame.civilian),
            slot(7,  PlayerRoleInGame.civilian),
            slot(8,  PlayerRoleInGame.civilian),
            slot(9,  PlayerRoleInGame.civilian),
            slot(10, PlayerRoleInGame.civilian)
        );

        BigDecimal result = GameService.calcBestMovePoints(candidates, slots);

        assertThat(result).isEqualByComparingTo(new BigDecimal(expectedPoints));
    }

    static Stream<Arguments> bestMoveScenarios() {
        return Stream.of(
            Arguments.of(List.of(1, 2, 3), "0.60"), // все чёрные
            Arguments.of(List.of(1, 2, 4), "0.30"), // мафия+мафия+мирный
            Arguments.of(List.of(1, 3, 4), "0.30"), // мафия+дон+мирный → 2 чёрных
            Arguments.of(List.of(1, 4, 5), "0.10"), // только 1 мафия
            Arguments.of(List.of(4, 5, 6), "0.00"), // все мирные
            Arguments.of(List.of(5, 6, 7), "0.00")  // мирные
        );
    }
}
