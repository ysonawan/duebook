package com.duebook.app.dto;

import lombok.Data;

@Data
public class UpdateBasicInfoRequest {
    private String name;
    private String phone;
    private String email;
}

