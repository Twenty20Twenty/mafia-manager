package com.mafia.manager.dto;

import lombok.Data;

/** Тело запроса POST /api/users/{id}/request-email-change */
@Data
public class RequestEmailChangeRequest {
    private String newEmail;
}
