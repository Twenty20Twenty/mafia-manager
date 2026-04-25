package com.mafia.manager.service;

import com.mafia.manager.dto.*;
import com.mafia.manager.entity.User;
import com.mafia.manager.entity.enums.UserGender;
import com.mafia.manager.entity.enums.UserRole;
import com.mafia.manager.exception.EmailAlreadyTakenException;
import com.mafia.manager.exception.EmailNotVerifiedException;
import com.mafia.manager.exception.NicknameAlreadyTakenException;
import com.mafia.manager.exception.PhantomAccountException;
import com.mafia.manager.repository.CityRepository;
import com.mafia.manager.repository.UserRepository;
import com.mafia.manager.security.CustomUserDetails;
import com.mafia.manager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository             userRepository;
    private final CityRepository             cityRepository;
    private final PasswordEncoder            passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager      authenticationManager;
    private final EmailVerificationService   emailVerificationService;

    // ── 1. РЕГИСТРАЦИЯ ────────────────────────────────────────────────────────

    /**
     * Регистрирует пользователя и отправляет 6-значный код подтверждения на email.
     * JWT не выдаётся до подтверждения. Возвращает userId для последующего вызова
     * /verify-email или /resend-code.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        validateNicknameUnique(request.getNickname());
        validateEmailUnique(request.getEmail());

        User user = buildNewUser(request);
        userRepository.save(user);

        // Отправляем код подтверждения (@Async — не блокирует ответ)
        emailVerificationService.sendRegistrationCode(user);

        return new RegisterResponse(user.getId(), user.getNickname(), user.getEmail());
    }

    // ── 2. ВХОД ───────────────────────────────────────────────────────────────

    /**
     * Аутентифицирует пользователя. Фантомы входить не могут.
     * Обычные пользователи — только с подтверждённым email.
     */
    public AuthResponse authenticate(AuthRequest request) {
        // Проверяем фантома ДО вызова authenticationManager —
        // у фантома passwordHash = null, что даст BadCredentials вместо понятного сообщения
        User candidate = userRepository.findByNickname(request.getNickname())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (Boolean.TRUE.equals(candidate.getIsPhantom())) {
            throw new PhantomAccountException();
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getNickname(), request.getPassword())
        );

        // Перечитываем после аутентификации (на случай lazy-полей)
        User user = userRepository.findByNickname(request.getNickname())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new EmailNotVerifiedException(user.getId(), user.getEmail());
        }

        return buildAuthResponse(user);
    }

    // ── 3. ПОДТВЕРЖДЕНИЕ EMAIL ────────────────────────────────────────────────

    /**
     * Подтверждает email по коду из письма. После успеха выдаёт JWT.
     */
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        emailVerificationService.confirmRegistration(request.getUserId(), request.getCode());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return buildAuthResponse(user);
    }

    // ── 4. ПОВТОРНАЯ ОТПРАВКА КОДА ────────────────────────────────────────────

    /** Повторно отправляет код подтверждения регистрации. Предыдущий инвалидируется. */
    @Transactional
    public void resendRegistrationCode(ResendCodeRequest request) {
        emailVerificationService.resendRegistrationCode(request.getUserId());
    }

    // ── 5. СБРОС ПАРОЛЯ ───────────────────────────────────────────────────────

    /**
     * Инициирует сброс пароля. Если email не найден — молчим (не раскрываем наличие аккаунта).
     * Всегда возвращает 200.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        emailVerificationService.requestPasswordReset(request.getEmail());
    }

    /**
     * Устанавливает новый пароль по токену из ссылки.
     * Возвращает JWT — пользователь сразу авторизован.
     */
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = emailVerificationService.validateResetTokenAndGetUser(request.getToken());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    // ── 6. СОЗДАНИЕ ФАНТОМНОГО АККАУНТА (ADMIN) ───────────────────────────────

    /**
     * Создаёт фантомный аккаунт без email.
     * isEmailVerified намеренно НЕ ставится в true — при активации через claimPhantom
     * пользователь подтверждает email отдельным шагом.
     */
    @Transactional
    public PhantomResponse createPhantom(CreatePhantomRequest request) {
        validateNicknameUnique(request.getNickname());

        User phantom = new User();
        phantom.setNickname(request.getNickname());
        phantom.setIsPhantom(true);
        phantom.setIsEmailVerified(false);
        phantom.setRole(UserRole.user);
        phantom.setPhantomCode(generateActivationCode());

        applyCity(phantom, request.getCity());
        applyGender(phantom, request.getGender());

        userRepository.save(phantom);

        return new PhantomResponse(phantom.getNickname(), phantom.getPhantomCode());
    }

    // ── 7. АКТИВАЦИЯ ФАНТОМНОГО АККАУНТА ─────────────────────────────────────

    /**
     * Привязывает фантомный аккаунт к реальному пользователю.
     * После привязки email нужно подтвердить — отправляем 6-значный код.
     * JWT не выдаётся до подтверждения, возвращаем { userId, email } как при регистрации.
     */
    @Transactional
    public RegisterResponse claimPhantom(ClaimPhantomRequest request) {
        User user = userRepository.findByPhantomCode(request.getPhantomCode())
                .orElseThrow(() -> new RuntimeException("Неверный код привязки"));

        validateNicknameMatchesPhantom(user, request.getNickname());
        validatePhantomNotYetClaimed(user);
        validateEmailUnique(request.getNewEmail());

        activatePhantomAccount(user, request.getNewEmail(), request.getNewPassword());
        userRepository.save(user);

        // Отправляем код подтверждения — как при обычной регистрации
        emailVerificationService.sendRegistrationCode(user);

        return new RegisterResponse(user.getId(), user.getNickname(), user.getEmail());
    }

    // ── ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ────────────────────────────────────────────────

    private User buildNewUser(RegisterRequest request) {
        User user = new User();
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.user);
        user.setIsPhantom(false);
        user.setIsEmailVerified(false); // подтверждается отдельным шагом

        applyCity(user, request.getCity());
        applyGender(user, request.getGender());

        return user;
    }

    private void applyCity(User user, String cityName) {
        if (cityName == null) return;
        cityRepository.findByName(cityName).ifPresent(user::setCity);
    }

    private void applyGender(User user, String gender) {
        try {
            user.setGender(UserGender.valueOf(gender));
        } catch (Exception e) {
            user.setGender(UserGender.male);
        }
    }

    private void activatePhantomAccount(User user, String newEmail, String newPassword) {
        user.setEmail(newEmail);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setIsPhantom(false);
        user.setPhantomCode(null);
        user.setIsEmailVerified(false); // подтверждается отдельным шагом через /verify-email
    }

    private String generateActivationCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(new CustomUserDetails(user));
        return new AuthResponse(
                token,
                user.getNickname(),
                user.getRole().name(),
                user.getId(),
                user.getClub() != null ? user.getClub().getId() : null
        );
    }

    // ── ВАЛИДАЦИЯ ─────────────────────────────────────────────────────────────

    private void validateNicknameUnique(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new NicknameAlreadyTakenException(nickname);
        }
    }

    private void validateEmailUnique(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyTakenException(email);
        }
    }

    private void validateNicknameMatchesPhantom(User phantom, String requestedNickname) {
        if (requestedNickname == null
                || !phantom.getNickname().equalsIgnoreCase(requestedNickname.trim())) {
            throw new RuntimeException("Никнейм не совпадает с кодом привязки");
        }
    }

    private void validatePhantomNotYetClaimed(User user) {
        if (!Boolean.TRUE.equals(user.getIsPhantom())) {
            throw new RuntimeException("Этот аккаунт уже активирован");
        }
    }
}
