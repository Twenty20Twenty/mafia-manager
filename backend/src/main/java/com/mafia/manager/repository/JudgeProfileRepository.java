package com.mafia.manager.repository;

import com.mafia.manager.entity.JudgeProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Добавляем JpaSpecificationExecutor для удобной фильтрации
@Repository
public interface JudgeProfileRepository extends JpaRepository<JudgeProfile, Long>, JpaSpecificationExecutor<JudgeProfile> {
    Optional<JudgeProfile> findByUserId(Long userId);
}
