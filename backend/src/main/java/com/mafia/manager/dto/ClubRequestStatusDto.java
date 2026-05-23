package com.mafia.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Статус заявок текущего пользователя в клубы.
 * Используется для отображения правильной кнопки на странице клуба.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClubRequestStatusDto {

    /** ID клуба, в который подана заявка (null — заявок нет) */
    private Long pendingClubId;

    /** Есть ли заявка именно в этот клуб */
    private boolean hasPendingRequestForThisClub;

    /** Есть ли заявка в какой-то другой клуб */
    private boolean hasPendingRequestForOtherClub;
}
