package com.mafia.manager.repository;

import com.mafia.manager.entity.TournamentParticipant;
import com.mafia.manager.entity.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
    List<TournamentParticipant> findByTournamentId(Long tournamentId);
    Optional<TournamentParticipant> findByTournamentIdAndUserId(Long tournamentId, Long userId);
    boolean existsByTournamentIdAndUserId(Long tournamentId, Long userId);

    List<TournamentParticipant> findByTournamentIdAndStatus(Long tournamentId, ParticipantStatus status);
}
