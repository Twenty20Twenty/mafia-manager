package com.mafia.manager.repository;

import com.mafia.manager.entity.TournamentSeedingException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SeedingExceptionRepository extends JpaRepository<TournamentSeedingException, Long> {
    List<TournamentSeedingException> findByTournamentId(Long tournamentId);
}
