package com.mafia.manager.repository;

import com.mafia.manager.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Integer> {
    Optional<City> findByName(String name); // Для маппинга при регистрации
    List<City> findByCountryId(Integer countryId);
}
