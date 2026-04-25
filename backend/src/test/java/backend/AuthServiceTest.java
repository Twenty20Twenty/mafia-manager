package com.mafia.manager.service;

import com.mafia.manager.dto.*;
import com.mafia.manager.entity.User;
import com.mafia.manager.entity.enums.UserRole;
import com.mafia.manager.exception.EmailAlreadyTakenException;
import com.mafia.manager.exception.EmailNotVerifiedException;
import com.mafia.manager.exception.NicknameAlreadyTakenException;
import com.mafia.manager.exception.PhantomAccountException;
import com.mafia.manager.repository.CityRepository;
import com.mafia.manager.repository.UserRepository;
import com.mafia.manager.security.CustomUserDetails;
import com.mafia.manager.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UT-01, UT-02, UT-03, UT-04, UT-12, UT-13 — Тесты AuthService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — модульные тесты")
class AuthServiceTest {

    @Mock UserRepository           userRepository;
    @Mock CityRepository           cityRepository;
    @Mock PasswordEncoder          passwordEncoder;
    @Mock JwtService               jwtService;
    @Mock AuthenticationManager    authenticationManager;
    @Mock EmailVerificationService emailVerificationService;

    @InjectMocks AuthService authService;

    // ── Фабрика пользователей ─────────────────────────────────────────────────

    private User realUser(String nickname, String email, boolean verified) {
        User u = new User();
        u.setId(1L);
        u.setNickname(nickname);
        u.setEmail(email);
        u.setPasswordHash("$2a$encoded");
        u.setRole(UserRole.user);
        u.setIsPhantom(false);
        u.setIsEmailVerified(verified);
        return u;
    }

    private User phantomUser(String nickname, String code) {
        User u = new User();
        u.setId(2L);
        u.setNickname(nickname);
        u.setRole(UserRole.user);
        u.setIsPhantom(true);
        u.setIsEmailVerified(false);
        u.setPhantomCode(code);
        return u;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UT-01, UT-02 — Регистрация
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("UT-01: Успешная регистрация — создаётся пользователь, код отправлен")
        void register_success_createsUserAndSendsCode() {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("testUser");
            req.setEmail("test@mail.ru");
            req.setPassword("Secret1!");

            when(userRepository.existsByNickname("testUser")).thenReturn(false);
            when(userRepository.existsByEmail("test@mail.ru")).thenReturn(false);
            when(passwordEncoder.encode("Secret1!")).thenReturn("$2a$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            RegisterResponse response = authService.register(req);

            assertThat(response.getNickname()).isEqualTo("testUser");
            assertThat(response.getEmail()).isEqualTo("test@mail.ru");
            assertThat(response.getUserId()).isEqualTo(1L);

            verify(emailVerificationService).sendRegistrationCode(any(User.class));
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("UT-02: Занятый e-mail → EmailAlreadyTakenException")
        void register_duplicateEmail_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("newUser");
            req.setEmail("existing@mail.ru");
            req.setPassword("Secret1!");

            when(userRepository.existsByNickname("newUser")).thenReturn(false);
            when(userRepository.existsByEmail("existing@mail.ru")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(EmailAlreadyTakenException.class)
                    .hasMessageContaining("existing@mail.ru");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Занятый никнейм → NicknameAlreadyTakenException")
        void register_duplicateNickname_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("taken");
            req.setEmail("new@mail.ru");
            req.setPassword("pass123");

            when(userRepository.existsByNickname("taken")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(NicknameAlreadyTakenException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Новый пользователь создаётся с is_phantom=false и is_email_verified=false")
        void register_newUser_hasCorrectDefaults() {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("fresh");
            req.setEmail("fresh@mail.ru");
            req.setPassword("pass123");

            when(userRepository.existsByNickname(any())).thenReturn(false);
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(10L);
                return u;
            });

            authService.register(req);

            verify(userRepository).save(argThat(u ->
                    !Boolean.TRUE.equals(u.getIsPhantom()) &&
                    !Boolean.TRUE.equals(u.getIsEmailVerified())
            ));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UT-03, UT-04 — Вход
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("authenticate()")
    class AuthenticateTests {

        @Test
        @DisplayName("UT-03: Корректные данные → возвращает JWT и данные пользователя")
        void authenticate_validCredentials_returnsToken() {
            User user = realUser("player1", "player@mail.ru", true);
            AuthRequest req = new AuthRequest();
            req.setNickname("player1");
            req.setPassword("correctPass");

            when(userRepository.findByNickname("player1")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("jwt.token.here");

            AuthResponse response = authService.authenticate(req);

            assertThat(response.getToken()).isEqualTo("jwt.token.here");
            assertThat(response.getNickname()).isEqualTo("player1");
            assertThat(response.getRole()).isEqualTo("user");
            assertThat(response.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("UT-04: Неверный пароль → BadCredentialsException")
        void authenticate_wrongPassword_throws() {
            User user = realUser("player1", "player@mail.ru", true);
            AuthRequest req = new AuthRequest();
            req.setNickname("player1");
            req.setPassword("wrongPass");

            when(userRepository.findByNickname("player1")).thenReturn(Optional.of(user));
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));

            assertThatThrownBy(() -> authService.authenticate(req))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Email не подтверждён → EmailNotVerifiedException с userId и email")
        void authenticate_emailNotVerified_throwsWithDetails() {
            User user = realUser("unverified", "u@mail.ru", false);
            AuthRequest req = new AuthRequest();
            req.setNickname("unverified");
            req.setPassword("pass");

            when(userRepository.findByNickname("unverified"))
                    .thenReturn(Optional.of(user))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.authenticate(req))
                    .isInstanceOf(EmailNotVerifiedException.class)
                    .satisfies(ex -> {
                        EmailNotVerifiedException e = (EmailNotVerifiedException) ex;
                        assertThat(e.getUserId()).isEqualTo(1L);
                        assertThat(e.getEmail()).isEqualTo("u@mail.ru");
                    });
        }

        @Test
        @DisplayName("Фантомный аккаунт → PhantomAccountException до вызова authManager")
        void authenticate_phantomAccount_throwsBeforeAuth() {
            User phantom = phantomUser("ghost", "ABC12345");
            AuthRequest req = new AuthRequest();
            req.setNickname("ghost");
            req.setPassword("anything");

            when(userRepository.findByNickname("ghost")).thenReturn(Optional.of(phantom));

            assertThatThrownBy(() -> authService.authenticate(req))
                    .isInstanceOf(PhantomAccountException.class);

            // authenticationManager не должен вызываться для фантома
            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Несуществующий пользователь → RuntimeException")
        void authenticate_unknownUser_throws() {
            AuthRequest req = new AuthRequest();
            req.setNickname("nobody");
            req.setPassword("pass");

            when(userRepository.findByNickname("nobody")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.authenticate(req))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UT-12 — Создание фантомного аккаунта
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createPhantom()")
    class CreatePhantomTests {

        @Test
        @DisplayName("UT-12: Создание фантома — is_phantom=true, код не null")
        void createPhantom_success_setsPhantomFlags() {
            CreatePhantomRequest req = new CreatePhantomRequest();
            req.setNickname("ghost1");
            req.setGender("male");

            when(userRepository.existsByNickname("ghost1")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(99L);
                return u;
            });

            PhantomResponse response = authService.createPhantom(req);

            assertThat(response.getNickname()).isEqualTo("ghost1");
            assertThat(response.getCode()).isNotNull().isNotBlank();

            verify(userRepository).save(argThat(u ->
                    Boolean.TRUE.equals(u.getIsPhantom()) &&
                    !Boolean.TRUE.equals(u.getIsEmailVerified()) &&
                    u.getPhantomCode() != null
            ));
        }

        @Test
        @DisplayName("Занятый никнейм при создании фантома → NicknameAlreadyTakenException")
        void createPhantom_duplicateNickname_throws() {
            CreatePhantomRequest req = new CreatePhantomRequest();
            req.setNickname("existing");

            when(userRepository.existsByNickname("existing")).thenReturn(true);

            assertThatThrownBy(() -> authService.createPhantom(req))
                    .isInstanceOf(NicknameAlreadyTakenException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UT-13 — Активация фантомного аккаунта
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("claimPhantom()")
    class ClaimPhantomTests {

        @Test
        @DisplayName("UT-13: Успешная активация — is_phantom=false, email установлен")
        void claimPhantom_success_activatesAccount() {
            User phantom = phantomUser("ghost1", "ABC12345");
            ClaimPhantomRequest req = new ClaimPhantomRequest();
            req.setNickname("ghost1");
            req.setPhantomCode("ABC12345");
            req.setNewEmail("real@mail.ru");
            req.setNewPassword("newPass123");

            when(userRepository.findByPhantomCode("ABC12345")).thenReturn(Optional.of(phantom));
            when(userRepository.existsByEmail("real@mail.ru")).thenReturn(false);
            when(passwordEncoder.encode("newPass123")).thenReturn("$2a$hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            RegisterResponse response = authService.claimPhantom(req);

            assertThat(response.getUserId()).isEqualTo(2L);
            assertThat(response.getEmail()).isEqualTo("real@mail.ru");

            // Проверяем, что пользователь больше не фантом
            verify(userRepository).save(argThat(u ->
                    !Boolean.TRUE.equals(u.getIsPhantom()) &&
                    "real@mail.ru".equals(u.getEmail()) &&
                    u.getPhantomCode() == null
            ));
            // Код подтверждения должен быть отправлен
            verify(emailVerificationService).sendRegistrationCode(any(User.class));
        }

        @Test
        @DisplayName("Неверный код привязки → RuntimeException")
        void claimPhantom_wrongCode_throws() {
            when(userRepository.findByPhantomCode("WRONG")).thenReturn(Optional.empty());

            ClaimPhantomRequest req = new ClaimPhantomRequest();
            req.setPhantomCode("WRONG");
            req.setNickname("ghost1");
            req.setNewEmail("e@mail.ru");
            req.setNewPassword("pass");

            assertThatThrownBy(() -> authService.claimPhantom(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Неверный код");
        }

        @Test
        @DisplayName("Никнейм не совпадает с кодом → RuntimeException")
        void claimPhantom_nicknameMismatch_throws() {
            User phantom = phantomUser("ghost1", "ABC12345");
            when(userRepository.findByPhantomCode("ABC12345")).thenReturn(Optional.of(phantom));

            ClaimPhantomRequest req = new ClaimPhantomRequest();
            req.setPhantomCode("ABC12345");
            req.setNickname("wrongNick");
            req.setNewEmail("e@mail.ru");
            req.setNewPassword("pass");

            assertThatThrownBy(() -> authService.claimPhantom(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Никнейм");
        }

        @Test
        @DisplayName("Email уже занят → EmailAlreadyTakenException")
        void claimPhantom_emailTaken_throws() {
            User phantom = phantomUser("ghost1", "ABC12345");
            when(userRepository.findByPhantomCode("ABC12345")).thenReturn(Optional.of(phantom));
            when(userRepository.existsByEmail("taken@mail.ru")).thenReturn(true);

            ClaimPhantomRequest req = new ClaimPhantomRequest();
            req.setPhantomCode("ABC12345");
            req.setNickname("ghost1");
            req.setNewEmail("taken@mail.ru");
            req.setNewPassword("pass");

            assertThatThrownBy(() -> authService.claimPhantom(req))
                    .isInstanceOf(EmailAlreadyTakenException.class);
        }

        @Test
        @DisplayName("Аккаунт уже активирован (не фантом) → RuntimeException")
        void claimPhantom_alreadyActivated_throws() {
            User realUser = realUser("ghost1", "real@mail.ru", true);
            realUser.setPhantomCode("ABC12345");
            // is_phantom = false → уже активирован
            when(userRepository.findByPhantomCode("ABC12345")).thenReturn(Optional.of(realUser));
            lenient().when(userRepository.existsByEmail(anyString())).thenReturn(false);

            ClaimPhantomRequest req = new ClaimPhantomRequest();
            req.setPhantomCode("ABC12345");
            req.setNickname("ghost1");
            req.setNewEmail("new@mail.ru");
            req.setNewPassword("pass");

            assertThatThrownBy(() -> authService.claimPhantom(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("уже активирован");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Сброс пароля
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("forgotPassword() / resetPassword()")
    class PasswordResetTests {

        @Test
        @DisplayName("forgotPassword — несуществующий email не вызывает ошибку (скрытие факта аккаунта)")
        void forgotPassword_unknownEmail_silentlyIgnored() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("nobody@mail.ru");

            // emailVerificationService вызовется с пустым Optional — не должно бросать
            doNothing().when(emailVerificationService).requestPasswordReset("nobody@mail.ru");

            // Не должно бросать исключение
            authService.forgotPassword(req);

            verify(emailVerificationService).requestPasswordReset("nobody@mail.ru");
        }

        @Test
        @DisplayName("resetPassword — успешный сброс возвращает JWT")
        void resetPassword_validToken_returnsToken() {
            User user = realUser("player", "p@mail.ru", true);
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setToken("valid-uuid-token");
            req.setNewPassword("newSecurePass");

            when(emailVerificationService.validateResetTokenAndGetUser("valid-uuid-token"))
                    .thenReturn(user);
            when(passwordEncoder.encode("newSecurePass")).thenReturn("$2a$newHash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any())).thenReturn("new.jwt.token");

            AuthResponse response = authService.resetPassword(req);

            assertThat(response.getToken()).isEqualTo("new.jwt.token");
            verify(userRepository).save(argThat(u -> "$2a$newHash".equals(u.getPasswordHash())));
        }
    }
}
