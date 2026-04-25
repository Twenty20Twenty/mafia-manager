package com.mafia.manager.service;

import com.mafia.manager.dto.UpdateUserRequest;
import com.mafia.manager.dto.UserDto;
import com.mafia.manager.entity.User;
import com.mafia.manager.entity.enums.UserGender;
import com.mafia.manager.entity.enums.UserRole;
import com.mafia.manager.repository.CityRepository;
import com.mafia.manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис управления профилями пользователей.
 *
 * <p>Предоставляет операции:</p>
 * <ul>
 *   <li>Постраничный поиск пользователей с фильтром по никнейму</li>
 *   <li>Получение профиля по ID</li>
 *   <li>Обновление профиля (только владелец или администратор)</li>
 *   <li>Удаление пользователя (только администратор)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CityRepository cityRepository;

    // ── СПИСОК ────────────────────────────────────────────────────────────────

    /**
     * Возвращает постраничный список пользователей.
     *
     * <p>Если {@code search} передан, выполняется поиск по частичному совпадению никнейма
     * (регистронезависимо). Иначе возвращаются все пользователи.</p>
     *
     * @param search   строка поиска по никнейму (nullable)
     * @param pageable параметры пагинации и сортировки
     * @return страница DTO пользователей
     */
    public Page<UserDto> getAllUsers(String search, Pageable pageable) {
        Page<User> users = (search != null && !search.isBlank())
                ? userRepository.findByNicknameContainingIgnoreCase(search, pageable)
                : userRepository.findAll(pageable);

        return users.map(this::mapToDto);
    }

    // ── ПОЛУЧЕНИЕ ─────────────────────────────────────────────────────────────

    /**
     * Возвращает профиль пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     * @return DTO пользователя
     * @throws RuntimeException если пользователь не найден
     */
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDto(user);
    }

    // ── ОБНОВЛЕНИЕ ────────────────────────────────────────────────────────────

    /**
     * Обновляет профиль пользователя.
     *
     * <p>Доступно только самому пользователю или администратору.
     * Каждое поле запроса применяется только при ненулевом значении.</p>
     *
     * <p>При смене никнейма проверяется уникальность нового значения.</p>
     *
     * @param id      идентификатор обновляемого пользователя
     * @param request новые данные профиля (поля nullable)
     * @return DTO обновлённого профиля
     * @throws AccessDeniedException если пользователь пытается редактировать чужой профиль
     * @throws RuntimeException      если новый никнейм уже занят
     */
    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User currentUser = getCurrentUser();
        User targetUser  = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        checkEditRights(currentUser, id);

        applyNickname(targetUser, request.getNickname());
        applyOptionalFields(targetUser, request);

        return mapToDto(userRepository.save(targetUser));
    }

    // ── УДАЛЕНИЕ ──────────────────────────────────────────────────────────────

    /**
     * Удаляет пользователя по идентификатору (жёсткое удаление).
     *
     * <p>Каскадно удаляет связанный профиль судьи и другие зависимые данные
     * (согласно настройкам БД).</p>
     *
     * @param id идентификатор удаляемого пользователя
     * @throws RuntimeException если пользователь не найден
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    // ── ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ────────────────────────────────────────────────

    /**
     * Проверяет, что текущий пользователь вправе редактировать профиль с указанным ID.
     *
     * @throws AccessDeniedException если попытка редактировать чужой профиль без прав администратора
     */
    private void checkEditRights(User currentUser, Long targetId) {
        boolean isAdmin = currentUser.getRole() == UserRole.admin;
        boolean isOwner = currentUser.getId().equals(targetId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Вы не можете редактировать чужой профиль");
        }
    }

    /**
     * Применяет новый никнейм к пользователю, если он не пустой.
     * Проверяет уникальность перед применением.
     */
    private void applyNickname(User user, String newNickname) {
        if (newNickname == null || newNickname.isBlank()) return;

        boolean nicknameChanged = !user.getNickname().equals(newNickname);
        if (nicknameChanged && userRepository.existsByNickname(newNickname)) {
            throw new RuntimeException("Никнейм уже занят");
        }

        user.setNickname(newNickname);
    }

    /**
     * Применяет остальные необязательные поля из запроса (аватар, ссылка, город, пол).
     * Каждое поле применяется только при ненулевом значении.
     */
    private void applyOptionalFields(User user, UpdateUserRequest request) {
        if (request.getAvatarUrl()   != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getSocialLink()  != null) user.setSocialLink(request.getSocialLink());

        if (request.getCity() != null) {
            cityRepository.findByName(request.getCity()).ifPresent(user::setCity);
        }

        if (request.getGender() != null) {
            try {
                user.setGender(UserGender.valueOf(request.getGender()));
            } catch (IllegalArgumentException ignored) {
                // Невалидное значение пола — игнорируем, не меняем текущее
            }
        }
    }

    /** Извлекает текущего аутентифицированного пользователя из Security Context. */
    private User getCurrentUser() {
        String nickname = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByNickname(nickname).orElseThrow();
    }

    // ── МАППИНГ ───────────────────────────────────────────────────────────────

    /** Преобразует сущность пользователя в полный DTO профиля. */
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .socialLink(user.getSocialLink())
                .role(user.getRole().name())
                .city(user.getCity()  != null ? user.getCity().getName()  : null)
                .clubId(user.getClub()   != null ? user.getClub().getId()   : null)
                .clubName(user.getClub() != null ? user.getClub().getName() : null)
                .isPhantom(user.getIsPhantom())
                .phantomCode(user.getPhantomCode())
                .canCreateTournaments(user.getCanCreateClubTournaments())
                .build();
    }
}
