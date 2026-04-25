// java/com/mafia/manager/repository/BestMoveRepository.java
package com.mafia.manager.repository;

import com.mafia.manager.entity.BestMove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface BestMoveRepository extends JpaRepository<BestMove, Long> {

    Optional<BestMove> findByGameId(Long gameId);

    void deleteByGameId(Long gameId);

    /**
     * ID всех игр, где конкретный игрок сделал Лучший Ход.
     * Автор ЛХ = authorSlot.user.id
     */
    @Query("""
        SELECT bm.game.id FROM BestMove bm
        WHERE bm.authorSlot.user.id = :userId
    """)
    Set<Long> findGameIdsWhereAuthorUserId(@Param("userId") Long userId);

    /**
     * ID игр, где игрок угадал все 3 кандидата (идеальный ЛХ).
     * "Идеальный" = points >= 0.6 (угадал 3 из 3 чёрных).
     */
    @Query("""
        SELECT bm.game.id FROM BestMove bm
        WHERE bm.authorSlot.user.id = :userId
          AND bm.points >= 0.6
    """)
    Set<Long> findGameIdsWherePerfectBestMoveByUserId(@Param("userId") Long userId);
}
