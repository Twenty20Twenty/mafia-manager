package com.mafia.manager.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String nickname;
    private String avatarUrl;
    private String socialLink;
    private String city;
    private String gender;
}
