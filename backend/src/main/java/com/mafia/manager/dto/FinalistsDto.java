package com.mafia.manager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FinalistsDto {

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID игроков-финалистов")
    private List<Long> playerIds;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Список зафиксирован?")
    private boolean locked;
}