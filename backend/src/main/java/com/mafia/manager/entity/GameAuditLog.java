package com.mafia.manager.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Аудит-лог операций с игровыми протоколами.
 *
 * <p>Фиксирует каждое изменение протокола: создание, сохранение черновика,
 * завершение игры, удаление, замену игрока в слоте.</p>
 *
 * <p>Не наследует BaseEntity — не нужен updated_at, только created_at.</p>
 */
@Data
@Entity
@Table(name = "game_audit_logs",
        indexes = {
                @Index(name = "idx_game_audit_game_id",  columnList = "game_id"),
                @Index(name = "idx_game_audit_actor_id", columnList = "actor_id"),
        })
public class GameAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID игры, к которой относится событие. Nullable — при массовом удалении. */
    @Column(name = "game_id")
    private Long gameId;

    /** ID турнира (денормализовано для удобства запросов). */
    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    /**
     * Тип операции.
     * Хранится как VARCHAR, чтобы не зависеть от миграции enum-а в БД.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 32)
    private GameAuditAction action;

    /** ID пользователя, выполнившего операцию. */
    @Column(name = "actor_id")
    private Long actorId;

    /** Никнейм актора на момент операции (денормализовано — не меняется при смене ника). */
    @Column(name = "actor_nickname", length = 50)
    private String actorNickname;

    /**
     * Дополнительные детали в формате JSON или plain-text.
     * Например: "winner=red, status=completed" или "slotNumber=5, newUserId=42".
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // ── Фабричные методы ──────────────────────────────────────────────────────

    public static GameAuditLog of(
            Long gameId, Long tournamentId,
            GameAuditAction action,
            Long actorId, String actorNickname,
            String details
    ) {
        GameAuditLog log = new GameAuditLog();
        log.setGameId(gameId);
        log.setTournamentId(tournamentId);
        log.setAction(action);
        log.setActorId(actorId);
        log.setActorNickname(actorNickname);
        log.setDetails(details);
        return log;
    }
}
