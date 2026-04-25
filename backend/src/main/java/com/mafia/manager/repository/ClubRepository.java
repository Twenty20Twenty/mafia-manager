package com.mafia.manager.repository;

import com.mafia.manager.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    // Для проверки уникальности имени при создании
    boolean existsByName(String name);
}
