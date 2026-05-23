package com.mafia.manager.service;

import com.mafia.manager.dto.AssignTeamRequest;
import com.mafia.manager.dto.CreateTeamRequest;
import com.mafia.manager.dto.TeamDto;
import com.mafia.manager.entity.Tournament;
import com.mafia.manager.entity.TournamentParticipant;
import com.mafia.manager.entity.TournamentTeam;
import com.mafia.manager.entity.enums.TournamentType;
import com.mafia.manager.repository.TournamentParticipantRepository;
import com.mafia.manager.repository.TournamentRepository;
import com.mafia.manager.repository.TournamentTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Управление командами турнира (тип = team).
 *
 * Правила:
 * - Нельзя создать больше команд чем maxParticipants / teamSize
 * - При удалении команды — участники открепляются (team = null)
 * - При назначении участника в команду — он открепляется от предыдущей команды
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TournamentTeamRepository       teamRepository;
    private final TournamentRepository           tournamentRepository;
    private final TournamentParticipantRepository participantRepository;

    // ── ЧТЕНИЕ ────────────────────────────────────────────────────────────────

    public List<TeamDto> getTeams(Long tournamentId) {
        return teamRepository.findByTournamentId(tournamentId).stream()
                .map(t -> mapToDto(t, tournamentId))
                .collect(Collectors.toList());
    }

    // ── СОЗДАНИЕ ──────────────────────────────────────────────────────────────

    @Transactional
    public TeamDto createTeam(Long tournamentId, CreateTeamRequest request) {
        Tournament tournament = loadTeamTournament(tournamentId);
        validateTeamLimit(tournament, tournamentId);

        TournamentTeam team = new TournamentTeam();
        team.setTournament(tournament);
        team.setName(request.getName() != null && !request.getName().isBlank()
                ? request.getName()
                : "Команда " + (teamRepository.countByTournamentId(tournamentId) + 1));

        team = teamRepository.save(team);
        return mapToDto(team, tournamentId);
    }

    // ── ПЕРЕИМЕНОВАНИЕ ────────────────────────────────────────────────────────

    @Transactional
    public TeamDto renameTeam(Long tournamentId, Long teamId, String newName) {
        TournamentTeam team = loadTeam(teamId, tournamentId);
        team.setName(newName);
        return mapToDto(teamRepository.save(team), tournamentId);
    }

    // ── УДАЛЕНИЕ КОМАНДЫ ──────────────────────────────────────────────────────

    @Transactional
    public void deleteTeam(Long tournamentId, Long teamId) {
        TournamentTeam team = loadTeam(teamId, tournamentId);

        // Открепляем всех участников
        List<TournamentParticipant> members = participantRepository.findByTeamId(teamId);
        members.forEach(p -> p.setTeam(null));
        participantRepository.saveAll(members);

        teamRepository.delete(team);
    }

    // ── НАЗНАЧЕНИЕ СОСТАВА ────────────────────────────────────────────────────

    /**
     * Полностью заменяет состав команды.
     * Прежние участники команды открепляются, новые — прикрепляются.
     */
    @Transactional
    public TeamDto assignMembers(Long tournamentId, Long teamId, AssignTeamRequest request) {
        Tournament tournament = loadTeamTournament(tournamentId);
        TournamentTeam team   = loadTeam(teamId, tournamentId);

        Integer teamSize = tournament.getSettings() != null
                ? tournament.getSettings().getTeamSize()
                : null;

        List<Long> newMemberIds = request.getMemberIds() == null ? List.of() : request.getMemberIds();

        if (teamSize != null && newMemberIds.size() > teamSize) {
            throw new IllegalArgumentException(
                    "Команда вмещает " + teamSize + " участников, передано: " + newMemberIds.size()
            );
        }

        // Открепляем текущих членов этой команды
        List<TournamentParticipant> oldMembers = participantRepository.findByTeamId(teamId);
        oldMembers.forEach(p -> p.setTeam(null));
        participantRepository.saveAll(oldMembers);

        // Прикрепляем новых (они могут быть из другой команды — убираем их оттуда)
        for (Long userId : newMemberIds) {
            TournamentParticipant participant = participantRepository
                    .findByTournamentIdAndUserId(tournamentId, userId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Участник " + userId + " не найден в турнире"));
            participant.setTeam(team);
            participantRepository.save(participant);
        }

        return mapToDto(team, tournamentId);
    }

    // ── ОТКРЕПИТЬ УЧАСТНИКА ───────────────────────────────────────────────────

    @Transactional
    public void removeMemberFromTeam(Long tournamentId, Long userId) {
        TournamentParticipant participant = participantRepository
                .findByTournamentIdAndUserId(tournamentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Участник не найден"));
        participant.setTeam(null);
        participantRepository.save(participant);
    }

    // ── ГЕНЕРАЦИЯ НАЗВАНИЙ ────────────────────────────────────────────────────

    /**
     * Возвращает N случайных уникальных названий вида "прилагательное + существительное".
     * Используется клиентом для автозаполнения имён команд.
     */
    public List<String> generateTeamNames(int count) {
        String[] adjectives = {
            "Розовые", "Синие", "Жёлтые", "Зелёные", "Красные", "Серые",
            "Белые", "Чёрные", "Золотые", "Серебряные", "Фиолетовые",
            "Оранжевые", "Бирюзовые", "Алые", "Янтарные", "Изумрудные",
            "Дикие", "Тихие", "Быстрые", "Хитрые", "Смелые", "Ленивые",
            "Грозные", "Могучие", "Ловкие", "Стремительные", "Сонные",
        };
        String[] nouns = {
            "Анчоусы", "Медведи", "Лисы", "Волки", "Орлы", "Акулы",
            "Крокодилы", "Пингвины", "Тигры", "Пантеры", "Дельфины",
            "Соколы", "Барсуки", "Гепарды", "Кабаны", "Павлины",
            "Осьминоги", "Фламинго", "Коршуны", "Носороги", "Хомяки",
            "Скорпионы", "Сурикаты", "Ламантины", "Альпаки", "Кенгуру",
        };

        List<String> all = new java.util.ArrayList<>();
        for (String adj : adjectives) {
            for (String noun : nouns) {
                all.add(adj + " " + noun);
            }
        }

        java.util.Collections.shuffle(all);
        return all.stream().limit(count).collect(Collectors.toList());
    }

    // ── ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ────────────────────────────────────────────────

    private Tournament loadTeamTournament(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Турнир не найден"));
        if (t.getType() != TournamentType.team) {
            throw new IllegalArgumentException("Команды доступны только для командных турниров");
        }
        return t;
    }

    private TournamentTeam loadTeam(Long teamId, Long tournamentId) {
        TournamentTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Команда не найдена"));
        if (!team.getTournament().getId().equals(tournamentId)) {
            throw new AccessDeniedException("Команда принадлежит другому турниру");
        }
        return team;
    }

    private void validateTeamLimit(Tournament tournament, Long tournamentId) {
        if (tournament.getSettings() == null) return;

        Integer maxParticipants = tournament.getSettings().getMaxParticipants();
        Integer teamSize        = tournament.getSettings().getTeamSize();

        if (maxParticipants == null || teamSize == null || teamSize <= 0) return;

        int maxTeams    = maxParticipants / teamSize;
        long currentTeams = teamRepository.countByTournamentId(tournamentId);

        if (currentTeams >= maxTeams) {
            throw new IllegalArgumentException(
                    "Максимальное количество команд (" + maxTeams + ") уже достигнуто"
            );
        }
    }

    private TeamDto mapToDto(TournamentTeam team, Long tournamentId) {
        List<TournamentParticipant> members = participantRepository.findByTeamId(team.getId());
        return TeamDto.builder()
                .id(team.getId())
                .tournamentId(tournamentId)
                .name(team.getName())
                .memberIds(members.stream().map(p -> p.getUser().getId()).collect(Collectors.toList()))
                .memberNicknames(members.stream().map(p -> p.getUser().getNickname()).collect(Collectors.toList()))
                .build();
    }
}
