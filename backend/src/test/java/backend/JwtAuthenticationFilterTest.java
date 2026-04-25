package com.mafia.manager.security;

import com.mafia.manager.entity.User;
import com.mafia.manager.entity.enums.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Тесты JwtAuthenticationFilter.
 * Покрывают: истёкший токен, подделанный токен, валидный токен, отсутствие токена.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter — безопасность JWT")
class JwtAuthenticationFilterTest {

    @Mock JwtService          jwtService;
    @Mock UserDetailsService  userDetailsService;
    @Mock FilterChain         filterChain;
    @Mock HttpServletRequest  request;
    @Mock HttpServletResponse response;

    @InjectMocks JwtAuthenticationFilter filter;

    private CustomUserDetails validUserDetails;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();

        User u = new User();
        u.setId(1L);
        u.setNickname("testUser");
        u.setPasswordHash("$2a$hash");
        u.setRole(UserRole.user);
        u.setIsPhantom(false);
        u.setIsEmailVerified(true);
        validUserDetails = new CustomUserDetails(u);
    }

    @Test
    @DisplayName("Запрос без Authorization-заголовка — фильтр пропускает без аутентификации")
    void noAuthHeader_filterPassesThrough() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Authorization без 'Bearer ' префикса — фильтр пропускает без аутентификации")
    void wrongAuthPrefix_filterPassesThrough() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Валидный JWT — SecurityContext заполняется аутентификацией")
    void validToken_setsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("testUser");
        when(userDetailsService.loadUserByUsername("testUser")).thenReturn(validUserDetails);
        when(jwtService.isTokenValid("valid.jwt.token", validUserDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("Истёкший JWT (ExpiredJwtException) — фильтр продолжает без аутентификации")
    void expiredToken_filterPassesThroughWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.jwt.token");
        when(jwtService.extractUsername("expired.jwt.token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        filter.doFilterInternal(request, response, filterChain);

        // Фильтр НЕ должен бросать исключение наружу
        verify(filterChain).doFilter(request, response);
        // SecurityContext остаётся пустым
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Подделанный JWT (MalformedJwtException) — фильтр продолжает без аутентификации")
    void malformedToken_filterPassesThroughWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer fake.token");
        when(jwtService.extractUsername("fake.token"))
                .thenThrow(new MalformedJwtException("Malformed token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Валидный токен, но isTokenValid=false — аутентификация НЕ устанавливается")
    void tokenInvalidated_noAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer revoked.token");
        when(jwtService.extractUsername("revoked.token")).thenReturn("testUser");
        when(userDetailsService.loadUserByUsername("testUser")).thenReturn(validUserDetails);
        when(jwtService.isTokenValid("revoked.token", validUserDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("JWT с другой подписью — SecurityContext остаётся пустым")
    void wrongSignatureToken_noAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer tampered.signature.token");
        when(jwtService.extractUsername("tampered.signature.token"))
                .thenThrow(new io.jsonwebtoken.security.SecurityException("Invalid signature"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
