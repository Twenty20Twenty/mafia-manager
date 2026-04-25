package com.mafia.manager.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BestMoveDto {
    private Long authorId; // ID игрока (User), не слота (для удобства фронта)
    private List<Integer> candidates; // [1, 5, 9]
    private Integer guessedCount;
    private BigDecimal points;
}
