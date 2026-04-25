package com.mafia.manager.repository;

import com.mafia.manager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);
    Optional<User> findByPhantomCode(String phantomCode);

    // Нужен для requestPasswordReset — ищем пользователя по email
    Optional<User> findByEmail(String email);

    boolean existsByNickname(String nickname);
    boolean existsByEmail(String email);

    List<User> findByClubId(Long clubId);

    Page<User> findByNicknameContainingIgnoreCase(String nickname, Pageable pageable);
}
