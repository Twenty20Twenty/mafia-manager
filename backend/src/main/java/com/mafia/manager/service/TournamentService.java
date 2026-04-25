package com.mafia.manager.service;

import com.mafia.manager.dto.*;
import com.mafia.manager.entity.*;
import com.mafia.manager.entity.enums.*;
import com.mafia.manager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mafia.manager.entity.enums.TournamentStatus.completed;

/**
 * Сервис управления турнирами.
 *
 * <p>Покрывает полный жизненный цикл турнира:</p>
 * <ul>
 *   <li>CRUD-операции над турнирами</li>
 *   <li>Управление участниками (заявки, одобрение, удаление)</li>
 *   <li>Управление исключениями рассадки</li>
 *   <li>Назначение судей на столы</li>
 *   <li>Формирование лидерборда и номинаций</li>
 *   <li>Фиксация квалифай-топ10 и управление судьями турнира</li>
 * </ul>
 *
 * <p>При переводе турнира в статус {@code completed} автоматически вызывается
 * пересчёт статистики игроков через {@link PlayerStatsService}.</p>
 */
@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository            tournamentRepository;
    private final GameRepository                  gameRepository;
    private final UserRepository                  userRepository;
    private final CityRepository                  cityRepository;
    private final TournamentParticipantRepository participantRepository;
    private final SeedingExceptionRepository      exceptionRepository;
    private final TournamentJudgeRepository       tournamentJudgeRepository;
    private final PlayerStatsService              playerStatsService;

    // ── СПИСОК / ПОЛУЧЕНИЕ ────────────────────────────────────────────────────

    /**
     * Возвращает список турниров с опциональной фильтрацией.
     *
     * @param type   фильтр по типу ({@code null} — без фильтра)
     * @param status фильтр по статусу ({@code null} — без фильтра)
     * @param clubId фильтр по клубу ({@code null} — без фильтра)
     * @param search поиск по части названия ({@code null} — без поиска)
     * @return отфильтрованный список DTO турниров
     */
    public List<TournamentDto> getAll(String type, String status, Long clubId, String search) {
        return tournamentRepository.findAll().stream()
                .filter(t -> type   == null || t.getType().name().equalsIgnoreCase(type))
                .filter(t -> status == null || t.getStatus().name().equalsIgnoreCase(status))
                .filter(t -> clubId == null || (t.getClub() != null && t.getClub().getId().equals(clubId)))
                .filter(t -> search == null || t.getTitle().toLowerCase().contains(search.toLowerCase()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает турнир по идентификатору.
     *
     * @param id идентификатор турнира
     * @return DTO турнира
     * @throws NoSuchElementException если турнир не найден
     */
    public TournamentDto getById(Long id) {
        return mapToDto(tournamentRepository.findById(id).orElseThrow());
    }

    // ── СОЗДАНИЕ ──────────────────────────────────────────────────────────────

    /**
     * Создаёт новый турнир от имени текущего пользователя.
     *
     * <p>Клуб определяется автоматически из профиля текущего пользователя.
     * При создании статус всегда устанавливается в {@code registration},
     * независимо от того, что передал клиент.</p>
     *
     * @param request данные нового турнира
     * @return DTO созданного турнира
     * @throws AccessDeniedException если у пользователя нет права создавать турниры
     * @throws RuntimeException      если пользователь не состоит в клубе
     */
    @Transactional
    public TournamentDto create(CreateTournamentRequest request) {
        User currentUser = getCurrentUser();
        Club club = resolveUserClub(currentUser);

        if (!currentUser.hasRightToCreateTournament()) {
            throw new AccessDeniedException("У вас нет прав для создания турниров");
        }

        Tournament t = buildNewTournament(request, currentUser, club);
        return mapToDto(tournamentRepository.save(t));
    }

    // ── ОБНОВЛЕНИЕ ────────────────────────────────────────────────────────────

    /**
     * Обновляет данные турнира.
     *
     * <p>При переводе статуса в {@code completed} автоматически запускается
     * пересчёт статистики игроков ({@link PlayerStatsService#recalculateForTournament}).</p>
     *
     * @param id      идентификатор турнира
     * @param request новые данные
     * @return DTO обновлённого турнира
     * @throws AccessDeniedException если текущий пользователь не менеджер турнира
     * @throws RuntimeException      если передан недопустимый статус
     */
    @Transactional
    public TournamentDto update(Long id, CreateTournamentRequest request) {
        Tournament t = tournamentRepository.findById(id).orElseThrow();
        checkManagerRights(t);

        triggerStatsRecalcOnCompletion(t, request.getStatus());

        applyBasicFields(t, request);
        applyCity(t, request.getCityId());
        applyHeadJudge(t, request.getHeadJudgeId());
        applyFinalJudge(t, request.getFinalJudgeId());
        applyStatus(t, request.getStatus());

        return mapToDto(tournamentRepository.save(t));
    }

    // ── УЧАСТНИКИ ─────────────────────────────────────────────────────────────

    /**
     * Возвращает список всех участников турнира с их статусами.
     *
     * @param tournamentId идентификатор турнира
     * @return список DTO участников
     */
    public List<UserDto> getParticipants(Long tournamentId) {
        return participantRepository.findByTournamentId(tournamentId).stream()
                .map(p -> mapUserToDto(p.getUser(), p.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Подаёт заявку на участие в турнире от имени текущего пользователя.
     *
     * @param tournamentId идентификатор турнира
     * @throws RuntimeException если заявка уже существует или турнир заполнен
     */
    @Transactional
    public void apply(Long tournamentId) {
        User currentUser = getCurrentUser();
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();

        if (participantRepository.existsByTournamentIdAndUserId(tournamentId, currentUser.getId())) {
            throw new RuntimeException("Вы уже подали заявку или являетесь участником");
        }

        //checkParticipantLimit(t, tournamentId);

        TournamentParticipant p = new TournamentParticipant();
        p.setTournament(t);
        p.setUser(currentUser);
        p.setStatus(ParticipantStatus.pending);
        participantRepository.save(p);
    }

    /**
     * Добавляет участника в турнир напрямую (без заявки), со статусом {@code approved}.
     *
     * <p>Доступно менеджеру турнира.</p>
     *
     * @param tournamentId идентификатор турнира
     * @param userId       идентификатор добавляемого пользователя
     * @throws RuntimeException если игрок уже является участником или турнир заполнен
     */
    @Transactional
    public void addParticipant(Long tournamentId, Long userId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        if (participantRepository.existsByTournamentIdAndUserId(tournamentId, userId)) {
            throw new RuntimeException("Игрок (ID=" + userId + ") уже является участником турнира");
        }

        //checkParticipantLimit(t, tournamentId);

        User user = userRepository.findById(userId).orElseThrow();

        TournamentParticipant p = new TournamentParticipant();
        p.setTournament(t);
        p.setUser(user);
        p.setStatus(ParticipantStatus.approved);
        participantRepository.save(p);
    }

    /**
     * Удаляет участника из турнира.
     *
     * <p>Доступно менеджеру турнира.</p>
     *
     * @param tournamentId идентификатор турнира
     * @param userId       идентификатор удаляемого участника
     */
    @Transactional
    public void removeParticipant(Long tournamentId, Long userId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        participantRepository.findByTournamentIdAndUserId(tournamentId, userId)
                .ifPresent(participantRepository::delete);
    }

    /**
     * Обновляет статус участника турнира.
     *
     * <p>При одобрении ({@code approved}) проверяется лимит участников.
     * При отклонении ({@code rejected}) запись участника удаляется.</p>
     *
     * @param tournamentId идентификатор турнира
     * @param userId       идентификатор участника
     * @param statusStr    новый статус: {@code pending}, {@code approved}, {@code rejected}, {@code kicked}
     * @throws RuntimeException если статус недопустим или заявка не найдена
     */
    @Transactional
    public void updateParticipantStatus(Long tournamentId, Long userId, String statusStr) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        ParticipantStatus newStatus = parseParticipantStatus(statusStr);

        TournamentParticipant participant = participantRepository
                .findByTournamentIdAndUserId(tournamentId, userId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (newStatus == ParticipantStatus.approved) {
            //checkParticipantLimit(t, tournamentId);
            participant.setStatus(newStatus);
            participantRepository.save(participant);
        } else if (newStatus == ParticipantStatus.rejected) {
            participantRepository.delete(participant);
        }
    }

    // ── ИСКЛЮЧЕНИЯ РАССАДКИ ───────────────────────────────────────────────────

    /**
     * Возвращает список исключений рассадки (пар игроков, которые не должны сидеть вместе).
     *
     * @param tournamentId идентификатор турнира
     * @return список DTO исключений
     */
    public List<SeedingExceptionDto> getExceptions(Long tournamentId) {
        return exceptionRepository.findByTournamentId(tournamentId).stream()
                .map(e -> SeedingExceptionDto.builder()
                        .id(e.getId())
                        .tournamentId(tournamentId)
                        .player1Id(e.getPlayer1().getId())
                        .player1Nickname(e.getPlayer1().getNickname())
                        .player2Id(e.getPlayer2().getId())
                        .player2Nickname(e.getPlayer2().getNickname())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Добавляет исключение рассадки между двумя игроками.
     *
     * @param tournamentId идентификатор турнира
     * @param p1Id         идентификатор первого игрока
     * @param p2Id         идентификатор второго игрока
     * @throws AccessDeniedException если текущий пользователь не менеджер турнира
     */
    @Transactional
    public void addException(Long tournamentId, Long p1Id, Long p2Id) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        User p1 = userRepository.findById(p1Id).orElseThrow();
        User p2 = userRepository.findById(p2Id).orElseThrow();

        TournamentSeedingException ex = new TournamentSeedingException();
        ex.setTournament(t);
        ex.setPlayer1(p1);
        ex.setPlayer2(p2);
        exceptionRepository.save(ex);
    }

    /**
     * Удаляет исключение рассадки.
     *
     * @param tournamentId идентификатор турнира
     * @param exceptionId  идентификатор исключения
     * @throws RuntimeException если исключение не найдено или принадлежит другому турниру
     */
    @Transactional
    public void deleteException(Long tournamentId, Long exceptionId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        TournamentSeedingException ex = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new RuntimeException("Исключение не найдено"));

        if (!ex.getTournament().getId().equals(tournamentId)) {
            throw new RuntimeException("Исключение принадлежит другому турниру");
        }

        exceptionRepository.delete(ex);
    }

    // ── СУДЬИ СТОЛОВ ─────────────────────────────────────────────────────────

    /**
     * Возвращает карту назначений судей на столы ({@code "номер_стола" → id_судьи}).
     *
     * @param tournamentId идентификатор турнира
     * @return карта назначений или пустая карта, если назначений нет
     */
    public Map<String, Long> getTableJudges(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();

        if (t.getSettings() == null || t.getSettings().getStaticTableJudges() == null) {
            return Map.of();
        }

        return t.getSettings().getStaticTableJudges();
    }

    /**
     * Обновляет назначения судей на столы.
     *
     * <p>Ключи карты проходят валидацию: должны быть натуральными числами
     * в диапазоне от 1 до {@code maxTables} (рассчитывается из {@code maxParticipants}).</p>
     *
     * @param tournamentId идентификатор турнира
     * @param tableJudges  карта {@code "номер_стола" → id_судьи}
     * @throws RuntimeException если ключи карты некорректны или настройки не инициализированы
     */
    @Transactional
    public void updateTableJudges(Long tournamentId, Map<String, Long> tableJudges) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        if (t.getSettings() == null) {
            throw new RuntimeException("Настройки турнира не инициализированы");
        }

        validateTableJudgesMap(tableJudges, t);
        t.getSettings().setStaticTableJudges(tableJudges);
        tournamentRepository.save(t);
    }

    // ── ЛИДЕРБОРДЫ И НОМИНАЦИИ ────────────────────────────────────────────────

    /**
     * Возвращает лидерборд турнира.
     *
     * <p>Если в настройках включён флаг {@code areResultsHidden} и текущий пользователь
     * не является менеджером, возвращаются записи со скрытыми очками.</p>
     *
     * @param id            идентификатор турнира
     * @param includeFinals учитывать ли финальные игры (с коэффициентом)
     * @param sortBy        режим сортировки: {@code "avg"} — по среднему, иначе — по сумме
     * @return список DTO лидерборда
     */
    public List<LeaderboardEntryDto> getLeaderboard(Long id, boolean includeFinals, String sortBy) {
        Tournament t = tournamentRepository.findById(id).orElseThrow();
        String resolvedSortBy = "avg".equals(sortBy) ? "avg" : "total";

        List<LeaderboardEntryDto> leaderboard = tournamentRepository.getLeaderboard(id, includeFinals, resolvedSortBy);

        if (areResultsHidden(t) && !isCurrentUserManager(t)) {
            return leaderboard.stream()
                    .map(LeaderboardEntryDto::withHiddenScores)
                    .collect(Collectors.toList());
        }

        return leaderboard;
    }

    /**
     * Возвращает командный лидерборд турнира.
     *
     * <p>При скрытых результатах очки скрываются так же, как в обычном лидерборде.</p>
     *
     * @param id идентификатор турнира
     * @return список DTO командного лидерборда
     */
    public List<TeamLeaderboardEntryDto> getTeamLeaderboard(Long id) {
        Tournament t = tournamentRepository.findById(id).orElseThrow();
        List<TeamLeaderboardEntryDto> leaderboard = tournamentRepository.getTeamLeaderboard(id);

        if (areResultsHidden(t) && !isCurrentUserManager(t)) {
            return leaderboard.stream()
                    .map(TeamLeaderboardEntryDto::withHiddenScores)
                    .collect(Collectors.toList());
        }

        return leaderboard;
    }

    /**
     * Возвращает номинационный рейтинг участников турнира.
     *
     * @param id       идентификатор турнира
     * @param calcMode режим расчёта номинаций
     * @return список проекций номинаций
     */
    public List<NominationDto> getNominations(Long id, String calcMode) {
        return tournamentRepository.getNominations(id, calcMode);
    }

    // ── ФИНАЛ: ФИКСАЦИЯ КВАЛИФАЙЕРОВ ─────────────────────────────────────────

    /**
     * Фиксирует топ-10 игроков по итогам отборочных туров.
     *
     * <p>ID игроков сохраняются в {@code settings.top10PlayerIds},
     * флаг {@code areQualifiersFixed} устанавливается в {@code true}.
     * После этого можно генерировать финальный тур.</p>
     *
     * @param tournamentId идентификатор турнира
     * @throws RuntimeException если в лидерборде менее 10 участников
     *                          или настройки не инициализированы
     */
    @Transactional
    public void fixQualifiers(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        List<LeaderboardEntryDto> leaderboard = tournamentRepository.getLeaderboard(tournamentId, false, "total");

        List<Long> top10 = leaderboard.stream()
                .limit(10)
                .map(LeaderboardEntryDto::getUserId)
                .collect(Collectors.toList());

        if (top10.size() < 10) {
            throw new RuntimeException(
                    "Недостаточно данных: в лидерборде менее 10 участников (найдено: " + top10.size() + ")"
            );
        }

        if (t.getSettings() == null) {
            throw new RuntimeException("Настройки турнира не инициализированы");
        }

        t.getSettings().setTop10PlayerIds(top10);
        t.getSettings().setAreQualifiersFixed(true);
        tournamentRepository.save(t);
    }

    // ── СУДЬИ ТУРНИРА ─────────────────────────────────────────────────────────

    /**
     * Возвращает список ID судей, аккредитованных на турнире.
     *
     * @param tournamentId идентификатор турнира
     * @return список ID пользователей-судей
     */
    public List<Long> getTournamentJudges(Long tournamentId) {
        return tournamentJudgeRepository.findUserIdsByTournamentId(tournamentId);
    }

    /**
     * Полностью заменяет список аккредитованных судей турнира.
     *
     * <p>Текущий список удаляется, затем создаётся новый из переданных ID.
     * Передача {@code null} или пустого списка снимает всех судей.</p>
     *
     * @param tournamentId идентификатор турнира
     * @param judgeIds     список ID новых судей (nullable)
     */
    @Transactional
    public void updateTournamentJudges(Long tournamentId, List<Long> judgeIds) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        tournamentJudgeRepository.deleteByTournamentId(tournamentId);

        if (judgeIds == null || judgeIds.isEmpty()) return;

        List<TournamentJudge> judges = judgeIds.stream()
                .map(uid -> {
                    User user = userRepository.findById(uid)
                            .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + uid));
                    TournamentJudge tj = new TournamentJudge();
                    tj.setTournament(t);
                    tj.setUser(user);
                    return tj;
                })
                .collect(Collectors.toList());

        tournamentJudgeRepository.saveAll(judges);
    }

    // ── ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ────────────────────────────────────────────────

    /**
     * Собирает сущность нового турнира из запроса.
     */
    private Tournament buildNewTournament(CreateTournamentRequest request, User organizer, Club club) {
        Tournament t = new Tournament();
        t.setTitle(request.getTitle());
        t.setDescription(request.getDescription());
        t.setClub(club);
        t.setOrganizer(organizer);
        t.setType(TournamentType.valueOf(request.getType()));
        t.setStartDate(request.getStartDate());
        t.setEndDate(request.getEndDate());
        t.setSettings(request.getSettings());
        t.setStatus(TournamentStatus.registration);

        if (request.getCityId() != null) {
            cityRepository.findById(Math.toIntExact(request.getCityId())).ifPresent(t::setCity);
        }
        if (request.getFinalJudgeId() != null && t.getSettings() != null) {
            t.getSettings().setFinalJudgeId(request.getFinalJudgeId());
        }

        return t;
    }

    /** Применяет базовые текстовые поля турнира из запроса. */
    private void applyBasicFields(Tournament t, CreateTournamentRequest request) {
        t.setTitle(request.getTitle());
        t.setDescription(request.getDescription());
        t.setSettings(request.getSettings());

        if (request.getStartDate() != null) t.setStartDate(request.getStartDate());
        if (request.getEndDate()   != null) t.setEndDate(request.getEndDate());
    }

    /** Применяет город к турниру. При {@code null} — сбрасывает значение. */
    private void applyCity(Tournament t, Long cityId) {
        if (cityId != null) {
            cityRepository.findById(Math.toIntExact(cityId)).ifPresent(t::setCity);
        } else {
            t.setCity(null);
        }
    }

    /**
     * Назначает главного судью турнира.
     * При {@code null} или {@code 0} — снимает ГС.
     */
    private void applyHeadJudge(Tournament t, Long headJudgeId) {
        if (headJudgeId == null || headJudgeId == 0) {
            t.setHeadJudge(null);
        } else {
            User hj = userRepository.findById(headJudgeId)
                    .orElseThrow(() -> new RuntimeException("Судья не найден: " + headJudgeId));
            t.setHeadJudge(hj);
        }
    }

    /** Сохраняет ID финального судьи в JSONB-настройках турнира. */
    private void applyFinalJudge(Tournament t, Long finalJudgeId) {
        if (t.getSettings() != null) {
            t.getSettings().setFinalJudgeId(finalJudgeId);
        }
    }

    /**
     * Применяет новый статус турнира.
     * Игнорирует пустое/null значение.
     *
     * @throws RuntimeException при передаче недопустимого строкового значения
     */
    private void applyStatus(Tournament t, String status) {
        if (status == null || status.isBlank()) return;

        try {
            t.setStatus(TournamentStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Недопустимый статус: " + status
                    + ". Допустимые: registration, active, completed, archived"
            );
        }
    }

    /**
     * Запускает пересчёт статистики, если турнир переводится в статус {@code completed}
     * и ещё не имел этого статуса.
     */
    private void triggerStatsRecalcOnCompletion(Tournament t, String newStatus) {
        if (t.getStatus() != completed && Objects.equals(newStatus, "completed")) {
            playerStatsService.recalculateForTournament(t.getId());
        }
    }

    /**
     * Проверяет, не превышен ли лимит участников турнира.
     * Если {@code maxParticipants} не задан — ограничение отсутствует.
     *
     * @throws RuntimeException если турнир заполнен
     */
    private void checkParticipantLimit(Tournament t, Long tournamentId) {
        Integer maxParticipants = t.getSettings() != null ? t.getSettings().getMaxParticipants() : null;
        if (maxParticipants == null) return;

        long approvedCount = participantRepository
                .findByTournamentIdAndStatus(tournamentId, ParticipantStatus.approved)
                .size();

        if (approvedCount >= maxParticipants) {
            throw new RuntimeException(
                    "Турнир заполнен: максимальное количество участников (" + maxParticipants + ") достигнуто"
            );
        }
    }

    /**
     * Валидирует карту назначений судей на столы.
     *
     * <p>Ключи должны быть натуральными числами от 1 до {@code maxTables}.
     * Значения (ID судей) должны быть положительными или null («снять судью»).</p>
     */
    private void validateTableJudgesMap(Map<String, Long> tableJudges, Tournament t) {
        if (tableJudges == null || tableJudges.isEmpty()) return;

        int maxParticipants = t.getSettings().getMaxParticipants() != null
                ? t.getSettings().getMaxParticipants() : 100;
        int maxTables = Math.max(1, maxParticipants / 10);

        for (Map.Entry<String, Long> entry : tableJudges.entrySet()) {
            String key = entry.getKey();
            try {
                int tableNum = Integer.parseInt(key);
                if (tableNum < 1 || tableNum > maxTables) {
                    throw new RuntimeException("Некорректный номер стола: " + key + ". Допустимый диапазон: 1–" + maxTables);
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Ключ карты судей должен быть числом, получено: '" + key + "'");
            }
            if (entry.getValue() != null && entry.getValue() <= 0) {
                throw new RuntimeException("ID судьи должен быть положительным числом, стол: " + key);
            }
        }
    }

    /** Разбирает строку статуса участника, выбрасывает исключение при недопустимом значении. */
    private ParticipantStatus parseParticipantStatus(String statusStr) {
        try {
            return ParticipantStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Недопустимый статус: " + statusStr
                    + ". Допустимые: pending, approved, rejected, kicked"
            );
        }
    }

    /**
     * Проверяет, что текущий пользователь является менеджером турнира
     * (организатором, ГС или администратором).
     *
     * @throws AccessDeniedException если права отсутствуют
     */
    private void checkManagerRights(Tournament t) {
        User cu = getCurrentUser();
        if (cu.getRole() == UserRole.admin) return;

        boolean isOrganizer = t.getOrganizer() != null && t.getOrganizer().getId().equals(cu.getId());
        boolean isHeadJudge = t.getHeadJudge()  != null && t.getHeadJudge().getId().equals(cu.getId());

        if (!isOrganizer && !isHeadJudge) {
            throw new AccessDeniedException("Недостаточно прав для управления турниром");
        }
    }

    /**
     * Проверяет, является ли текущий пользователь менеджером турнира.
     * Не выбрасывает исключений — используется для условной логики (напр., скрытие очков).
     */
    private boolean isCurrentUserManager(Tournament t) {
        try {
            String nick = SecurityContextHolder.getContext().getAuthentication().getName();
            if (nick == null || "anonymousUser".equals(nick)) return false;

            User cu = userRepository.findByNickname(nick).orElse(null);
            if (cu == null) return false;
            if (cu.getRole() == UserRole.admin) return true;

            boolean isOrganizer = t.getOrganizer() != null && t.getOrganizer().getId().equals(cu.getId());
            boolean isHeadJudge = t.getHeadJudge()  != null && t.getHeadJudge().getId().equals(cu.getId());

            return isOrganizer || isHeadJudge;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Возвращает клуб текущего пользователя.
     *
     * @throws RuntimeException если пользователь не состоит ни в одном клубе
     */
    private Club resolveUserClub(User user) {
        if (user.getClub() == null) {
            throw new RuntimeException("Вы не состоите ни в одном клубе. Вступите в клуб, чтобы создавать турниры.");
        }
        return user.getClub();
    }

    /**
     * Возвращает {@code true}, если флаг скрытия результатов в настройках турнира включён.
     */
    private boolean areResultsHidden(Tournament t) {
        return t.getSettings() != null && Boolean.TRUE.equals(t.getSettings().getAreResultsHidden());
    }

    /** Извлекает текущего аутентифицированного пользователя из Security Context. */
    private User getCurrentUser() {
        String nick = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByNickname(nick).orElseThrow();
    }

    // Добавить/изменить в TournamentService.java

// ── ФИНАЛИСТЫ (замена fixQualifiers) ─────────────────────────────────────────

    /**
     * Устанавливает список финалистов вручную.
     *
     * <p>Принимает до 10 ID игроков. Если {@code lock=true} — дополнительно
     * фиксирует список (устанавливает {@code areQualifiersFixed=true}).</p>
     *
     * @throws RuntimeException если передано более 10 игроков
     *                          или игрок не является участником турнира
     */
    @Transactional
    public void setFinalists(Long tournamentId, SetFinalistsRequest req) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        List<Long> ids = req.getPlayerIds() == null ? List.of() : req.getPlayerIds();

        if (ids.size() > 10) {
            throw new RuntimeException("Финалистов не может быть более 10, передано: " + ids.size());
        }

        validateFinalistsAreParticipants(tournamentId, ids);

        if (t.getSettings() == null) {
            throw new RuntimeException("Настройки турнира не инициализированы");
        }

        t.getSettings().setTop10PlayerIds(ids);

        if (req.isLock()) {
            t.getSettings().setAreQualifiersFixed(true);
        }

        tournamentRepository.save(t);
    }

    /**
     * Возвращает текущий список финалистов и флаг фиксации.
     */
    public FinalistsDto getFinalists(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();

        if (t.getSettings() != null) {
            return FinalistsDto.builder()
                    .locked(Boolean.TRUE.equals(t.getSettings().getAreQualifiersFixed()))
                    .playerIds(t.getSettings().getTop10PlayerIds() != null
                            ? t.getSettings().getTop10PlayerIds()
                            : List.of())
                    .build();
        } else {
            return FinalistsDto.builder()
                    .locked(false)
                    .playerIds(List.of())
                    .build();
        }
    }

    /**
     * Снимает фиксацию финалистов, позволяя снова изменять список.
     */
    @Transactional
    public void unlockFinalists(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        if (t.getSettings() != null) {
            t.getSettings().setAreQualifiersFixed(false);
            tournamentRepository.save(t);
        }
    }

    /**
     * Автоматически заполняет финалистов из топ лидерборда (аналог старого fixQualifiers).
     * Сохраняет, но не фиксирует — можно потом скорректировать вручную.
     */
    @Transactional
    public void autoFillFinalists(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        checkManagerRights(t);

        List<LeaderboardEntryDto> leaderboard = tournamentRepository.getLeaderboard(tournamentId, false, "total");
        List<Long> top = leaderboard.stream()
                .limit(10)
                .map(LeaderboardEntryDto::getUserId)
                .collect(Collectors.toList());

        if (t.getSettings() == null) {
            throw new RuntimeException("Настройки турнира не инициализированы");
        }

        t.getSettings().setTop10PlayerIds(top);
        // Не фиксируем — организатор может скорректировать вручную
        tournamentRepository.save(t);
    }

    /** Проверяет, что все переданные ID являются одобренными участниками турнира. */
    private void validateFinalistsAreParticipants(Long tournamentId, List<Long> ids) {
        if (ids.isEmpty()) return;

        Set<Long> approvedIds = participantRepository
                .findByTournamentIdAndStatus(tournamentId, ParticipantStatus.approved)
                .stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());

        List<Long> notFound = ids.stream()
                .filter(id -> !approvedIds.contains(id))
                .collect(Collectors.toList());

        if (!notFound.isEmpty()) {
            throw new RuntimeException(
                    "Следующие игроки не являются участниками турнира: " + notFound);
        }
    }

    // ── МАППИНГ ───────────────────────────────────────────────────────────────

    /** Преобразует сущность турнира в полный DTO. */
    private TournamentDto mapToDto(Tournament t) {
        return TournamentDto.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus().name())
                .type(t.getType().name())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .clubId(t.getClub() != null ? t.getClub().getId() : null)
                .clubName(t.getClub() != null ? t.getClub().getName() : null)
                .cityId(t.getCity() != null ? t.getCity().getId() : null)
                .cityName(t.getCity() != null ? t.getCity().getName() : null)
                .settings(t.getSettings())
                .organizerId(t.getOrganizer() != null ? t.getOrganizer().getId() : null)
                .organizerName(t.getOrganizer() != null ? t.getOrganizer().getNickname() : null)
                .organizerAvatar(t.getOrganizer() != null ? t.getOrganizer().getAvatarUrl() : null)
                .headJudgeId(t.getHeadJudge() != null ? t.getHeadJudge().getId() : null)
                .headJudgeName(t.getHeadJudge() != null ? t.getHeadJudge().getNickname() : null)
                .headJudgeAvatar(t.getHeadJudge() != null ? t.getHeadJudge().getAvatarUrl() : null)
                .participantsCount(
                        participantRepository.findByTournamentIdAndStatus(t.getId(), ParticipantStatus.approved).size()
                )
                .completedGamesCount(
                        (int) gameRepository.countByTournamentIdAndStatus(t.getId(), GameStatus.completed)
                )
                .build();
    }

    /** Преобразует пользователя в DTO участника с его статусом в турнире. */
    private UserDto mapUserToDto(User u, ParticipantStatus status) {
        return UserDto.builder()
                .id(u.getId())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .clubId(u.getClub()   != null ? u.getClub().getId()   : null)
                .clubName(u.getClub() != null ? u.getClub().getName() : null)
                .status(status != null ? status.name() : null)
                .build();
    }
}
