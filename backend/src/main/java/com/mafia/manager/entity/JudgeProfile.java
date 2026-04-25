package com.mafia.manager.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "judges_profiles")
public class JudgeProfile {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Говорит Hibernate, что PK этой таблицы == PK юзера
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_judge")
    private Boolean isJudge;

    @Column(name = "can_judge_finals")
    private Boolean canJudgeFinals;

    @Column(name = "can_be_head_judge")
    private Boolean canBeHeadJudge;

    @Column(name = "judge_since")
    private LocalDate judgeSince;
}
