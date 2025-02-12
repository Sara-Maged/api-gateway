package com.example.api_gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDetails {

    private Long id;
    private String username;
    private String password;
    private boolean enabled;
}
