package com.duebook.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long userId;
    private String name;
    private String phone;
    private String email;

    public AuthResponse(String token, Long userId, String name, String phone, String email) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.email = email;
    }
}

