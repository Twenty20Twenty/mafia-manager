package com.mafia.manager.service;

import com.mafia.manager.dto.LeaderboardEntryDto;
import com.mafia.manager.dto.PlayerTournamentDto;
import com.mafia.manager.dto.TeamLeaderboardEntryDto;
import com.mafia.manager.entity.Tournament;
import com.mafia.manager.entity.TournamentParticipant;
import com.mafia.manager.entity.enums.ParticipantStatus;
import com.mafia.manager.entity.enums.TournamentStatus;
import com.mafia.manager.entity.enums.TournamentType;
import com.mafia.manager.repository.TournamentParticipantRepository;
import com.mafia.manager.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Сервис для получения истории турниров конкретного игрока.
 *
 * <p>Возвращает все турниры, в которых игрок является одобренным участником,
 * с его местом в финальном рейтинге (если турнир завершён и результаты открыты).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTournamentsService {

    private final TournamentParticipantRepository participantRepository;
    private final TournamentRepository            tournamentRepository;

    /**
     * Возвращает список турниров игрока, отсортированных от новых к старым.
     * Завершённые турниры идут после активных (при прочих равных датах).
     *
     * @param userId идентификатор пользователя
     * @return список DTO турниров с позицией игрока
     */
    public List<PlayerTournamentDto> getTournamentsForUser(Long userId) {
        List<TournamentParticipant> participations = participantRepository
                .findByUserIdAndStatus(userId, ParticipantStatus.approved);

        return participations.stream()
                .map(p -> buildDto(p.getTournament(), userId))
                .sorted(Comparator
                        // Завершённые — в конец
                        .<PlayerTournamentDto>comparingInt(dto ->
                                "completed".equals(dto.getStatus()) || "archived".equals(dto.getStatus()) ? 1 : 0)
                        // Внутри группы — от новых к старым
                        .thenComparing(Comparator.comparing(
                                PlayerTournamentDto::getStartDate,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                )
                .collect(Collectors.toList());
    }

    // ── Приватные методы ──────────────────────────────────────────────────────

    private PlayerTournamentDto buildDto(Tournament t, Long userId) {
        boolean isCompleted      = t.getStatus() == TournamentStatus.completed;
        boolean areResultsHidden = t.getSettings() != null
                && Boolean.TRUE.equals(t.getSettings().getAreResultsHidden());
        boolean isSeason         = t.getType() == TournamentType.season;
        boolean isTeam           = t.getType() == TournamentType.team;

        Integer individualRank = null;
        Integer teamRank       = null;

        if (isCompleted && !areResultsHidden && !isSeason) {
            individualRank = resolveIndividualRank(t.getId(), userId);
            if (isTeam) {
                teamRank = resolveTeamRank(t.getId(), userId);
            }
        }

        return PlayerTournamentDto.builder()
                .tournamentId(t.getId())
                .title(t.getTitle())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .clubName(t.getClub() != null ? t.getClub().getName() : null)
                .clubId(t.getClub()   != null ? t.getClub().getId()   : null)
                .individualRank(individualRank)
                .teamRank(teamRank)
                .build();
    }

    /**
     * Определяет место игрока в личном лидерборде (позиция 1-based).
     */
    private Integer resolveIndividualRank(Long tournamentId, Long userId) {
        try {
            List<LeaderboardEntryDto> board = tournamentRepository
                    .getLeaderboard(tournamentId, false, "total");

            return IntStream.range(0, board.size())
                    .filter(i -> userId.equals(board.get(i).getUserId()))
                    .map(i -> i + 1)
                    .boxed()
                    .findFirst()
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Failed to resolve individual rank for user={}, tournament={}", userId, tournamentId, ex);
            return null;
        }
    }

    /**
     * Определяет место команды игрока в командном лидерборде (позиция 1-based).
     */
    private Integer resolveTeamRank(Long tournamentId, Long userId) {
        try {
            Long teamId = participantRepository
                    .findByTournamentIdAndUserId(tournamentId, userId)
                    .map(p -> p.getTeam() != null ? p.getTeam().getId() : null)
                    .orElse(null);

            if (teamId == null) return null;

            List<TeamLeaderboardEntryDto> board = tournamentRepository
                    .getTeamLeaderboard(tournamentId);

            final Long finalTeamId = teamId;
            return IntStream.range(0, board.size())
                    .filter(i -> finalTeamId.equals(board.get(i).getTeamId()))
                    .map(i -> i + 1)
                    .boxed()
                    .findFirst()
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Failed to resolve team rank for user={}, tournament={}", userId, tournamentId, ex);
            return null;
        }
    }
}
