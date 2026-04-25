package com.mafia.manager.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "best_moves")
public class BestMove {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_slot_id")
    private GameSlot authorSlot;

    @Column(name = "candidate_1_slot")
    private Integer candidate1Slot;

    @Column(name = "candidate_2_slot")
    private Integer candidate2Slot;

    @Column(name = "candidate_3_slot")
    private Integer candidate3Slot;

    @Column(name = "guessed_count")
    private Integer guessedCount;

    // ИСПРАВЛЕНО: Добавлена точность
    @Column(precision = 3, scale = 2)
    private BigDecimal points;
}
