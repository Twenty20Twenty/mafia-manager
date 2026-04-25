// java/com/mafia/manager/repository/GameSlotRepository.java
package com.mafia.manager.repository;

import com.mafia.manager.entity.GameSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameSlotRepository extends JpaRepository<GameSlot, Long> {

    List<GameSlot> findByGameId(Long gameId);

    /**
     * Все слоты конкретного игрока в ЗАВЕРШЁННЫХ играх (status = 'completed').
     * Жадно подгружаем game и tournament, чтобы избежать N+1.
     */
    @Query("""
        SELECT gs FROM GameSlot gs
        JOIN FETCH gs.game g
        WHERE gs.user.id = :userId
          AND g.status = 'completed'
          AND g.winner IS NOT NULL
        ORDER BY g.date ASC NULLS LAST
    """)
    List<GameSlot> findCompletedSlotsByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT recalculate_player_ci(:tournamentId, :userId)", nativeQuery = true)
    void callRecalculatePlayerCi(@Param("tournamentId") Long tournamentId,
                                 @Param("userId") Long userId);

    @Query(value = "SELECT user_id FROM game_slots WHERE game_id = :gameId AND user_id IS NOT NULL", nativeQuery = true)
    List<Long> findUserIdsByGameId(@Param("gameId") Long gameId);
}
