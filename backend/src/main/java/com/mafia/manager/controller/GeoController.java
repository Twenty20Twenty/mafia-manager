package com.mafia.manager.controller;

import com.mafia.manager.dto.CityDto;
import com.mafia.manager.dto.CountryDto;
import com.mafia.manager.service.GeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
@Tag(name = "Geo", description = "Справочник стран и городов")
public class GeoController {

    private final GeoService geoService;

    @Operation(summary = "Список всех стран")
    @GetMapping("/countries")
    public ResponseEntity<List<CountryDto>> getCountries() {
        return ResponseEntity.ok(geoService.getCountries());
    }

    @Operation(
            summary = "Список городов",
            parameters = @Parameter(name = "countryId", description = "Фильтр по стране (опционально)")
    )
    @GetMapping("/cities")
    public ResponseEntity<List<CityDto>> getCities(
            @RequestParam(required = false) Integer countryId
    ) {
        return ResponseEntity.ok(geoService.getCities(countryId));
    }
}
