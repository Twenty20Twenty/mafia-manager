package com.mafia.manager.service;

import com.mafia.manager.dto.CityDto;
import com.mafia.manager.dto.CountryDto;
import com.mafia.manager.entity.City;
import com.mafia.manager.entity.Country;
import com.mafia.manager.repository.CityRepository;
import com.mafia.manager.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис географических справочников.
 *
 * <p>Предоставляет доступ к справочным данным стран и городов.
 * Используется при регистрации, создании клубов и турниров для
 * привязки сущностей к конкретному городу.</p>
 *
 * <p>Все операции — только для чтения, запись в справочники
 * производится через миграции БД.</p>
 */
@Service
@RequiredArgsConstructor
public class GeoService {

    private final CountryRepository countryRepository;
    private final CityRepository    cityRepository;

    /**
     * Возвращает список всех стран.
     *
     * @return список DTO стран
     */
    public List<CountryDto> getCountries() {
        return countryRepository.findAll().stream()
                .map(this::mapCountry)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список городов.
     *
     * <p>Если передан {@code countryId} — возвращаются только города этой страны.
     * Если {@code countryId} равен {@code null} — возвращаются все города.</p>
     *
     * @param countryId идентификатор страны для фильтрации (nullable)
     * @return список DTO городов
     */
    public List<CityDto> getCities(Integer countryId) {
        List<City> cities = (countryId != null)
                ? cityRepository.findByCountryId(countryId)
                : cityRepository.findAll();

        return cities.stream()
                .map(this::mapCity)
                .collect(Collectors.toList());
    }

    // ── МАППИНГ ───────────────────────────────────────────────────────────────

    /** Преобразует сущность страны в DTO. */
    private CountryDto mapCountry(Country c) {
        return CountryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .build();
    }

    /** Преобразует сущность города в DTO (включая данные страны). */
    private CityDto mapCity(City c) {
        return CityDto.builder()
                .id(c.getId())
                .name(c.getName())
                .countryId(c.getCountry().getId())
                .countryName(c.getCountry().getName())
                .build();
    }
}
