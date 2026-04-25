package com.mafia.manager.repository;

import com.mafia.manager.entity.EmailVerification;
import com.mafia.manager.entity.enums.EmailVerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    /** Находит активный (не использованный) код пользователя заданного типа. */
    Optional<EmailVerification> findByUserIdAndTypeAndUsedAtIsNull(
            Long userId,
            EmailVerificationType type
    );

    /** Находит активный код по строке кода и типу (для REGISTRATION и EMAIL_CHANGE). */
    Optional<EmailVerification> findByCodeAndTypeAndUsedAtIsNull(
            String code,
            EmailVerificationType type
    );

    /**
     * Инвалидирует (помечает как использованные) все активные коды
     * пользователя заданного типа. Используется при повторной отправке —
     * старые коды сразу аннулируются.
     */
    @Modifying
    @Query("""
        UPDATE EmailVerification ev
        SET ev.usedAt = CURRENT_TIMESTAMP
        WHERE ev.user.id = :userId
          AND ev.type = :type
          AND ev.usedAt IS NULL
    """)
    void invalidateAllActive(
            @Param("userId") Long userId,
            @Param("type") EmailVerificationType type
    );
}
