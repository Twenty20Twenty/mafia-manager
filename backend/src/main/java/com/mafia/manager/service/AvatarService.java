package com.mafia.manager.service;

import com.mafia.manager.entity.Club;
import com.mafia.manager.entity.User;
import com.mafia.manager.entity.enums.UserRole;
import com.mafia.manager.repository.ClubRepository;
import com.mafia.manager.repository.UserRepository;
import com.mafia.manager.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Бизнес-логика управления аватарами пользователей и клубов.
 *
 * <p>Отвечает за:</p>
 * <ul>
 *   <li>Проверку прав (только владелец или админ)</li>
 *   <li>Удаление старого аватара из хранилища перед заменой</li>
 *   <li>Сохранение нового URL в БД</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AvatarService {

    private final StorageService    storageService;
    private final UserRepository    userRepository;
    private final ClubRepository    clubRepository;

    // ── Пользователи ─────────────────────────────────────────────────────────

    /**
     * Загружает новый аватар пользователя.
     * Старый аватар удаляется из хранилища (если был загружен через нас).
     *
     * @return публичный URL нового аватара
     */
    @Transactional
    public String uploadUserAvatar(Long userId, MultipartFile file) {
        User currentUser = getCurrentUser();
        User targetUser  = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        checkUserEditRights(currentUser, userId);

        // Удаляем старый аватар из хранилища
        storageService.deleteByUrl(targetUser.getAvatarUrl());

        // Загружаем новый
        String newUrl = storageService.uploadAvatar(userId, file);
        targetUser.setAvatarUrl(newUrl);
        userRepository.save(targetUser);

        return newUrl;
    }

    /**
     * Удаляет аватар пользователя (из хранилища и из БД).
     */
    @Transactional
    public void deleteUserAvatar(Long userId) {
        User currentUser = getCurrentUser();
        User targetUser  = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        checkUserEditRights(currentUser, userId);

        storageService.deleteByUrl(targetUser.getAvatarUrl());
        targetUser.setAvatarUrl(null);
        userRepository.save(targetUser);
    }

    // ── Клубы ────────────────────────────────────────────────────────────────

    /**
     * Загружает логотип клуба.
     * Доступно только президенту клуба или администратору.
     *
     * @return публичный URL нового логотипа
     */
    @Transactional
    public String uploadClubAvatar(Long clubId, MultipartFile file) {
        User currentUser = getCurrentUser();
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Клуб не найден"));

        checkClubEditRights(currentUser, club);

        storageService.deleteByUrl(club.getLogoUrl());

        String newUrl = storageService.uploadAvatar(clubId, file);
        club.setLogoUrl(newUrl);
        clubRepository.save(club);

        return newUrl;
    }

    /**
     * Удаляет логотип клуба.
     */
    @Transactional
    public void deleteClubAvatar(Long clubId) {
        User currentUser = getCurrentUser();
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Клуб не найден"));

        checkClubEditRights(currentUser, club);

        storageService.deleteByUrl(club.getLogoUrl());
        club.setLogoUrl(null);
        clubRepository.save(club);
    }

    // ── Проверки прав ─────────────────────────────────────────────────────────

    private void checkUserEditRights(User currentUser, Long targetUserId) {
        boolean isAdmin = currentUser.getRole() == UserRole.admin;
        boolean isOwner = currentUser.getId().equals(targetUserId);
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Нет прав для изменения аватара этого пользователя");
        }
    }

    private void checkClubEditRights(User currentUser, Club club) {
        boolean isAdmin     = currentUser.getRole() == UserRole.admin;
        boolean isPresident = club.getPresident() != null
                && club.getPresident().getId().equals(currentUser.getId());
        if (!isAdmin && !isPresident) {
            throw new AccessDeniedException("Только президент клуба может изменять логотип");
        }
    }

    private User getCurrentUser() {
        String nick = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByNickname(nick)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден в контексте"));
    }
}
