package com.mafia.manager.repository;

import com.mafia.manager.entity.TournamentTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, Long> {

    List<TournamentTeam> findByTournamentId(Long tournamentId);

    long countByTournamentId(Long tournamentId);

    void deleteByTournamentId(Long tournamentId);
}
