package com.jadakeel.auth.dto;

import com.jadakeel.auth.model.UserAccount;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private UUID userId;
    private String email;
    private UserAccount.Role role;
    private String token;
}
