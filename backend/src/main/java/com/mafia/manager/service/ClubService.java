package com.mafia.manager.service;

import com.mafia.manager.dto.ClubDto;
import com.mafia.manager.dto.CreateClubRequest;
import com.mafia.manager.dto.UserDto;
import com.mafia.manager.entity.Club;
import com.mafia.manager.entity.ClubRequest;
import com.mafia.manager.entity.User;
import com.mafia.manager.entity.enums.UserRole;
import com.mafia.manager.repository.CityRepository;
import com.mafia.manager.repository.ClubRepository;
import com.mafia.manager.repository.ClubRequestRepository;
import com.mafia.manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис управления клубами.
 *
 * <p>Охватывает полный жизненный цикл клуба:</p>
 * <ul>
 *   <li>CRUD-операции над клубами</li>
 *   <li>Управление членством: заявки, одобрение, исключение, выход</li>
 *   <li>Управление правами: статус «Турнирного оператора» и право создавать турниры</li>
 * </ul>
 *
 * <p>Все изменяющие операции требуют прав президента клуба или администратора.</p>
 */
@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository        clubRepository;
    private final UserRepository        userRepository;
    private final ClubRequestRepository clubRequestRepository;
    private final CityRepository        cityRepository;

    // ── ЧТЕНИЕ ────────────────────────────────────────────────────────────────

    /**
     * Возвращает список всех клубов без состава участников.
     *
     * @return список DTO всех клубов
     */
    public List<ClubDto> getAllClubs() {
        return clubRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает полные данные клуба, включая список участников.
     *
     * @param id идентификатор клуба
     * @return DTO клуба с полем {@code members}
     * @throws RuntimeException если клуб не найден
     */
    public ClubDto getClubById(Long id) {
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Клуб не найден"));

        ClubDto dto = mapToDto(club);
        List<User> members = userRepository.findByClubId(id);
        dto.setMembers(members.stream().map(this::mapUserToDto).collect(Collectors.toList()));
        return dto;
    }

    // ── СОЗДАНИЕ / ОБНОВЛЕНИЕ ─────────────────────────────────────────────────

    /**
     * Создаёт новый клуб от имени текущего пользователя.
     *
     * <p>Создатель автоматически становится президентом клуба
     * и добавляется в него как участник.</p>
     *
     * @param request данные нового клуба (name, description, socialLink, city)
     * @return DTO созданного клуба
     * @throws RuntimeException если пользователь уже состоит в другом клубе
     */
    @Transactional
    public ClubDto createClub(CreateClubRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getClub() != null) {
            throw new RuntimeException("Вы уже состоите в клубе");
        }

        Club club = new Club();
        club.setName(request.getName());
        club.setDescription(request.getDescription());
        club.setSocialLink(request.getSocialLink());
        club.setIsTournamentOperator(false);
        club.setPresident(currentUser);

        if (request.getCity() != null) {
            cityRepository.findByName(request.getCity()).ifPresent(club::setCity);
        }

        club = clubRepository.save(club);

        currentUser.setClub(club);
        userRepository.save(currentUser);

        return mapToDto(club);
    }

    /**
     * Обновляет данные клуба (название, описание, ссылка).
     *
     * <p>Доступно только президенту клуба или администратору.</p>
     *
     * @param id      идентификатор клуба
     * @param request новые данные клуба
     * @return DTO обновлённого клуба
     * @throws AccessDeniedException если у текущего пользователя нет прав
     */
    @Transactional
    public ClubDto updateClub(Long id, CreateClubRequest request) {
        Club club = clubRepository.findById(id).orElseThrow();
        User currentUser = getCurrentUser();

        checkPresidentOrAdmin(club, currentUser, "Только президент может редактировать клуб");
        System.out.println(request);
        club.setName(request.getName());
        club.setDescription(request.getDescription());
        club.setSocialLink(request.getSocialLink());

        return mapToDto(clubRepository.save(club));
    }

    /**
     * Удаляет клуб вместе со всеми связями.
     *
     * <p>Перед удалением у всех участников обнуляются клуб и права создания турниров.
     * Доступно только президенту или администратору.</p>
     *
     * @param clubId идентификатор клуба
     * @throws AccessDeniedException если у текущего пользователя нет прав
     */
    @Transactional
    public void deleteClub(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Клуб не найден"));
        User currentUser = getCurrentUser();

        checkPresidentOrAdmin(club, currentUser, "Только президент может удалить клуб");

        detachAllMembers(clubId);
        clubRepository.delete(club);
    }

    // ── ПРАВА КЛУБА ───────────────────────────────────────────────────────────

    /**
     * Устанавливает или снимает статус «Турнирный Оператор» у клуба.
     *
     * <p>При выдаче статуса: президент клуба автоматически получает право создавать турниры.</p>
     * <p>При снятии статуса: у всех участников клуба это право отзывается.</p>
     *
     * @param id     идентификатор клуба
     * @param status {@code true} — выдать, {@code false} — отозвать
     * @throws RuntimeException если клуб не найден
     */
    @Transactional
    public void setTournamentOperator(Long id, boolean status) {
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Клуб не найден"));

        club.setIsTournamentOperator(status);

        if (status) {
            grantTournamentRightToPresident(club);
        } else {
            revokeAllMembersTournamentRights(id);
        }

        clubRepository.save(club);
    }

    /**
     * Выдаёт конкретному участнику клуба право создавать турниры.
     *
     * <p>Доступно только президенту клуба-оператора.</p>
     *
     * @param clubId идентификатор клуба
     * @param userId идентификатор участника
     * @throws AccessDeniedException если текущий пользователь не президент
     * @throws RuntimeException      если клуб не является оператором или игрок не из клуба
     */
    @Transactional
    public void grantTournamentRight(Long clubId, Long userId) {
        Club club = clubRepository.findById(clubId).orElseThrow();
        checkIsPresident(club, "Только президент");

        if (!Boolean.TRUE.equals(club.getIsTournamentOperator())) {
            throw new RuntimeException("Клуб не является оператором");
        }

        User target = userRepository.findById(userId).orElseThrow();
        validateMemberBelongsToClub(target, clubId);

        target.setCanCreateClubTournaments(true);
        userRepository.save(target);
    }

    /**
     * Отзывает у участника клуба право создавать турниры.
     *
     * <p>Доступно только президенту клуба.</p>
     *
     * @param clubId идентификатор клуба
     * @param userId идентификатор участника
     * @throws AccessDeniedException если текущий пользователь не президент
     * @throws RuntimeException      если игрок не найден или не из клуба
     */
    @Transactional
    public void revokeTournamentRight(Long clubId, Long userId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Клуб не найден"));
        checkIsPresident(club, "Только президент может забирать права");

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Игрок не найден"));
        validateMemberBelongsToClub(target, clubId);

        target.setCanCreateClubTournaments(false);
        userRepository.save(target);
    }

    // ── ЧЛЕНСТВО ──────────────────────────────────────────────────────────────

    /**
     * Подаёт заявку на вступление в клуб от имени текущего пользователя.
     *
     * @param clubId идентификатор клуба
     * @throws RuntimeException если пользователь уже в клубе или заявка уже подана
     */
    @Transactional
    public void joinRequest(Long clubId) {
        User currentUser = getCurrentUser();
        if (currentUser.getClub() != null) {
            throw new RuntimeException("Вы уже в клубе");
        }

        Club club = clubRepository.findById(clubId).orElseThrow();
        if (clubRequestRepository.existsByUserIdAndClubId(currentUser.getId(), clubId)) {
            throw new RuntimeException("Заявка уже подана");
        }

        ClubRequest req = new ClubRequest();
        req.setUser(currentUser);
        req.setClub(club);
        clubRequestRepository.save(req);
    }

    /**
     * Возвращает список пользователей, подавших заявку в клуб.
     *
     * <p>Доступно только президенту клуба.</p>
     *
     * @param clubId идентификатор клуба
     * @return список DTO пользователей с ожидающими заявками
     */
    public List<UserDto> getRequests(Long clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow();
        checkIsPresident(club, "Нет прав");

        return clubRequestRepository.findByClubId(clubId).stream()
                .map(r -> mapUserToDto(r.getUser()))
                .collect(Collectors.toList());
    }

    /**
     * Одобряет или отклоняет заявку пользователя на вступление в клуб.
     *
     * <p>При одобрении: пользователь добавляется в клуб.
     * При отклонении: заявка просто удаляется.</p>
     *
     * @param clubId  идентификатор клуба
     * @param userId  идентификатор заявителя
     * @param approve {@code true} — одобрить, {@code false} — отклонить
     * @throws AccessDeniedException если текущий пользователь не президент
     * @throws RuntimeException      если заявка не найдена
     */
    @Transactional
    public void approveRequest(Long clubId, Long userId, boolean approve) {
        Club club = clubRepository.findById(clubId).orElseThrow();
        checkIsPresident(club, "Нет прав");

        ClubRequest req = clubRequestRepository.findByUserIdAndClubId(userId, clubId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (approve) {
            User user = req.getUser();
            user.setClub(club);
            userRepository.save(user);
        }

        clubRequestRepository.delete(req);
    }

    /**
     * Исключает участника из клуба.
     *
     * <p>Доступно только президенту. Президент не может исключить сам себя.</p>
     *
     * @param clubId идентификатор клуба
     * @param userId идентификатор исключаемого участника
     * @throws AccessDeniedException если текущий пользователь не президент
     * @throws RuntimeException      если пытаются исключить самого президента
     */
    @Transactional
    public void kickMember(Long clubId, Long userId) {
        Club club = clubRepository.findById(clubId).orElseThrow();
        User currentUser = getCurrentUser();
        checkIsPresident(club, "Нет прав");

        User target = userRepository.findById(userId).orElseThrow();
        if (target.getId().equals(currentUser.getId())) {
            throw new RuntimeException("Нельзя исключить себя");
        }

        target.setClub(null);
        target.setCanCreateClubTournaments(false);
        userRepository.save(target);
    }

    /**
     * Позволяет текущему пользователю покинуть клуб.
     *
     * <p>Президент клуба не может выйти — он должен передать права или удалить клуб.</p>
     *
     * @param clubId идентификатор клуба
     * @throws RuntimeException если пользователь не состоит в этом клубе
     *                          или является президентом
     */
    @Transactional
    public void leaveClub(Long clubId) {
        User currentUser = getCurrentUser();
        Club club = clubRepository.findById(clubId).orElseThrow();

        if (currentUser.getClub() == null || !currentUser.getClub().getId().equals(clubId)) {
            throw new RuntimeException("Вы не состоите в этом клубе");
        }

        if (club.getPresident().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Президент не может покинуть клуб. Передайте права или удалите клуб.");
        }

        currentUser.setClub(null);
        currentUser.setCanCreateClubTournaments(false);
        userRepository.save(currentUser);
    }

    // ── ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ────────────────────────────────────────────────

    /**
     * Отзывает у всех участников клуба право создавать турниры и убирает флаг оператора.
     */
    private void revokeAllMembersTournamentRights(Long clubId) {
        List<User> members = userRepository.findByClubId(clubId);
        members.forEach(m -> m.setCanCreateClubTournaments(false));
        userRepository.saveAll(members);
    }

    /**
     * Выдаёт президенту клуба право создавать турниры (при назначении оператором).
     */
    private void grantTournamentRightToPresident(Club club) {
        User president = club.getPresident();
        if (president != null) {
            president.setCanCreateClubTournaments(true);
            userRepository.save(president);
        }
    }

    /**
     * Обнуляет клуб и права всех участников перед удалением клуба.
     */
    private void detachAllMembers(Long clubId) {
        List<User> members = userRepository.findByClubId(clubId);
        members.forEach(m -> {
            m.setClub(null);
            m.setCanCreateClubTournaments(false);
        });
        userRepository.saveAll(members);
    }

    /**
     * Проверяет, что target-пользователь состоит в указанном клубе.
     */
    private void validateMemberBelongsToClub(User target, Long clubId) {
        if (target.getClub() == null || !target.getClub().getId().equals(clubId)) {
            throw new RuntimeException("Игрок не из вашего клуба");
        }
    }

    /**
     * Проверяет, что текущий пользователь является президентом клуба.
     *
     * @throws AccessDeniedException с переданным сообщением, если проверка не пройдена
     */
    private void checkIsPresident(Club club, String errorMessage) {
        User currentUser = getCurrentUser();
        if (!club.getPresident().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(errorMessage);
        }
    }

    /**
     * Проверяет, что текущий пользователь является президентом клуба или администратором.
     *
     * @throws AccessDeniedException с переданным сообщением, если проверка не пройдена
     */
    private void checkPresidentOrAdmin(Club club, User currentUser, String errorMessage) {
        boolean isPresident = club.getPresident().getId().equals(currentUser.getId());
        boolean isAdmin     = currentUser.getRole() == UserRole.admin;
        if (!isPresident && !isAdmin) {
            throw new AccessDeniedException(errorMessage);
        }
    }

    /** Извлекает текущего аутентифицированного пользователя из Security Context. */
    private User getCurrentUser() {
        String nick = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByNickname(nick).orElseThrow();
    }

    // ── МАППИНГ ───────────────────────────────────────────────────────────────

    /** Преобразует сущность клуба в DTO (без списка участников). */
    private ClubDto mapToDto(Club club) {
        return ClubDto.builder()
                .id(club.getId())
                .name(club.getName())
                .logoUrl(club.getLogoUrl())
                .city(club.getCity() != null ? club.getCity().getName() : null)
                .isTournamentOperator(club.getIsTournamentOperator())
                .presidentId(club.getPresident() != null ? club.getPresident().getId() : null)
                .presidentName(club.getPresident() != null ? club.getPresident().getNickname() : null)
                .description(club.getDescription())
                .socialLink(club.getSocialLink())
                .build();
    }

    /** Преобразует сущность пользователя в компактный DTO для списка участников. */
    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .canCreateTournaments(user.getCanCreateClubTournaments())
                .city(user.getCity() != null ? user.getCity().getName() : null)
                .build();
    }
}
