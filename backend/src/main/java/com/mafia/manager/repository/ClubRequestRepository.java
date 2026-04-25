package com.mafia.manager.repository;

import com.mafia.manager.entity.ClubRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClubRequestRepository extends JpaRepository<ClubRequest, Long> {
    List<ClubRequest> findByClubId(Long clubId);
    Optional<ClubRequest> findByUserIdAndClubId(Long userId, Long clubId);
    boolean existsByUserIdAndClubId(Long userId, Long clubId);
}
