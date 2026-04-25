package com.mafia.manager.repository;

import com.mafia.manager.entity.TournamentJudge;
import com.mafia.manager.entity.TournamentJudgeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TournamentJudgeRepository extends JpaRepository<TournamentJudge, TournamentJudgeId> {

    List<TournamentJudge> findByTournamentId(Long tournamentId);

    @Modifying
    @Query("DELETE FROM TournamentJudge tj WHERE tj.tournament.id = :tournamentId")
    void deleteByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query("SELECT tj.user.id FROM TournamentJudge tj WHERE tj.tournament.id = :tournamentId")
    List<Long> findUserIdsByTournamentId(@Param("tournamentId") Long tournamentId);
}