package com.mafia.manager.service;

import com.mafia.manager.dto.JudgeDto;
import com.mafia.manager.dto.UpdateJudgeRightsRequest;
import com.mafia.manager.entity.JudgeProfile;
import com.mafia.manager.entity.User;
import com.mafia.manager.repository.JudgeProfileRepository;
import com.mafia.manager.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервис управления профилями судей.
 *
 * <p>Судейский профиль ({@link JudgeProfile}) создаётся при первом назначении прав.
 * Хранит три независимых флага прав и дату начала судейства.</p>
 *
 * <p><strong>Логика даты судейства ({@code judgeSince}):</strong></p>
 * <ul>
 *   <li>Если {@code isJudge} переходит {@code false → true} — ставится сегодняшняя дата</li>
 *   <li>Если {@code isJudge} переходит {@code true → false} — дата обнуляется</li>
 *   <li>Остальные флаги ({@code canJudgeFinals}, {@code canBeHeadJudge}) дату не затрагивают</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class JudgeService {

    private final JudgeProfileRepository judgeRepository;
    private final UserRepository         userRepository;

    // ── ЧТЕНИЕ ────────────────────────────────────────────────────────────────

    /**
     * Возвращает постраничный список действующих судей с опциональной фильтрацией.
     *
     * <p>Базовое условие: {@code isJudge = true}. Фильтры применяются дополнительно.</p>
     *
     * @param canJudgeFinals фильтр по праву судить финалы ({@code null} — без фильтра)
     * @param canBeHead      фильтр по праву быть главным судьёй ({@code null} — без фильтра)
     * @param search         поиск по части никнейма ({@code null} — без поиска)
     * @param pageable       параметры пагинации и сортировки
     * @return страница DTO судей
     */
    public Page<JudgeDto> getAllJudges(Boolean canJudgeFinals, Boolean canBeHead, String search, Pageable pageable) {
        Specification<JudgeProfile> spec = buildFilterSpec(canJudgeFinals, canBeHead, search);
        return judgeRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    /**
     * Возвращает профиль судьи по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return DTO судьи, если профиль существует, иначе {@link Optional#empty()}
     */
    public Optional<JudgeDto> getJudgeByUserId(Long userId) {
        return judgeRepository.findByUserId(userId).map(this::mapToDto);
    }

    // ── ОБНОВЛЕНИЕ ПРАВ (ADMIN) ───────────────────────────────────────────────

    /**
     * Обновляет права судьи. Создаёт профиль, если он ещё не существует.
     *
     * <p>Каждое поле запроса применяется только при ненулевом значении,
     * что позволяет частично обновлять права без сброса остальных.</p>
     *
     * @param userId  идентификатор пользователя
     * @param request новые права (поля nullable — null означает «не менять»)
     * @return обновлённый DTO судьи
     * @throws RuntimeException если пользователь не найден
     */
    @Transactional
    public JudgeDto updateRights(Long userId, UpdateJudgeRightsRequest request) {
        JudgeProfile profile = judgeRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyProfile(userId));

        applyIsJudgeFlag(profile, request.getIsJudge());

        if (request.getCanJudgeFinals() != null) {
            profile.setCanJudgeFinals(request.getCanJudgeFinals());
        }
        if (request.getCanBeHeadJudge() != null) {
            profile.setCanBeHeadJudge(request.getCanBeHeadJudge());
        }

        return mapToDto(judgeRepository.save(profile));
    }

    // ── ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ────────────────────────────────────────────────

    /**
     * Применяет флаг {@code isJudge} и управляет датой судейства.
     *
     * <p>Дата ставится при первом назначении ({@code false → true})
     * и сбрасывается при снятии статуса ({@code true → false}).</p>
     */
    private void applyIsJudgeFlag(JudgeProfile profile, Boolean newIsJudge) {
        if (newIsJudge == null) return;

        boolean wasJudge  = Boolean.TRUE.equals(profile.getIsJudge());
        boolean willBeJudge = newIsJudge;

        profile.setIsJudge(willBeJudge);

        if (!wasJudge && willBeJudge) {
            profile.setJudgeSince(LocalDate.now());
        } else if (wasJudge && !willBeJudge) {
            profile.setJudgeSince(null);
        }
    }

    /**
     * Создаёт и немедленно персистирует пустой профиль судьи для существующего пользователя.
     *
     * @throws RuntimeException если пользователь не найден
     */
    private JudgeProfile createEmptyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        JudgeProfile profile = new JudgeProfile();
        profile.setUser(user);
        profile.setIsJudge(false);

        return judgeRepository.saveAndFlush(profile);
    }

    /**
     * Строит {@link Specification} для фильтрации судей.
     * Базовое условие {@code isJudge = true} применяется всегда.
     */
    private Specification<JudgeProfile> buildFilterSpec(Boolean canJudgeFinals, Boolean canBeHead, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("isJudge")));

            if (Boolean.TRUE.equals(canJudgeFinals)) {
                predicates.add(cb.isTrue(root.get("canJudgeFinals")));
            }
            if (Boolean.TRUE.equals(canBeHead)) {
                predicates.add(cb.isTrue(root.get("canBeHeadJudge")));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("user").get("nickname")),
                        "%" + search.toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ── МАППИНГ ───────────────────────────────────────────────────────────────

    /** Преобразует профиль судьи в DTO. */
    private JudgeDto mapToDto(JudgeProfile p) {
        return JudgeDto.builder()
                .userId(p.getUserId())
                .nickname(p.getUser().getNickname())
                .avatarUrl(p.getUser().getAvatarUrl())
                .isJudge(p.getIsJudge())
                .canJudgeFinals(p.getCanJudgeFinals())
                .canBeHeadJudge(p.getCanBeHeadJudge())
                .judgeSince(p.getJudgeSince())
                .clubName(p.getUser().getClub() != null ? p.getUser().getClub().getName() : null)
                .build();
    }
}
