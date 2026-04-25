package com.mafia.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String city;
    private String socialLink;
    private String role;
    private Boolean canCreateTournaments;
    private Long clubId;
    private String clubName;
    private String status;

    private Boolean isPhantom;
    private String phantomCode;
}
