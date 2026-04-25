package com.mafia.manager.dto;
import lombok.Data;

@Data
public class CreatePhantomRequest {
    private String nickname;
    private String city;
    private String gender;
}
