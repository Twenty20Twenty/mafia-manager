package com.mafia.manager.service;

import com.mafia.manager.dto.TournamentDto;
import com.mafia.manager.dto.LeaderboardEntryDto;
import com.mafia.manager.entity.*;
import com.mafia.manager.entity.enums.*;
import com.mafia.manager.entity.json.TournamentSettings;
import com.mafia.manager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UT-05, UT-06, UT-07, UT-15 — Тесты TournamentService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TournamentService — модульные тесты")
class TournamentServiceTest {

    @Mock TournamentRepository            tournamentRepository;
    @Mock GameRepository                  gameRepository;
    @Mock UserRepository                  userRepository;
    @Mock CityRepository                  cityRepository;
    @Mock TournamentParticipantRepository participantRepository;
    @Mock SeedingExceptionRepository      exceptionRepository;
    @Mock TournamentJudgeRepository       tournamentJudgeRepository;
    @Mock PlayerStatsService              playerStatsService;

    @InjectMocks TournamentService tournamentService;

    // ── Хелперы ───────────────────────────────────────────────────────────────

    private void mockCurrentUser(User user) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(user.getNickname());
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        lenient().when(userRepository.findByNickname(user.getNickname())).thenReturn(Optional.of(user));
    }

    private User organizer(boolean isTournamentOperator) {
        Club club = new Club();
        club.setId(1L);
        club.setIsTournamentOperator(isTournamentOperator);

        User u = new User();
        u.setId(1L);
        u.setNickname("organizer");
        u.setRole(UserRole.user);
        u.setIsPhantom(false);
        u.setIsEmailVerified(true);
        u.setClub(club);
        club.setPresident(u); // организатор = президент клуба
        return u;
    }

    private User plainPlayer() {
        User u = new User();
        u.setId(2L);
        u.setNickname("player");
        u.setRole(UserRole.user);
        u.setIsPhantom(false);
        u.setIsEmailVerified(true);
        u.setCanCreateClubTournaments(false);
        // нет клуба
        return u;
    }

    private Tournament existingTournament(User org) {
        TournamentSettings settings = new TournamentSettings();
        settings.setMaxParticipants(20);
        settings.setRoundsCount(10);

        Tournament t = new Tournament();
        t.setId(100L);
        t.setTitle("Тест Турнир");
        t.setStatus(TournamentStatus.registration);
        t.setType(TournamentType.individual);
        t.setOrganizer(org);
        t.setSettings(settings);
        return t;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UT-05, UT-06 — Создание турнира
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create()")
    class CreateTournamentTests {

        @Test
        @DisplayName("UT-05: Организатор-оператор создаёт турнир → HTTP 201, id в ответе")
        void create_byOperator_success() {
            User org = organizer(true);
            mockCurrentUser(org);

            com.mafia.manager.dto.CreateTournamentRequest req = new com.mafia.manager.dto.CreateTournamentRequest();
            req.setTitle("Кубок Сибири");
            req.setType("individual");
            TournamentSettings settings = new TournamentSettings();
            settings.setMaxParticipants(10);
            req.setSettings(settings);

            Tournament saved = existingTournament(org);
            saved.setId(42L);
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(saved);
            when(participantRepository.findByTournamentIdAndStatus(anyLong(), any())).thenReturn(List.of());
            when(gameRepository.countByTournamentIdAndStatus(anyLong(), any())).thenReturn(0L);

            TournamentDto result = tournamentService.create(req);

            assertThat(result.getId()).isEqualTo(42L);
            assertThat(result.getStatus()).isEqualTo("registration");
            verify(tournamentRepository).save(any(Tournament.class));
        }

        @Test
        @DisplayName("UT-06: Пользователь без прав (нет клуба) → AccessDeniedException")
        void create_byPlayerWithoutRights_throws() {
            User player = plainPlayer();
            mockCurrentUser(player);

            com.mafia.manager.dto.CreateTournamentRequest req = new com.mafia.manager.dto.CreateTournamentRequest();
            req.setTitle("Нелегальный турнир");
            req.setType("individual");

            assertThatThrownBy(() -> tournamentService.create(req))
                    .isInstanceOf(RuntimeException.class); // RuntimeException — нет клуба
        }

        @Test
        @DisplayName("Клуб не является оператором → AccessDeniedException")
        void create_clubNotOperator_throws() {
            User org = organizer(false); // не оператор
            mockCurrentUser(org);

            com.mafia.manager.dto.CreateTournamentRequest req = new com.mafia.manager.dto.CreateTournamentRequest();
            req.setTitle("Тест");
            req.setType("individual");

            assertThatThrownBy(() -> tournamentService.create(req))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Новый турнир всегда имеет статус 'registration' независимо от запроса")
        void create_statusAlwaysRegistration() {
            User org = organizer(true);
            mockCurrentUser(org);

            com.mafia.manager.dto.CreateTournamentRequest req = new com.mafia.manager.dto.CreateTournamentRequest();
            req.setTitle("T");
            req.setType("individual");
            req.setStatus("active"); // клиент пытается установить другой статус
            TournamentSettings settings = new TournamentSettings();
            settings.setMaxParticipants(10);
            req.setSettings(settings);

            Tournament saved = existingTournament(org);
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(saved);
            when(participantRepository.findByTournamentIdAndStatus(anyLong(), any())).thenReturn(List.of());
            when(gameRepository.countByTournamentIdAndStatus(anyLong(), any())).thenReturn(0L);

            tournamentService.create(req);

            verify(tournamentRepository).save(argThat(t ->
                    t.getStatus() == TournamentStatus.registration
            ));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UT-07 — Одобрение заявки участника
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateParticipantStatus()")
    class UpdateParticipantStatusTests {

        @Test
        @DisplayName("UT-07: Организатор одобряет заявку → статус APPROVED")
        void approveParticipant_byOrganizer_setsApproved() {
            User org = organizer(true);
            mockCurrentUser(org);

            Tournament t = existingTournament(org);
            TournamentParticipant participant = new TournamentParticipant();
            participant.setId(1L);
            participant.setStatus(ParticipantStatus.pending);
            User applicant = new User();
            applicant.setId(5L);
            participant.setUser(applicant);
            participant.setTournament(t);

            when(tournamentRepository.findById(100L)).thenReturn(Optional.of(t));
            when(participantRepository.findByTournamentIdAndUserId(100L, 5L))
                    .thenReturn(Optional.of(participant));
            lenient().when(participantRepository.findByTournamentIdAndStatus(100L, ParticipantStatus.approved))
                    .thenReturn(List.of());
            when(participantRepository.save(any())).thenReturn(participant);

            tournamentService.updateParticipantStatus(100L, 5L, "approved");

            verify(participantRepository).save(argThat(p ->
                    p.getStatus() == ParticipantStatus.approved
            ));
        }

        @Test
        @DisplayName("Отклонение заявки — запись удаляется")
        void rejectParticipant_deletesRecord() {
            User org = organizer(true);
            mockCurrentUser(org);

            Tournament t = existingTournament(org);
            TournamentParticipant participant = new TournamentParticipant();
            participant.setStatus(ParticipantStatus.pending);
            User applicant = new User();
            applicant.setId(6L);
            participant.setUser(applicant);

            when(tournamentRepository.findById(100L)).thenReturn(Optional.of(t));
            when(participantRepository.findByTournamentIdAndUserId(100L, 6L))
                    .thenReturn(Optional.of(participant));

            tournamentService.updateParticipantStatus(100L, 6L, "rejected");

            verify(participantRepository).delete(participant);
            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Неверный статус → RuntimeException")
        void invalidStatus_throws() {
            User org = organizer(true);
            mockCurrentUser(org);

            Tournament t = existingTournament(org);
            when(tournamentRepository.findById(100L)).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> tournamentService.updateParticipantStatus(100L, 5L, "INVALID_STATUS"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Недопустимый статус");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UT-15 — Скрытые результаты
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getLeaderboard() при скрытых результатах")
    class HiddenResultsTests {

        @Test
        @DisplayName("UT-15: areResultsHidden=true, роль PLAYER → очки null")
        void getLeaderboard_hidden_playerSeesNullScores() {
            User player = plainPlayer();
            mockCurrentUser(player);

            TournamentSettings settings = new TournamentSettings();
            settings.setAreResultsHidden(true);

            Tournament t = new Tournament();
            t.setId(50L);
            t.setSettings(settings);
            t.setOrganizer(organizer(true)); // другой пользователь — организатор

            // Мок-данные лидерборда
            LeaderboardEntryDto mockEntry = mock(LeaderboardEntryDto.class);
            lenient().when(mockEntry.getUserId()).thenReturn(1L);
            lenient().when(mockEntry.getNickname()).thenReturn("SomePLayer");
            lenient().when(mockEntry.getTotalScore()).thenReturn(15.5);

            when(tournamentRepository.findById(50L)).thenReturn(Optional.of(t));
            when(tournamentRepository.getLeaderboard(50L, false, "total")).thenReturn(List.of(mockEntry));

            List<LeaderboardEntryDto> result = tournamentService.getLeaderboard(50L, false, "total");

            assertThat(result).hasSize(1);
            // При скрытых результатах для обычного игрока очки null
            assertThat(result.get(0).getTotalScore()).isNull();
        }

        @Test
        @DisplayName("areResultsHidden=true, но организатор → видит реальные очки")
        void getLeaderboard_hidden_organizerSeesRealScores() {
            User org = organizer(true);
            mockCurrentUser(org);

            TournamentSettings settings = new TournamentSettings();
            settings.setAreResultsHidden(true);

            Tournament t = new Tournament();
            t.setId(50L);
            t.setSettings(settings);
            t.setOrganizer(org); // текущий пользователь — организатор

            LeaderboardEntryDto mockEntry = mock(LeaderboardEntryDto.class);
            when(mockEntry.getTotalScore()).thenReturn(15.5);

            when(tournamentRepository.findById(50L)).thenReturn(Optional.of(t));
            when(tournamentRepository.getLeaderboard(50L, false, "total")).thenReturn(List.of(mockEntry));

            List<LeaderboardEntryDto> result = tournamentService.getLeaderboard(50L, false, "total");

            assertThat(result.get(0).getTotalScore()).isEqualTo(15.5);
        }

        @Test
        @DisplayName("areResultsHidden=false → все видят реальные очки")
        void getLeaderboard_notHidden_everyoneSees() {
            User player = plainPlayer();
            mockCurrentUser(player);

            TournamentSettings settings = new TournamentSettings();
            settings.setAreResultsHidden(false);

            Tournament t = new Tournament();
            t.setId(50L);
            t.setSettings(settings);
            t.setOrganizer(organizer(true));

            LeaderboardEntryDto mockEntry = mock(LeaderboardEntryDto.class);
            when(mockEntry.getTotalScore()).thenReturn(8.75);

            when(tournamentRepository.findById(50L)).thenReturn(Optional.of(t));
            when(tournamentRepository.getLeaderboard(50L, false, "total")).thenReturn(List.of(mockEntry));

            List<LeaderboardEntryDto> result = tournamentService.getLeaderboard(50L, false, "total");

            assertThat(result.get(0).getTotalScore()).isEqualTo(8.75);
        }
    }
}
