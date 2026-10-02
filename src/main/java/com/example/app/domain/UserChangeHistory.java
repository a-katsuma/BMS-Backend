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
public class UserChangeHistory {
    private Integer historyId;
    private Integer userId;
    private Integer companyId;
    private String changeType; // "PASSWORD_RESET" / "EMAIL_CHANGE" / "ROLE_TRANSFER"
    private String oldValue;
    private String newValue;
    private String operatedBy;
    private LocalDateTime createdAt;
}