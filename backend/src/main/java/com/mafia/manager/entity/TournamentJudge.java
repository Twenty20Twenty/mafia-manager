package com.mafia.manager.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tournament_judges")
public class TournamentJudge {

    @EmbeddedId
    private TournamentJudgeId id = new TournamentJudgeId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tournamentId")
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
}