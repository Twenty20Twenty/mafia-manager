package com.mafia.manager.entity;

import com.mafia.manager.entity.enums.PlayerRoleInGame;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "game_slots")
public class GameSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "slot_number")
    private Integer slotNumber;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "player_role_in_game")
    private PlayerRoleInGame role;

    @Column(name = "is_first_killed")
    private Boolean isFirstKilled;

    private Integer fouls;

    // Используем BigDecimal для точности, так как в БД DECIMAL
    @Column(name = "extra_points_positive", precision = 4, scale = 1) // Уточнено для соответствия
    private BigDecimal extraPointsPositive;

    @Column(name = "extra_points_negative", precision = 4, scale = 1) // Уточнено для соответствия
    private BigDecimal extraPointsNegative;

    @Column(name = "penalty_points", precision = 4, scale = 1) // Уточнено для соответствия
    private BigDecimal penaltyPoints;

    // ИСПРАВЛЕНО: Добавлена точность
    @Column(name = "compensation_points", precision = 4, scale = 3, insertable = false, updatable = false)
    private BigDecimal compensationPoints;

    // ИСПРАВЛЕНО: Добавлена точность
    @Column(name = "computed_score", precision = 5, scale = 3, insertable = false, updatable = false)
    private BigDecimal computedScore;
}
