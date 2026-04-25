package com.mafia.manager.service;

import com.mafia.manager.entity.EmailVerification;
import com.mafia.manager.entity.User;
import com.mafia.manager.entity.enums.EmailVerificationType;
import com.mafia.manager.exception.PhantomAccountException;
import com.mafia.manager.repository.EmailVerificationRepository;
import com.mafia.manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Вся бизнес-логика email-подтверждений.
 *
 * Три сценария:
 * 1. REGISTRATION  — 6-значный код, TTL 15 минут
 * 2. EMAIL_CHANGE  — 6-значный код, TTL 15 минут
 * 3. PASSWORD_RESET — UUID-токен, TTL 1 час
 *
 * EmailService отвечает только за отправку писем.
 * AuthService и UserService вызывают методы этого сервиса.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository              userRepository;
    private final EmailService                emailService;

    private static final int CODE_TTL_MINUTES = 15;
    private static final int TOKEN_TTL_HOURS  = 1;

    // ── 1. РЕГИСТРАЦИЯ ────────────────────────────────────────────────────────

    /**
     * Создаёт код подтверждения для только что зарегистрированного пользователя
     * и отправляет письмо. Вызывается из AuthService.register() сразу после сохранения User.
     */
    @Transactional
    public void sendRegistrationCode(User user) {
        String code = createAndSaveCode(user, EmailVerificationType.REGISTRATION, null);
        emailService.sendRegistrationCode(user.getEmail(), user.getNickname(), code);
    }

    /**
     * Повторная отправка кода регистрации (кнопка «Отправить снова»).
     * Предыдущие активные коды инвалидируются.
     *
     * Фантомный аккаунт не имеет email — отправка невозможна и бессмысленна.
     */
    @Transactional
    public void resendRegistrationCode(Long userId) {
        User user = findUserOrThrow(userId);

        if (Boolean.TRUE.equals(user.getIsPhantom())) {
            throw new PhantomAccountException();
        }

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new IllegalStateException("Email уже подтверждён.");
        }

        verificationRepository.invalidateAllActive(userId, EmailVerificationType.REGISTRATION);
        String code = createAndSaveCode(user, EmailVerificationType.REGISTRATION, null);
        emailService.sendRegistrationCode(user.getEmail(), user.getNickname(), code);
    }

    /**
     * Подтверждает email по коду. Устанавливает is_email_verified = true.
     *
     * Фантомный аккаунт не может подтверждать email через этот flow.
     *
     * @throws RuntimeException если код не найден, уже использован или истёк
     */
    @Transactional
    public void confirmRegistration(Long userId, String code) {
        User user = findUserOrThrow(userId);

        if (Boolean.TRUE.equals(user.getIsPhantom())) {
            throw new PhantomAccountException();
        }

        EmailVerification ev = findActiveCode(userId, EmailVerificationType.REGISTRATION);
        validateCode(ev, code);
        markUsed(ev);

        user.setIsEmailVerified(true);
        userRepository.save(user);
    }

    // ── 2. СМЕНА EMAIL ────────────────────────────────────────────────────────

    /**
     * Запрашивает смену email: проверяет что новый email свободен,
     * сохраняет запись с new_email и отправляет код на НОВЫЙ адрес.
     */
    @Transactional
    public void requestEmailChange(Long userId, String newEmail) {
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email уже используется другим аккаунтом");
        }

        User user = findUserOrThrow(userId);
        verificationRepository.invalidateAllActive(userId, EmailVerificationType.EMAIL_CHANGE);
        String code = createAndSaveCode(user, EmailVerificationType.EMAIL_CHANGE, newEmail);
        emailService.sendEmailChangeCode(newEmail, user.getNickname(), code);
    }

    /**
     * Подтверждает смену email по коду. Обновляет users.email.
     */
    @Transactional
    public void confirmEmailChange(Long userId, String code) {
        EmailVerification ev = findActiveCode(userId, EmailVerificationType.EMAIL_CHANGE);
        validateCode(ev, code);

        String newEmail = ev.getNewEmail();
        if (newEmail == null) {
            throw new RuntimeException("Некорректная запись смены email: new_email отсутствует");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email уже занят — попробуйте запросить смену заново");
        }

        markUsed(ev);

        User user = ev.getUser();
        user.setEmail(newEmail);
        userRepository.save(user);
    }

    // ── 3. СБРОС ПАРОЛЯ ───────────────────────────────────────────────────────

    /**
     * Инициирует сброс пароля по email. Если пользователь не найден —
     * молчаливо игнорируем (не раскрываем факт существования аккаунта).
     */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            verificationRepository.invalidateAllActive(user.getId(), EmailVerificationType.PASSWORD_RESET);
            String token = UUID.randomUUID().toString();
            createAndSaveToken(user, token);
            emailService.sendPasswordResetLink(email, user.getNickname(), token);
        });
    }

    /**
     * Проверяет UUID-токен из ссылки и возвращает пользователя.
     */
    @Transactional
    public User validateResetTokenAndGetUser(String token) {
        EmailVerification ev = verificationRepository
                .findByCodeAndTypeAndUsedAtIsNull(token, EmailVerificationType.PASSWORD_RESET)
                .orElseThrow(() -> new RuntimeException("Ссылка для сброса пароля недействительна или устарела"));

        if (ev.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Ссылка для сброса пароля истекла. Запросите новую.");
        }

        markUsed(ev);
        return ev.getUser();
    }

    // ── ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ────────────────────────────────────────────────

    private String createAndSaveCode(User user, EmailVerificationType type, String newEmail) {
        String code = generateNumericCode();
        EmailVerification ev = buildVerification(user, type, code, newEmail, CODE_TTL_MINUTES * 60L);
        verificationRepository.save(ev);
        return code;
    }

    private void createAndSaveToken(User user, String token) {
        EmailVerification ev = buildVerification(
                user, EmailVerificationType.PASSWORD_RESET, token, null, TOKEN_TTL_HOURS * 3600L);
        verificationRepository.save(ev);
    }

    private EmailVerification buildVerification(User user, EmailVerificationType type,
                                                String code, String newEmail, long ttlSeconds) {
        EmailVerification ev = new EmailVerification();
        ev.setUser(user);
        ev.setType(type);
        ev.setCode(code);
        ev.setNewEmail(newEmail);
        ev.setExpiresAt(OffsetDateTime.now().plusSeconds(ttlSeconds));
        return ev;
    }

    private EmailVerification findActiveCode(Long userId, EmailVerificationType type) {
        return verificationRepository
                .findByUserIdAndTypeAndUsedAtIsNull(userId, type)
                .orElseThrow(() -> new RuntimeException("Активный код не найден. Запросите новый."));
    }

    private void validateCode(EmailVerification ev, String inputCode) {
        if (ev.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Код истёк. Запросите новый.");
        }
        if (!ev.getCode().equals(inputCode)) {
            throw new RuntimeException("Неверный код.");
        }
    }

    private void markUsed(EmailVerification ev) {
        ev.setUsedAt(OffsetDateTime.now());
        verificationRepository.save(ev);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    private String generateNumericCode() {
        int number = new Random().nextInt(1_000_000);
        return String.format("%06d", number);
    }
}
