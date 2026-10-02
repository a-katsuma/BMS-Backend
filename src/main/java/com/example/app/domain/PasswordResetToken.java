package com.example.app.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    private Integer tokenId;
    private Integer userId;
    private String token;
    private LocalDateTime expiresAt;
    private Integer used;
    private LocalDateTime createdAt;
}