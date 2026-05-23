package com.mafia.manager.repository;

import com.mafia.manager.entity.GameAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameAuditLogRepository extends JpaRepository<GameAuditLog, Long> {

    List<GameAuditLog> findByGameIdOrderByCreatedAtAsc(Long gameId);

    List<GameAuditLog> findByTournamentIdOrderByCreatedAtDesc(Long tournamentId);
}
