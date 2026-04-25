package com.mafia.manager.service;

import com.mafia.manager.entity.Game;
import com.mafia.manager.entity.Tournament;
import com.mafia.manager.entity.User;
import com.mafia.manager.entity.enums.UserRole;
import com.mafia.manager.repository.GameRepository;
import com.mafia.manager.repository.TournamentRepository;
import com.mafia.manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис проверки прав доступа к ресурсам платформы.
 *
 * <p>Используется в аннотациях {@code @PreAuthorize} через SpEL:
 * {@code @PreAuthorize("@permissionService.canEditGame(#gameId)")}.</p>
 *
 * <p>Иерархия прав (от высших к низшим):</p>
 * <ol>
 *   <li><strong>Администратор</strong> — полный доступ ко всему</li>
 *   <li><strong>Организатор / ГС турнира</strong> — управление турниром и всеми его играми</li>
 *   <li><strong>Судья стола</strong> — редактирование протокола конкретной игры</li>
 * </ol>
 */
@Service("permissionService")
@RequiredArgsConstructor
public class PermissionService {

    private final GameRepository       gameRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository       userRepository;

    /**
     * Проверяет, может ли текущий пользователь редактировать протокол игры.
     *
     * <p>Право есть у:</p>
     * <ul>
     *   <li>администратора</li>
     *   <li>судьи, назначенного на эту конкретную игру</li>
     *   <li>организатора или главного судьи турнира</li>
     * </ul>
     *
     * @param gameId идентификатор игры
     * @return {@code true}, если редактирование разрешено
     * @throws RuntimeException если игра не найдена
     */
    @Transactional(readOnly = true)
    public boolean canEditGame(Long gameId) {
        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) return true;

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        boolean isTableJudge = game.getJudge() != null
                && game.getJudge().getId().equals(currentUser.getId());

        return isTableJudge || isTournamentManager(game.getTournament(), currentUser);
    }

    /**
     * Проверяет, может ли текущий пользователь управлять турниром
     * (создавать игры, изменять настройки, управлять участниками).
     *
     * <p>Право есть у администратора, организатора и ГС турнира.</p>
     *
     * @param tournamentId идентификатор турнира
     * @return {@code true}, если управление разрешено
     * @throws RuntimeException если турнир не найден
     */
    @Transactional(readOnly = true)
    public boolean canManageTournament(Long tournamentId) {
        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) return true;

        Tournament t = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        return isTournamentManager(t, currentUser);
    }

    /**
     * Проверяет, может ли текущий пользователь удалить игру.
     *
     * <p>Право есть только у администратора, организатора и ГС турнира.
     * Обычный судья стола удалять игру не может.</p>
     *
     * @param gameId идентификатор игры
     * @return {@code true}, если удаление разрешено
     * @throws RuntimeException если игра не найдена
     */
    @Transactional(readOnly = true)
    public boolean canDeleteGame(Long gameId) {
        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) return true;

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        return isTournamentManager(game.getTournament(), currentUser);
    }

    // ── ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ────────────────────────────────────────────────

    /**
     * Проверяет, является ли пользователь менеджером турнира
     * (организатором или главным судьёй).
     */
    private boolean isTournamentManager(Tournament t, User u) {
        boolean isOrganizer  = t.getOrganizer()  != null && t.getOrganizer().getId().equals(u.getId());
        boolean isHeadJudge  = t.getHeadJudge()  != null && t.getHeadJudge().getId().equals(u.getId());
        return isOrganizer || isHeadJudge;
    }

    /** Возвращает {@code true}, если у пользователя роль {@code admin}. */
    private boolean isAdmin(User user) {
        return user.getRole() == UserRole.admin;
    }

    /**
     * Извлекает текущего аутентифицированного пользователя из Security Context.
     *
     * @throws RuntimeException если пользователь не найден в БД
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByNickname(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found in context"));
    }
}
