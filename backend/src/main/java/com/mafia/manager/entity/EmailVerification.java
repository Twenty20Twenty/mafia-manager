package com.mafia.manager.entity;

import com.mafia.manager.entity.enums.EmailVerificationType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;

/**
 * Хранит одноразовые коды и токены для трёх сценариев:
 * <ul>
 *   <li>REGISTRATION  — 6-значный цифровой код подтверждения email при регистрации</li>
 *   <li>EMAIL_CHANGE  — 6-значный цифровой код подтверждения нового email</li>
 *   <li>PASSWORD_RESET — UUID-токен для сброса пароля (вставляется в ссылку)</li>
 * </ul>
 *
 * <p>Не наследует BaseEntity намеренно: нам не нужен updated_at,
 * а created_at управляется БД через DEFAULT NOW().</p>
 */
@Data
@Entity
@Table(name = "email_verifications")
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "email_verification_type", nullable = false)
    private EmailVerificationType type;

    /** 6 цифр для REGISTRATION/EMAIL_CHANGE, UUID-строка для PASSWORD_RESET. */
    @Column(nullable = false)
    private String code;

    /** Новый email — заполняется только для EMAIL_CHANGE. */
    @Column(name = "new_email")
    private String newEmail;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** NULL = код ещё активен. Заполняется в момент успешного использования. */
    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
