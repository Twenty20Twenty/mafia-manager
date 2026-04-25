// java/com/mafia/manager/repository/PlayerStatsRepository.java
package com.mafia.manager.repository;

import com.mafia.manager.entity.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {

    /** Все периоды статистики для конкретного игрока (за всё время + по годам). */
    List<PlayerStats> findByUserId(Long userId);

    /**
     * Один конкретный период:
     * periodYear = null  → запись "за всё время"
     * periodYear = 2025  → запись за 2025 год
     */
    Optional<PlayerStats> findByUserIdAndPeriodYear(Long userId, Short periodYear);

    /** Удалить всю статистику игрока (при полном пересчёте). */
    void deleteByUserId(Long userId);
}
