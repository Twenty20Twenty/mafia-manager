package com.mafia.manager.repository;

import com.mafia.manager.entity.PlayerStatsByType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerStatsByTypeRepository extends JpaRepository<PlayerStatsByType, Long> {

    /**
     * Все записи для игрока (все типы турниров, все периоды).
     * Используется на странице профиля для построения агрегатов.
     */
    List<PlayerStatsByType> findByUserId(Long userId);

    /**
     * Все записи для игрока по конкретному типу турнира.
     * Используется при пересчёте, чтобы не трогать другие типы.
     */
    List<PlayerStatsByType> findByUserIdAndTournamentType(Long userId, String tournamentType);

    /**
     * Одна конкретная запись: игрок + тип турнира + период.
     * Используется для upsert при пересчёте.
     */
    Optional<PlayerStatsByType> findByUserIdAndTournamentTypeAndPeriodYear(
            Long userId,
            String tournamentType,
            Short periodYear
    );

    /** Удалить всю статистику по типу для игрока (при полном пересчёте по типу). */
    void deleteByUserIdAndTournamentType(Long userId, String tournamentType);

    /** Удалить всю статистику по типу для всех игроков (при массовом пересчёте). */
    void deleteByTournamentType(String tournamentType);
}
